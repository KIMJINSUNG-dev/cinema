/***
 * Showtime.java 엔티티 데이터에 접근하는 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-10-14: 파일 생성 및 작성
 * -2026-10-15: updateShowtimeStatus() 메서드 추가
 * -2026-10-15: findByScreenId(), findById() 조회 메서드 추가
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.domain.movie.Movie;
import com.cinema.backend.domain.movie.MovieRepository;
import com.cinema.backend.domain.screen.Screen;
import com.cinema.backend.domain.screen.ScreenRepository;
import com.cinema.backend.domain.showtime.dto.ShowtimeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.cinema.backend.domain.showtime.Showtime.calcEndTime;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final ScreenRepository screenRepository;
    private final MovieRepository movieRepository;

    @Value("${cinema.showtime.buffer-minutes}")
    private int bufferMinutes;

    @Transactional
    public ShowtimeResponse createShowtime(Long movieId, Long screenId, LocalDateTime startTime) {

        if (movieId == null) {

            throw new IllegalArgumentException("잘못된 영화 정보입니다.");
        }

        if (screenId == null) {

            throw new IllegalArgumentException("잘못된 상영관 정보입니다.");
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 영화입니다."));

        Screen screen = screenRepository.findById(screenId)
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
    public ShowtimeResponse findById(Long showtimeId) {

        if (showtimeId == null) {

            throw new IllegalArgumentException("잘못된 상영정보입니다.");
        }

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영정보입니다."));

        return new ShowtimeResponse(showtime);
    }

    @Transactional
    public ShowtimeResponse updateSchedule(Long showtimeId, LocalDateTime startTime) {

        if (showtimeId == null) {

            throw new IllegalArgumentException("잘못된 상영정보입니다.");
        }

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영정보입니다."));

        LocalDateTime endTime = calcEndTime(startTime, showtime.getMovie());
        validateNoOverlap(showtime.getScreen(), showtimeId, startTime, endTime);

        showtime.updateSchedule(startTime);
        return new ShowtimeResponse(showtime);
    }

    @Transactional
    public ShowtimeResponse updateShowtimeStatus(Long showtimeId, String showtimeStatus) {

        if (showtimeId == null) {

            throw new IllegalArgumentException("잘못된 상영정보입니다.");
        }

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영정보입니다."));

        showtime.updateShowtimeStatus(showtimeStatus);
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
}
