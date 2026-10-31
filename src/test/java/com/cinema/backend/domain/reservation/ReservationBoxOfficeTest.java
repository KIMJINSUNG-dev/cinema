/***
 * ReservationService의 직원 예약(회원 대리 예약 · 현장 판매, D-43) 통합 테스트를 담당하는 클래스
 * 작성이력
 * -2026-10-31: 파일 생성 및 작성 (ReservationConditionTest.java 구조 참고)
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.member.Member;
import com.cinema.backend.domain.member.MemberService;
import com.cinema.backend.domain.member.dto.MemberResponse;
import com.cinema.backend.domain.movie.MovieService;
import com.cinema.backend.domain.reservation.dto.ReservationResponse;
import com.cinema.backend.domain.screen.ScreenService;
import com.cinema.backend.domain.screen.SeatGradeService;
import com.cinema.backend.domain.screen.SeatService;
import com.cinema.backend.domain.showtime.ShowtimeService;
import com.cinema.backend.global.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ReservationBoxOfficeTest {

    @Autowired private ReservationService reservationService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private MovieService movieService;
    @Autowired private ScreenService screenService;
    @Autowired private SeatGradeService seatGradeService;
    @Autowired private SeatService seatService;
    @Autowired private ShowtimeService showtimeService;
    @Autowired private MemberService memberService;
    @Autowired private WalkInAccountInitializer walkInAccountInitializer;

    @Value("${cinema.reservation.walk-in-account-email}")
    private String walkInAccountEmail;

    private Long movieId;
    private Long screenId;
    private Long gradeId;
    private Long showtimeId; // 활성화된 상영 일정 (2099-01-01 13:00)
    private String staffEmail;
    private Long staffId;
    private String targetEmail;
    private Long targetMemberId;

    @BeforeEach
    void setUp() {

        movieId = movieService.createMovie("매표소 테스트 영화", null, null, null, 120).getId();
        movieService.updateMovie(movieId, "매표소 테스트 영화", null, "SHOWING", "ACTIVE", null, null, 120);

        screenId = createActiveScreen("매표소 테스트관");

        gradeId = seatGradeService.createSeatGrade("매표소-일반", 10000, null).getId();

        showtimeId = showtimeService.createShowtime(movieId, screenId, at(1, 13, 0)).getId();
        showtimeService.updateShowtimeStatus(showtimeId, "ACTIVE");

        staffEmail = "boxoffice-staff@test.com";
        staffId = memberService.createMember(staffEmail, "password1234", "매표소직원").getId();
        memberService.updateMember(staffId, "매표소직원", "STAFF", false);

        targetEmail = "boxoffice-target@test.com";
        targetMemberId = memberService.createMember(targetEmail, "password1234", "대리예약대상").getId();
    }

    @Test
    @DisplayName("대리 예약: 성공하면 주인은 대상 회원, 처리 직원은 로그인한 직원이다")
    void createBoxOfficeMemberReservation_success() {

        Long seatId = createSeat("A", 1);

        ReservationResponse response = reservationService.createBoxOfficeMemberReservation(
                staffEmail, targetMemberId, showtimeId, List.of(seatId));

        assertThat(response.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
        assertThat(response.getChannel()).isEqualTo(Reservation.Channel.BOX_OFFICE);

        Reservation reservation = reservationRepository.findById(response.getId()).orElseThrow();
        assertThat(reservation.getMember().getEmail()).isEqualTo(targetEmail);
        assertThat(reservation.getHandledBy().getEmail()).isEqualTo(staffEmail);
        assertThat(reservation.getReservationSeats())
                .extracting(rs -> rs.getSeat().getId(), ReservationSeat::getPrice)
                .containsExactly(tuple(seatId, 10000));
    }

    @Test
    @DisplayName("현장 판매: 성공하면 주인은 시스템 계정, 처리 직원은 로그인한 직원이다")
    void createBoxOfficeWalkInReservation_success() {

        Long seatId = createSeat("A", 1);

        ReservationResponse response = reservationService.createBoxOfficeWalkInReservation(
                staffEmail, showtimeId, List.of(seatId));

        assertThat(response.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
        assertThat(response.getChannel()).isEqualTo(Reservation.Channel.BOX_OFFICE);

        Reservation reservation = reservationRepository.findById(response.getId()).orElseThrow();
        assertThat(reservation.getMember().getEmail()).isEqualTo(walkInAccountEmail);
        assertThat(reservation.getHandledBy().getEmail()).isEqualTo(staffEmail);
    }

    @Test
    @DisplayName("현장 판매 시스템 계정은 앱 시작 시 생성되고, 다시 초기화해도 계정은 그대로 1개다")
    void walkInAccountInitializer_idempotent() {

        MemberResponse before = memberService.findByEmail(walkInAccountEmail);
        assertThat(before.getRole()).isEqualTo(Member.Role.CUSTOMER);

        walkInAccountInitializer.run(null);

        MemberResponse after = memberService.findByEmail(walkInAccountEmail);
        assertThat(after.getId()).isEqualTo(before.getId());
    }

    @Test
    @DisplayName("대리 예약: 대상 회원이 존재하지 않으면 거부된다 (400)")
    void createBoxOfficeMemberReservation_targetNotFound_rejected() {

        Long seatId = createSeat("A", 1);

        assertThatThrownBy(() -> reservationService.createBoxOfficeMemberReservation(
                staffEmail, 999_999_999L, showtimeId, List.of(seatId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 회원정보입니다.");
    }

    @Test
    @DisplayName("대리 예약: 대상 회원이 탈퇴했으면 거부된다 (400)")
    void createBoxOfficeMemberReservation_targetWithdrawn_rejected() {

        String withdrawnEmail = "boxoffice-withdrawn@test.com";
        Long withdrawnId = memberService.createMember(withdrawnEmail, "password1234", "탈퇴대상").getId();
        memberService.updateMember(withdrawnId, "탈퇴대상", "CUSTOMER", true);

        Long seatId = createSeat("A", 1);

        assertThatThrownBy(() -> reservationService.createBoxOfficeMemberReservation(
                staffEmail, withdrawnId, showtimeId, List.of(seatId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("탈퇴한 회원정보입니다.");
    }

    @Test
    @DisplayName("대리 예약: 대상이 현장 판매 시스템 계정이면 거부된다 (400)")
    void createBoxOfficeMemberReservation_targetIsWalkInAccount_rejected() {

        Long walkInAccountId = memberService.findByEmail(walkInAccountEmail).getId();
        Long seatId = createSeat("A", 1);

        assertThatThrownBy(() -> reservationService.createBoxOfficeMemberReservation(
                staffEmail, walkInAccountId, showtimeId, List.of(seatId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현장 판매 시스템 계정은 대리 예약 대상이 될 수 없습니다.");
    }

    @Test
    @DisplayName("처리 직원의 DB 역할이 CUSTOMER로 강등되면 거부된다 (403)")
    void createBoxOfficeMemberReservation_demotedStaff_accessDenied() {

        memberService.updateMember(staffId, "매표소직원", "CUSTOMER", false);
        Long seatId = createSeat("A", 1);

        assertThatThrownBy(() -> reservationService.createBoxOfficeMemberReservation(
                staffEmail, targetMemberId, showtimeId, List.of(seatId)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("권한이 없습니다.");
    }

    @Test
    @DisplayName("처리 직원이 탈퇴했으면 거부된다 (403)")
    void createBoxOfficeMemberReservation_withdrawnStaff_accessDenied() {

        memberService.updateMember(staffId, "매표소직원", "STAFF", true);
        Long seatId = createSeat("A", 1);

        assertThatThrownBy(() -> reservationService.createBoxOfficeMemberReservation(
                staffEmail, targetMemberId, showtimeId, List.of(seatId)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("권한이 없습니다.");
    }

    @Test
    @DisplayName("대리 예약: 이미 예약된 좌석은 거부된다 (409, 공통 로직 사용 확인)")
    void createBoxOfficeMemberReservation_alreadyReservedSeat_conflict() {

        Long seatId = createSeat("A", 1);
        reservationService.createBoxOfficeMemberReservation(staffEmail, targetMemberId, showtimeId, List.of(seatId));

        assertThatThrownBy(() -> reservationService.createBoxOfficeMemberReservation(
                staffEmail, targetMemberId, showtimeId, List.of(seatId)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 예약된 좌석입니다.");
    }

    @Test
    @DisplayName("현장 판매: 온라인 고객이 먼저 예약한 좌석은 거부된다 (409)")
    void createBoxOfficeWalkInReservation_seatReservedByOnlineCustomer_conflict() {

        Long seatId = createSeat("A", 1);
        reservationService.createOnlineReservation(targetEmail, showtimeId, List.of(seatId));

        assertThatThrownBy(() -> reservationService.createBoxOfficeWalkInReservation(
                staffEmail, showtimeId, List.of(seatId)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 예약된 좌석입니다.");
    }

    // 테스트용 상영관 생성: 생성 시 CLOSED이므로 ACTIVE로 변경 (ReservationConditionTest와 같은 방식)
    private Long createActiveScreen(String name) {

        Long id = screenService.createScreen(name).getId();
        screenService.updateScreen(id, name, "ACTIVE");
        return id;
    }

    private Long createSeat(String row, int column) {

        return seatService.createSeat(screenId, gradeId, row, column, null).getId();
    }

    // 2099년 기준 시각 (엔티티가 과거 시각을 거부하므로 먼 미래 날짜 사용, ReservationConditionTest와 같은 방식)
    private LocalDateTime at(int day, int hour, int minute) {

        return LocalDateTime.of(2099, 1, day, hour, minute);
    }
}
