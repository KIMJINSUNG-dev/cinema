/***
 * reservation_seats의 유니크 제약(uk_reservation_seats_showtime_seat_active, D-40)이
 * 실제 MySQL에서 의도대로 동작하는지(2차 방어, D-45) 검증하는 통합 테스트를 담당하는 클래스
 * 작성이력
 * -2026-10-29: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.member.Member;
import com.cinema.backend.domain.member.MemberRepository;
import com.cinema.backend.domain.member.MemberService;
import com.cinema.backend.domain.movie.MovieService;
import com.cinema.backend.domain.screen.ScreenService;
import com.cinema.backend.domain.screen.Seat;
import com.cinema.backend.domain.screen.SeatGradeService;
import com.cinema.backend.domain.screen.SeatRepository;
import com.cinema.backend.domain.screen.SeatService;
import com.cinema.backend.domain.showtime.Showtime;
import com.cinema.backend.domain.showtime.ShowtimeRepository;
import com.cinema.backend.domain.showtime.ShowtimeService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ReservationSeatConstraintTest {

    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationSeatRepository reservationSeatRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private MovieService movieService;
    @Autowired private ScreenService screenService;
    @Autowired private SeatGradeService seatGradeService;
    @Autowired private SeatService seatService;
    @Autowired private ShowtimeService showtimeService;
    @Autowired private MemberService memberService;
    @Autowired private EntityManager entityManager;

    private Showtime showtime;
    private Seat seat;
    private Member member;

    @BeforeEach
    void setUp() {

        // 영화·상영관·좌석·회원·상영 일정은 기존 통합 테스트와 같은 방식(서비스 메서드)으로 준비
        Long movieId = movieService.createMovie("제약검사 테스트 영화", null, null, null, 120).getId();
        movieService.updateMovie(movieId, "제약검사 테스트 영화", null, "SHOWING", "ACTIVE", null, null, 120);

        Long screenId = screenService.createScreen("제약검사 테스트관").getId();
        screenService.updateScreen(screenId, "제약검사 테스트관", "ACTIVE");

        Long gradeId = seatGradeService.createSeatGrade("제약검사 등급", 10000, null).getId();
        Long seatId = seatService.createSeat(screenId, gradeId, "A", 1, null).getId();

        Long showtimeId = showtimeService.createShowtime(movieId, screenId, LocalDateTime.of(2099, 1, 1, 13, 0)).getId();
        showtimeService.updateShowtimeStatus(showtimeId, "ACTIVE");

        Long memberId = memberService.createMember("constraint@test.com", "password1234", "제약검사회원").getId();

        // 이 테스트는 서비스의 "이미 예약된 좌석" 검사가 아니라 DB 유니크 제약 자체를 검증 대상으로 하므로,
        // 예약은 서비스가 아니라 엔티티 메서드로 직접 만든다(D-37 규칙 2의 예외 허용).
        // 그러려면 Reservation.online()·addSeat()에 넘길 영속 상태의 엔티티가 필요해 리포지토리로 다시 조회한다
        showtime = showtimeRepository.findById(showtimeId).orElseThrow();
        seat = seatRepository.findById(seatId).orElseThrow();
        member = memberRepository.findById(memberId).orElseThrow();
    }

    @Test
    @DisplayName("같은 상영 일정·좌석에 활성 예약 좌석을 2개 저장하면 유니크 제약 위반으로 거부된다 (DataIntegrityViolationException)")
    void duplicateActiveReservationSeat_violatesUniqueConstraint() {

        Reservation first = Reservation.online(member, showtime);
        first.addSeat(seat, 10000);
        reservationRepository.save(first);
        // ReservationSeat의 @Id는 IDENTITY 전략이라 DB가 값을 생성해야 하므로,
        // Hibernate는 save()(내부적으로 persist())가 호출되는 즉시 INSERT를 실행하고 flush를 기다리지 않는다.
        // 그래서 첫 번째 행은 이 지점에서 이미 DB에 반영되어 있음을 flush로 명시한다(실제로는 중복 호출)
        entityManager.flush();

        Reservation second = Reservation.online(member, showtime);
        second.addSeat(seat, 10000);

        // 같은 이유(IDENTITY + cascade = PERSIST)로 제약 위반은 별도 flush가 아니라
        // 이 save() 호출 자체에서 발생한다
        assertThatThrownBy(() -> reservationRepository.save(second))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_reservation_seats_showtime_seat_active");
    }

    @Test
    @DisplayName("같은 상영 일정·좌석이라도 취소된 예약 좌석(활성 표시 NULL)은 중복으로 보지 않는다")
    void canceledReservationSeats_areNotTreatedAsDuplicate() {

        Reservation first = Reservation.online(member, showtime);
        first.addSeat(seat, 10000);
        reservationRepository.save(first);
        entityManager.flush();

        first.cancel();
        // cancel()이 ReservationSeat.active를 null로 바꾸는 변경은 변경 감지(dirty checking)로 커밋 시점에 반영되지만,
        // 다음 활성 예약 좌석을 insert하기 전에 이 UPDATE가 먼저 DB에 반영되어야
        // (여전히 active=true인 상태에서 새 행을 끼워 넣어 발생하는) 거짓 제약 위반을 피할 수 있어 즉시 flush한다
        entityManager.flush();

        Reservation second = Reservation.online(member, showtime);
        second.addSeat(seat, 10000);
        reservationRepository.save(second);
        entityManager.flush();

        second.cancel();
        entityManager.flush();

        Reservation third = Reservation.online(member, showtime);
        third.addSeat(seat, 10000);
        reservationRepository.save(third);
        entityManager.flush();

        assertThat(reservationSeatRepository.countByShowtimeIdAndSeatIdAndActiveTrue(showtime.getId(), seat.getId()))
                .isEqualTo(1);

        Long totalCount = entityManager.createQuery(
                        "select count(rs) from ReservationSeat rs where rs.showtime.id = :showtimeId and rs.seat.id = :seatId",
                        Long.class)
                .setParameter("showtimeId", showtime.getId())
                .setParameter("seatId", seat.getId())
                .getSingleResult();
        assertThat(totalCount).isEqualTo(3L);
    }
}
