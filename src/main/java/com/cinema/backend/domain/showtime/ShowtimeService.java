/***
 * Showtime.java 엔티티 데이터에 접근하는 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-10-14: 파일 생성 및 작성
 * -2026-10-15: updateShowtimeStatus() 메서드 추가
 * -2026-10-15: findByScreenId(), findById() 조회 메서드 추가
 * -2026-11-01: 상영정보 · 상영관 비관적 락, 예매 정보가 존재하는 경우 상영정보 일정 변경 및 비활성화 거부를 위한
 *              createShowtime(), updateSchedule(), updateShowtimeStatus() 메서드 리워크 (D-44)
 *              -상영정보 · 상영관을 잠그기 위해 findByIdForUpdate() 사용(기존 findById() 메서드도 대체)
 *              -SELECT시 최신 데이터를 참조하기 위해 트랜잭션에 isolation = Isolation.READ_COMMITTED 적용
 *              -예매정보가 존재하는 경우 409(ConflictException) 예외 처리
 * -2026-11-04: 고객용 상영시간표 조회 메서드 findTimetable() 추가 (ShowtimeRepositoryCustom.findTimetable() 사용)
 *              -현재 시각은 서비스에서 구해 리포지토리에 넘김 (리포지토리가 직접 LocalDateTime.now()를 호출하지 않음)
 * -2026-11-13: 고객용 좌석 조회 메서드 findShowtimeSeats() 추가 및
 *              해당 메서드 기능 구현에 필요한 SeatAvailabilityResponse 객체의
 *              SeatAvailabilityStatus 설정을 위한 편의 메서드 getSeatAvailabilityStatus() 추가
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.domain.movie.Movie;
import com.cinema.backend.domain.movie.MovieRepository;
import com.cinema.backend.domain.reservation.ReservationSeatRepository;
import com.cinema.backend.domain.screen.Screen;
import com.cinema.backend.domain.screen.ScreenRepository;
import com.cinema.backend.domain.screen.Seat;
import com.cinema.backend.domain.screen.SeatRepository;
import com.cinema.backend.domain.showtime.dto.SeatAvailabilityResponse;
import com.cinema.backend.domain.showtime.dto.ShowtimeResponse;
import com.cinema.backend.domain.showtime.dto.ShowtimeSeatsResponse;
import com.cinema.backend.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.cinema.backend.domain.showtime.Showtime.calcEndTime;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final ScreenRepository screenRepository;
    private final MovieRepository movieRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final SeatRepository seatRepository;

    @Value("${cinema.showtime.buffer-minutes}")
    private int bufferMinutes;

    // 상영관 잠금 이후의 겹침 검사가 최신 커밋을 보도록 READ_COMMITTED 지정 (D-44)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShowtimeResponse createShowtime(Long movieId, Long screenId, LocalDateTime startTime) {

        if (movieId == null) {

            throw new IllegalArgumentException("잘못된 영화 정보입니다.");
        }

        if (screenId == null) {

            throw new IllegalArgumentException("잘못된 상영관 정보입니다.");
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 영화입니다."));

        Screen screen = screenRepository.findByIdForUpdate(screenId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영관입니다."));

        LocalDateTime endTime = calcEndTime(startTime, movie);
        validateNoOverlap(screen, null, startTime, endTime);

        Showtime showtime = new Showtime(movie, screen, startTime);
        showtimeRepository.save(showtime);

        return new ShowtimeResponse(showtime);
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> findByScreenId(Long screenId) {

        if (screenId == null) {

            throw new IllegalArgumentException("잘못된 상영관 정보입니다.");
        }

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영관입니다."));

        return showtimeRepository.findByScreenOrderByStartTimeAsc(screen)
                .stream()
                .map(ShowtimeResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> findTimetable(LocalDate date, Long movieId, Long screenId) {

        if (date == null) {

            throw new IllegalArgumentException("조회 날짜는 비워둘 수 없습니다.");
        }

        return showtimeRepository.findTimetable(date, movieId, screenId, LocalDateTime.now())
                .stream()
                .map(ShowtimeResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShowtimeResponse findById(Long showtimeId) {

        if (showtimeId == null) {

            throw new IllegalArgumentException("잘못된 상영정보입니다.");
        }

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영정보입니다."));

        return new ShowtimeResponse(showtime);
    }

    @Transactional(readOnly = true)
    public ShowtimeSeatsResponse findShowtimeSeats(Long showtimeId) {

        if (showtimeId == null) {

            throw new IllegalArgumentException("잘못된 상영정보입니다.");
        }

        Showtime showtime = showtimeRepository.findWithMovieAndScreenById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영정보입니다."));

        if (showtime.getShowtimeStatus() != Showtime.ShowtimeStatus.ACTIVE
                || showtime.getMovie().getDisplayStatus() != Movie.DisplayStatus.ACTIVE) {

            throw new ConflictException("예매할 수 없는 상영정보입니다.");
        }

        if (!showtime.getStartTime().isAfter(LocalDateTime.now())) {

            throw new ConflictException("상영정보의 예매 가능 시간이 종료되었습니다.");
        }

        List<Seat> seats = seatRepository.findAllWithGradeByScreenId(showtime.getScreen().getId());
        Set<Long> reservedIds = new HashSet<>(
                reservationSeatRepository.findReservedSeatIdsByShowtimeId(showtime.getId())
        );

        List<SeatAvailabilityResponse> seatAvailabilityResponses = seats.stream()
                .map(seat -> new SeatAvailabilityResponse(
                        seat,
                        getSeatAvailabilityStatus(seat, reservedIds)
                ))
                .toList();

        return new ShowtimeSeatsResponse(showtime, seatAvailabilityResponses);
    }

    // 상영 일정 · 상영관 잠금 이후의 예약 확인 · 겹침 검사가 최신 커밋을 보도록 READ_COMMITTED 지정 (D-44)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShowtimeResponse updateSchedule(Long showtimeId, LocalDateTime startTime) {

        if (showtimeId == null) {

            throw new IllegalArgumentException("잘못된 상영정보입니다.");
        }

        Showtime showtime = showtimeRepository.findByIdForUpdate(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영정보입니다."));

        if (reservationSeatRepository.existsByShowtimeIdAndActiveTrue(showtimeId)) {

            throw new ConflictException("예매된 좌석이 있는 상영정보는 일정을 변경할 수 없습니다.");
        }

        // 상영정보 업데이트시 상영관을 잠그기 위해 상영관 잠금 조회 메서드 호출
        screenRepository.findByIdForUpdate(showtime.getScreen().getId());

        LocalDateTime endTime = calcEndTime(startTime, showtime.getMovie());
        validateNoOverlap(showtime.getScreen(), showtimeId, startTime, endTime);

        showtime.updateSchedule(startTime);
        return new ShowtimeResponse(showtime);
    }

    // 상영 일정 잠금 이후의 예약 확인이 최신 커밋을 보도록 READ_COMMITTED 지정 (D-44)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShowtimeResponse updateShowtimeStatus(Long showtimeId, String showtimeStatus) {

        if (showtimeId == null) {

            throw new IllegalArgumentException("잘못된 상영정보입니다.");
        }

        Showtime showtime = showtimeRepository.findByIdForUpdate(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영정보입니다."));

        showtime.updateShowtimeStatus(showtimeStatus);
        if (showtime.getShowtimeStatus() == Showtime.ShowtimeStatus.DEACTIVATED
                && reservationSeatRepository.existsByShowtimeIdAndActiveTrue(showtimeId)) {

            throw new ConflictException("예매된 좌석이 있는 상영정보는 비활성화할 수 없습니다.");
        }

        return new ShowtimeResponse(showtime);
    }

    private void validateNoOverlap(
            Screen screen,
            Long excludeId,
            LocalDateTime newStartTime,
            LocalDateTime newEndTime) {

        LocalDateTime finalStartTime = newStartTime.minusMinutes(bufferMinutes);
        LocalDateTime finalEndTime = newEndTime.plusMinutes(bufferMinutes);

        if (excludeId == null) {

            if (showtimeRepository.existsByScreenAndStartTimeLessThanAndEndTimeGreaterThan(screen, finalEndTime, finalStartTime)) {

                throw new IllegalArgumentException("새 상영스케줄이 기존 상영스케줄과 겹칩니다.");
            }
        } else {

            if (showtimeRepository.existsByScreenAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(screen, excludeId, finalEndTime, finalStartTime)) {

                throw new IllegalArgumentException("새 상영스케줄이 기존 상영스케줄과 겹칩니다.");
            }
        }
    }

    private SeatAvailabilityResponse.SeatAvailabilityStatus getSeatAvailabilityStatus(
            Seat seat, Set<Long> reservedIds
    ) {

        Seat.SeatStatus seatStatus = seat.getSeatStatus();

        if (seatStatus != Seat.SeatStatus.ACTIVE) {

            return SeatAvailabilityResponse.SeatAvailabilityStatus.UNAVAILABLE;
        }

        if (reservedIds.contains(seat.getId())) {

            return SeatAvailabilityResponse.SeatAvailabilityStatus.RESERVED;
        }

        return SeatAvailabilityResponse.SeatAvailabilityStatus.AVAILABLE;
    }
}
