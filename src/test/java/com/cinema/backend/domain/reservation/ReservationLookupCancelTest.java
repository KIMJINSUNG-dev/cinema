/***
 * ReservationService의 예매 내역 조회 · 취소(고객용 · 직원용) 통합 테스트를 담당하는 클래스
 * 작성이력
 * -2026-10-31: 파일 생성 및 작성 (ReservationConditionTest.java 구조 참고)
 * -2026-10-31: 직원 목록 조회: 존재하지 않는 회원 id 거부(400) 시나리오 추가
 *
 * 1번(최신순 정렬) 테스트: 같은 트랜잭션 안에서 연속으로 예약을 생성하면 예매 일시(createdAt)가
 * 같은 값으로 저장될 수 있어(D-37 규칙 3이 과거 시각 검증에 2099년을 쓰는 것과 같은 이유로,
 * 실제 시스템 시각에 기대면 정렬 검증이 불안정해짐), ReservationConditionTest의 "이미 시작된 상영 일정"
 * 테스트와 같은 방식(D-37 규칙 2의 예외, JPQL 벌크 업데이트 + entityManager.clear())으로
 * 예매 일시를 서로 다른 값으로 직접 지정한 뒤 정렬을 검증한다.
 *
 * 7 · 12번(취소 마감) 테스트: 상영 시작까지 15분 미만인 상영 일정은 Showtime이 과거 시각만 거부하므로
 * (미래 시각이면 생성 가능) 실제 서비스(ShowtimeService.createShowtime)로 만들 수 있다.
 * 상영 시작을 "현재 시각 + 5분"으로 두면 취소 마감 시각(상영 시작 - 15분)은 생성 시점에 이미
 * "현재 시각 - 10분"이 되어 테스트 실행 중 어떤 지연이 있어도(최대 수 분) 항상 마감을 지난 상태이므로
 * 실행 시점에 따른 실패 위험이 없다.
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.member.MemberService;
import com.cinema.backend.domain.movie.MovieService;
import com.cinema.backend.domain.reservation.dto.ReservationResponse;
import com.cinema.backend.domain.screen.ScreenService;
import com.cinema.backend.domain.screen.SeatGradeService;
import com.cinema.backend.domain.screen.SeatService;
import com.cinema.backend.domain.showtime.ShowtimeService;
import com.cinema.backend.global.exception.ConflictException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ReservationLookupCancelTest {

    @Autowired private ReservationService reservationService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationSeatRepository reservationSeatRepository;
    @Autowired private MovieService movieService;
    @Autowired private ScreenService screenService;
    @Autowired private SeatGradeService seatGradeService;
    @Autowired private SeatService seatService;
    @Autowired private ShowtimeService showtimeService;
    @Autowired private MemberService memberService;
    @Autowired private EntityManager entityManager;

    private Long movieId;
    private Long screenId;
    private Long gradeId;
    private Long showtimeId; // 활성화된 상영 일정 (2099-01-01 13:00)
    private String ownerEmail;
    private Long ownerMemberId;
    private String otherEmail;

    @BeforeEach
    void setUp() {

        movieId = movieService.createMovie("조회취소 테스트 영화", null, null, null, 120).getId();
        movieService.updateMovie(movieId, "조회취소 테스트 영화", null, "SHOWING", "ACTIVE", null, null, 120);

        screenId = createActiveScreen("조회취소 테스트관");

        gradeId = seatGradeService.createSeatGrade("조회취소-일반", 10000, null).getId();

        showtimeId = showtimeService.createShowtime(movieId, screenId, at(1, 13, 0)).getId();
        showtimeService.updateShowtimeStatus(showtimeId, "ACTIVE");

        ownerEmail = "lookup-owner@test.com";
        ownerMemberId = memberService.createMember(ownerEmail, "password1234", "조회취소본인").getId();

        otherEmail = "lookup-other@test.com";
        memberService.createMember(otherEmail, "password1234", "조회취소타인");
    }

    @Test
    @DisplayName("고객 목록: 본인 예약만 예매 일시 최신순으로 조회되고, 취소된 예약도 포함된다")
    void findMyReservations_ownOnly_newestFirst_includesCanceled() {

        Long seat1Id = createSeat("D", 1);
        Long seat2Id = createSeat("D", 2);
        Long seat3Id = createSeat("D", 3);
        Long otherSeatId = createSeat("D", 4);

        Long res1Id = reservationService.createOnlineReservation(ownerEmail, showtimeId, List.of(seat1Id)).getId();
        Long res2Id = reservationService.createOnlineReservation(ownerEmail, showtimeId, List.of(seat2Id)).getId();
        Long res3Id = reservationService.createOnlineReservation(ownerEmail, showtimeId, List.of(seat3Id)).getId();
        reservationService.createOnlineReservation(otherEmail, showtimeId, List.of(otherSeatId));

        // 예매 일시를 서로 다른 값으로 직접 지정(클래스 주석 참고)
        setCreatedAt(res1Id, LocalDateTime.of(2099, 1, 1, 0, 0, 1));
        setCreatedAt(res2Id, LocalDateTime.of(2099, 1, 1, 0, 0, 2));
        setCreatedAt(res3Id, LocalDateTime.of(2099, 1, 1, 0, 0, 3));
        entityManager.clear();

        reservationService.cancelMyReservation(ownerEmail, res2Id);

        // 컨트롤러의 @PageableDefault(createdAt desc)가 실제로 전달하는 정렬을 서비스 테스트에서 직접 지정
        Page<ReservationResponse> page = reservationService.findMyReservations(
                ownerEmail, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent())
                .extracting(ReservationResponse::getId)
                .containsExactly(res3Id, res2Id, res1Id);
        assertThat(page.getContent())
                .filteredOn(r -> r.getId().equals(res2Id))
                .extracting(ReservationResponse::getReservationStatus)
                .containsExactly(Reservation.ReservationStatus.CANCELED);
    }

    @Test
    @DisplayName("고객 단건: 본인 예약을 조회하면 예매 일시가 채워진 응답을 반환한다")
    void findMyReservation_own_success() {

        Long seatId = createSeat("A", 1);
        Long resId = reservationService.createOnlineReservation(
                ownerEmail, showtimeId, List.of(seatId)).getId();

        ReservationResponse response = reservationService.findMyReservation(ownerEmail, resId);

        assertThat(response.getId()).isEqualTo(resId);
        assertThat(response.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
        assertThat(response.getReservedAt()).isNotNull();
    }

    @Test
    @DisplayName("고객 단건: 다른 회원의 예약을 조회하면 거부된다 (403)")
    void findMyReservation_otherMember_accessDenied() {

        Long seatId = createSeat("A", 1);
        Long resId = reservationService.createOnlineReservation(
                otherEmail, showtimeId, List.of(seatId)).getId();

        assertThatThrownBy(() -> reservationService.findMyReservation(ownerEmail, resId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("권한이 없습니다.");
    }

    @Test
    @DisplayName("고객 취소: 본인 예약을 취소하면 좌석이 반환되어 다른 회원이 다시 예약할 수 있다")
    void cancelMyReservation_own_success() {

        Long seatId = createSeat("A", 1);
        Long resId = reservationService.createOnlineReservation(
                ownerEmail, showtimeId, List.of(seatId)).getId();

        ReservationResponse response = reservationService.cancelMyReservation(ownerEmail, resId);

        assertThat(response.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CANCELED);
        assertThat(reservationSeatRepository.countByShowtimeIdAndSeatIdAndActiveTrue(showtimeId, seatId)).isZero();

        ReservationResponse rebooked = reservationService.createOnlineReservation(
                otherEmail, showtimeId, List.of(seatId));
        assertThat(rebooked.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("고객 취소: 다른 회원의 예약을 취소하려 하면 거부되고 예약 상태는 그대로다 (403)")
    void cancelMyReservation_otherMember_accessDenied() {

        Long seatId = createSeat("A", 1);
        Long resId = reservationService.createOnlineReservation(
                otherEmail, showtimeId, List.of(seatId)).getId();

        assertThatThrownBy(() -> reservationService.cancelMyReservation(ownerEmail, resId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("권한이 없습니다.");

        Reservation reservation = reservationRepository.findById(resId).orElseThrow();
        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("고객 취소: 이미 취소된 예약을 다시 취소하면 거부된다 (409)")
    void cancelMyReservation_alreadyCanceled_conflict() {

        Long seatId = createSeat("A", 1);
        Long resId = reservationService.createOnlineReservation(
                ownerEmail, showtimeId, List.of(seatId)).getId();
        reservationService.cancelMyReservation(ownerEmail, resId);

        assertThatThrownBy(() -> reservationService.cancelMyReservation(ownerEmail, resId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("취소 상태의 예약은 취소할 수 없습니다.");
    }

    @Test
    @DisplayName("고객 취소: 취소 마감 시각이 지난 예약은 취소할 수 없고 상태는 그대로다 (409)")
    void cancelMyReservation_afterDeadline_conflict() {

        Long soonShowtimeId = createSoonActiveShowtime();
        Long seatId = createSeat("E", 1);
        Long resId = reservationService.createOnlineReservation(
                ownerEmail, soonShowtimeId, List.of(seatId)).getId();

        assertThatThrownBy(() -> reservationService.cancelMyReservation(ownerEmail, resId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("취소 가능 시간이 지난 예약입니다.");

        Reservation reservation = reservationRepository.findById(resId).orElseThrow();
        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("탈퇴한 회원은 고객용 예매 내역을 조회할 수 없다 (400)")
    void withdrawnMember_customerLookup_rejected() {

        String withdrawnEmail = "lookup-withdrawn@test.com";
        Long withdrawnId = memberService.createMember(withdrawnEmail, "password1234", "조회취소탈퇴").getId();
        memberService.updateMember(withdrawnId, "조회취소탈퇴", "CUSTOMER", true);

        assertThatThrownBy(() -> reservationService.findMyReservations(withdrawnEmail, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("탈퇴한 회원정보입니다.");
    }

    @Test
    @DisplayName("직원 목록: memberId로 조회하면 해당 회원의 예약만 조회된다")
    void findReservationsByMemberId_onlyThatMember() {

        Long seatId = createSeat("A", 1);
        Long otherSeatId = createSeat("A", 2);

        Long ownerResId = reservationService.createOnlineReservation(
                ownerEmail, showtimeId, List.of(seatId)).getId();
        reservationService.createOnlineReservation(otherEmail, showtimeId, List.of(otherSeatId));

        Page<ReservationResponse> page = reservationService.findReservationsByMemberId(
                ownerMemberId, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(ReservationResponse::getId)
                .containsExactly(ownerResId);
    }

    @Test
    @DisplayName("직원 목록: 존재하지 않는 회원 id로 조회하면 거부된다 (400)")
    void findReservationsByMemberId_nonExistentMember_rejected() {

        assertThatThrownBy(() -> reservationService.findReservationsByMemberId(
                999_999_999L, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 회원정보입니다.");
    }

    @Test
    @DisplayName("직원 단건: 다른 회원의 예약도 조회할 수 있다")
    void findReservation_otherMember_success() {

        Long seatId = createSeat("A", 1);
        Long resId = reservationService.createOnlineReservation(
                otherEmail, showtimeId, List.of(seatId)).getId();

        ReservationResponse response = reservationService.findReservation(resId);

        assertThat(response.getId()).isEqualTo(resId);
    }

    @Test
    @DisplayName("직원 취소: 다른 회원의 예약을 취소하면 좌석 활성 표시가 해제된다")
    void cancelReservation_otherMember_success() {

        Long seatId = createSeat("A", 1);
        Long resId = reservationService.createOnlineReservation(
                otherEmail, showtimeId, List.of(seatId)).getId();

        ReservationResponse response = reservationService.cancelReservation(resId);

        assertThat(response.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CANCELED);
        assertThat(reservationSeatRepository.countByShowtimeIdAndSeatIdAndActiveTrue(showtimeId, seatId)).isZero();
    }

    @Test
    @DisplayName("직원 취소: 취소 마감 시각이 지난 예약은 취소할 수 없다 (409)")
    void cancelReservation_afterDeadline_conflict() {

        Long soonShowtimeId = createSoonActiveShowtime();
        Long seatId = createSeat("E", 1);
        Long resId = reservationService.createOnlineReservation(
                otherEmail, soonShowtimeId, List.of(seatId)).getId();

        assertThatThrownBy(() -> reservationService.cancelReservation(resId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("취소 가능 시간이 지난 예약입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 예약을 조회하면 거부된다 (400)")
    void nonExistentReservation_rejected() {

        assertThatThrownBy(() -> reservationService.findReservation(999_999_999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 예약정보입니다.");
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

    // 취소 마감(15분)보다 가까운 상영 일정: 클래스 상단 주석 참고
    private Long createSoonActiveShowtime() {

        Long id = showtimeService.createShowtime(movieId, screenId, LocalDateTime.now().plusMinutes(5)).getId();
        showtimeService.updateShowtimeStatus(id, "ACTIVE");
        return id;
    }

    // 예매 일시(createdAt) 직접 지정: 클래스 상단 주석 참고 (D-37 규칙 2의 예외)
    private void setCreatedAt(Long reservationId, LocalDateTime createdAt) {

        entityManager.createQuery("update Reservation r set r.createdAt = :createdAt where r.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", reservationId)
                .executeUpdate();
    }

    // 2099년 기준 시각 (엔티티가 과거 시각을 거부하므로 먼 미래 날짜 사용, ReservationConditionTest와 같은 방식)
    private LocalDateTime at(int day, int hour, int minute) {

        return LocalDateTime.of(2099, 1, day, hour, minute);
    }
}
