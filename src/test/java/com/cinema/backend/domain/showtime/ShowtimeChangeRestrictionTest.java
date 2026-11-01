/***
 * 상영 일정 변경 제한(활성 예약이 있는 경우 시간 변경 · 비활성화 거부, D-44) 통합 테스트를 담당하는 클래스
 * 작성이력
 * -2026-11-01: 파일 생성 및 작성 (ShowtimeOverlapTest.java · ReservationConditionTest.java 구조 참고)
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.member.MemberService;
import com.cinema.backend.domain.movie.MovieService;
import com.cinema.backend.domain.reservation.ReservationService;
import com.cinema.backend.domain.reservation.dto.ReservationResponse;
import com.cinema.backend.domain.screen.ScreenService;
import com.cinema.backend.domain.screen.SeatGradeService;
import com.cinema.backend.domain.screen.SeatService;
import com.cinema.backend.domain.showtime.dto.ShowtimeResponse;
import com.cinema.backend.global.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ShowtimeChangeRestrictionTest {

    @Autowired private ShowtimeService showtimeService;
    @Autowired private ReservationService reservationService;
    @Autowired private MovieService movieService;
    @Autowired private ScreenService screenService;
    @Autowired private SeatGradeService seatGradeService;
    @Autowired private SeatService seatService;
    @Autowired private MemberService memberService;

    private Long movieId;
    private Long screenId;
    private Long seatId;
    private Long showtimeId; // 활성화된 상영 일정 (2099-01-01 13:00)
    private String memberEmail;
    private String staffEmail;
    private Long staffId;

    @BeforeEach
    void setUp() {

        // 영화: 공개(ACTIVE) · 상영 중 (ReservationConditionTest와 같은 준비 방식)
        movieId = movieService.createMovie("변경제한 테스트 영화", null, null, null, 120).getId();
        movieService.updateMovie(movieId, "변경제한 테스트 영화", null, "SHOWING", "ACTIVE", null, null, 120);

        // 상영관: 생성 시 CLOSED이므로 ACTIVE로 변경
        screenId = createActiveScreen("변경제한 테스트관");

        Long gradeId = seatGradeService.createSeatGrade("변경제한-일반", 10000, null).getId();
        seatId = seatService.createSeat(screenId, gradeId, "A", 1, null).getId();

        // 상영 일정: 2099년(D-37), 활성화
        showtimeId = showtimeService.createShowtime(movieId, screenId, at(1, 13, 0)).getId();
        showtimeService.updateShowtimeStatus(showtimeId, "ACTIVE");

        memberEmail = "restriction@test.com";
        memberService.createMember(memberEmail, "password1234", "변경제한회원");

        // 현장 판매 시나리오(3번)에서 쓰는 직원 계정 (ReservationBoxOfficeTest와 같은 준비 방식)
        staffEmail = "restriction-staff@test.com";
        staffId = memberService.createMember(staffEmail, "password1234", "변경제한직원").getId();
        memberService.updateMember(staffId, "변경제한직원", "STAFF", false);
    }

    @Test
    @DisplayName("활성 예약이 있는 상영 일정은 시간을 변경할 수 없다 (409), 시작 시각은 그대로다")
    void activeReservation_updateSchedule_rejected() {

        reservationService.createOnlineReservation(memberEmail, showtimeId, List.of(seatId));

        LocalDateTime originalStartTime = showtimeService.findById(showtimeId).getStartTime();

        assertThatThrownBy(() -> showtimeService.updateSchedule(showtimeId, at(2, 13, 0)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("예매된 좌석이 있는 상영정보는 일정을 변경할 수 없습니다.");

        // ShowtimeService.updateSchedule()은 예약 확인이 일정 변경보다 먼저라 시작 시각이 바뀌지 않는다
        assertThat(showtimeService.findById(showtimeId).getStartTime()).isEqualTo(originalStartTime);
    }

    @Test
    @DisplayName("활성 예약이 있는 상영 일정은 비활성화할 수 없다 (409)")
    void activeReservation_deactivate_rejected() {

        reservationService.createOnlineReservation(memberEmail, showtimeId, List.of(seatId));

        /*
         * ShowtimeService.updateShowtimeStatus()는 엔티티 상태를 먼저 바꾼 뒤 예약을 확인해 예외를 던진다.
         * 이 테스트는 @Transactional이라 서비스가 테스트 트랜잭션에 합류하므로, 예외가 발생해도
         * 메모리상 엔티티는 DEACTIVATED로 바뀐 채 남아 있고(실제 DB 커밋은 테스트 종료 시 전체 롤백) 그대로
         * 다시 조회해도 바뀐 값을 보게 된다. 그래서 여기서는 "상태가 그대로인지"를 확인하지 않는다.
         * 실제 커밋 기준의 확인은 ShowtimeConcurrencyTest 9번 시나리오에서 한다.
         */
        assertThatThrownBy(() -> showtimeService.updateShowtimeStatus(showtimeId, "DEACTIVATED"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("예매된 좌석이 있는 상영정보는 비활성화할 수 없습니다.");
    }

    @Test
    @DisplayName("현장 판매(직원 예약)만 있어도 비활성화할 수 없다 (409, 예약 경로와 무관하게 거부)")
    void boxOfficeOnlyReservation_deactivate_rejected() {

        reservationService.createBoxOfficeWalkInReservation(staffEmail, showtimeId, List.of(seatId));

        assertThatThrownBy(() -> showtimeService.updateShowtimeStatus(showtimeId, "DEACTIVATED"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("예매된 좌석이 있는 상영정보는 비활성화할 수 없습니다.");
    }

    @Test
    @DisplayName("예약을 모두 취소하면 시간 변경에 성공한다")
    void afterCancelAllReservations_updateSchedule_success() {

        ReservationResponse reservation = reservationService.createOnlineReservation(
                memberEmail, showtimeId, List.of(seatId));
        reservationService.cancelMyReservation(memberEmail, reservation.getId());

        ShowtimeResponse updated = showtimeService.updateSchedule(showtimeId, at(2, 13, 0));

        assertThat(updated.getStartTime()).isEqualTo(at(2, 13, 0));
    }

    @Test
    @DisplayName("예약을 모두 취소하면 비활성화에 성공한다")
    void afterCancelAllReservations_deactivate_success() {

        ReservationResponse reservation = reservationService.createOnlineReservation(
                memberEmail, showtimeId, List.of(seatId));
        reservationService.cancelMyReservation(memberEmail, reservation.getId());

        ShowtimeResponse updated = showtimeService.updateShowtimeStatus(showtimeId, "DEACTIVATED");

        assertThat(updated.getShowtimeStatus()).isEqualTo(Showtime.ShowtimeStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("예약이 없는 상영 일정은 시간 변경에 성공한다 (기존 동작 유지)")
    void noReservation_updateSchedule_success() {

        ShowtimeResponse updated = showtimeService.updateSchedule(showtimeId, at(2, 13, 0));

        assertThat(updated.getStartTime()).isEqualTo(at(2, 13, 0));
    }

    @Test
    @DisplayName("예약이 없는 상영 일정은 비활성화에 성공한다 (기존 동작 유지)")
    void noReservation_deactivate_success() {

        ShowtimeResponse updated = showtimeService.updateShowtimeStatus(showtimeId, "DEACTIVATED");

        assertThat(updated.getShowtimeStatus()).isEqualTo(Showtime.ShowtimeStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("상태 값이 null이면 거부된다 (400, 엔티티 검사가 먼저 걸려 500이 되지 않는다)")
    void nullStatus_rejected() {

        assertThatThrownBy(() -> showtimeService.updateShowtimeStatus(showtimeId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상영정보상태는 비워둘 수 없습니다.");
    }

    // 테스트용 상영관 생성: 생성 시 CLOSED이므로 ACTIVE로 변경 (ReservationConditionTest와 같은 방식)
    private Long createActiveScreen(String name) {

        Long id = screenService.createScreen(name).getId();
        screenService.updateScreen(id, name, "ACTIVE");
        return id;
    }

    // 2099년 기준 시각 (엔티티가 과거 시각을 거부하므로 먼 미래 날짜 사용, day로 일정 간 겹침을 피함)
    private LocalDateTime at(int day, int hour, int minute) {

        return LocalDateTime.of(2099, 1, day, hour, minute);
    }
}
