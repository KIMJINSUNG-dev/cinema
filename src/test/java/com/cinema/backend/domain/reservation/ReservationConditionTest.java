/***
 * ReservationService.createOnlineReservation()의 예약 조건 검사(D-42) 통합 테스트를 담당하는 클래스
 * 작성이력
 * -2026-10-29: 파일 생성 및 작성
 * -2026-10-30: createOnlineReservation() 반환 타입 변경(ReservationResponse)에 따른 성공 테스트 수정,
 *              상영 일정 id null 검사 거부 시나리오 추가
 * -2026-10-30: 성공 테스트에 ReservationResponse 응답 검증(상태 · 경로 · 상영 정보 · 좌석 목록 · 총액) 추가
 * -2026-10-31: 성공 테스트에 reservedAt(예매 일시) 채워짐 확인 1줄 추가 (예매 내역 조회 · 취소 API)
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.member.MemberService;
import com.cinema.backend.domain.movie.MovieService;
import com.cinema.backend.domain.reservation.dto.ReservationResponse;
import com.cinema.backend.domain.reservation.dto.ReservationSeatResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ReservationConditionTest {

    @Autowired private ReservationService reservationService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private MovieService movieService;
    @Autowired private ScreenService screenService;
    @Autowired private SeatGradeService seatGradeService;
    @Autowired private SeatService seatService;
    @Autowired private ShowtimeService showtimeService;
    @Autowired private MemberService memberService;
    @Autowired private EntityManager entityManager;

    @Value("${cinema.reservation.max-reservation-seat-count}")
    private int maxReservationSeatCount;

    private Long movieId;
    private Long screenId;
    private Long gradeAId; // 조건검사-일반, 기준 가격 10000원
    private Long gradeBId; // 조건검사-VIP, 기준 가격 20000원
    private Long seatA1Id;
    private Long seatB1Id;
    private Long showtimeId; // 활성화된 상영 일정 (2099-01-01 13:00)
    private String memberEmail;

    @BeforeEach
    void setUp() {

        // 영화: 공개(ACTIVE) · 상영 중 (ReservationConcurrencyTest와 같은 준비 방식)
        movieId = movieService.createMovie("조건검사 테스트 영화", null, null, null, 120).getId();
        movieService.updateMovie(movieId, "조건검사 테스트 영화", null, "SHOWING", "ACTIVE", null, null, 120);

        // 상영관: 생성 시 CLOSED이므로 ACTIVE로 변경
        screenId = createActiveScreen("조건검사 테스트관");

        // 기준 가격이 다른 좌석 등급 2개 (1번 시나리오의 좌석별 가격 검증용)
        gradeAId = seatGradeService.createSeatGrade("조건검사-일반", 10000, null).getId();
        gradeBId = seatGradeService.createSeatGrade("조건검사-VIP", 20000, null).getId();

        seatA1Id = seatService.createSeat(screenId, gradeAId, "A", 1, null).getId();
        seatB1Id = seatService.createSeat(screenId, gradeBId, "B", 1, null).getId();

        // 상영 일정: 2099년(D-37), 활성화
        showtimeId = showtimeService.createShowtime(movieId, screenId, at(1, 13, 0)).getId();
        showtimeService.updateShowtimeStatus(showtimeId, "ACTIVE");

        memberEmail = "condition@test.com";
        memberService.createMember(memberEmail, "password1234", "조건검사회원");
    }

    @Test
    @DisplayName("상영 일정 id가 없으면 거부된다 (400)")
    void nullShowtimeId_rejected() {

        assertThatThrownBy(() -> reservationService.createOnlineReservation(
                memberEmail, null, List.of(seatA1Id)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 상영정보입니다.");
    }

    @Test
    @DisplayName("정상 예약: 기준 가격이 다른 좌석 2석을 예약하면 확정 상태로 저장되고 좌석별 가격이 등급 기준 가격과 같다")
    void createOnlineReservation_success() {

        ReservationResponse response = reservationService.createOnlineReservation(
                memberEmail, showtimeId, List.of(seatA1Id, seatB1Id));

        // 응답 DTO 검증: 상태 · 경로 · 상영 정보 · 좌석 목록 · 총액
        assertThat(response.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
        assertThat(response.getChannel()).isEqualTo(Reservation.Channel.ONLINE);
        assertThat(response.getShowtimeId()).isEqualTo(showtimeId);
        assertThat(response.getMovieName()).isEqualTo("조건검사 테스트 영화");
        assertThat(response.getScreenName()).isEqualTo("조건검사 테스트관");
        assertThat(response.getStartTime()).isEqualTo(at(1, 13, 0));
        assertThat(response.getTotalPrice()).isEqualTo(30000);
        assertThat(response.getReservedAt()).isNotNull();
        assertThat(response.getSeats())
                .extracting(
                        ReservationSeatResponse::getSeatId,
                        ReservationSeatResponse::getSeatCode,
                        ReservationSeatResponse::getPrice)
                .containsExactlyInAnyOrder(
                        tuple(seatA1Id, "A1", 10000),
                        tuple(seatB1Id, "B1", 20000)
                );

        Reservation reservation = reservationRepository.findById(response.getId()).orElseThrow();

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
        assertThat(reservation.getChannel()).isEqualTo(Reservation.Channel.ONLINE);
        assertThat(reservation.getMember().getEmail()).isEqualTo(memberEmail);
        assertThat(reservation.getShowtime().getId()).isEqualTo(showtimeId);
        assertThat(reservation.getReservationSeats()).hasSize(2);
        assertThat(reservation.getReservationSeats())
                .extracting(rs -> rs.getSeat().getId(), ReservationSeat::getPrice)
                .containsExactlyInAnyOrder(
                        tuple(seatA1Id, 10000),
                        tuple(seatB1Id, 20000)
                );
    }

    @Test
    @DisplayName("좌석 목록이 비어 있으면 거부된다 (400)")
    void emptySeatList_rejected() {

        assertThatThrownBy(() -> reservationService.createOnlineReservation(
                memberEmail, showtimeId, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예매할 좌석을 선택해주세요.");
    }

    @Test
    @DisplayName("한 요청 안에서 같은 좌석을 중복 선택하면 거부된다 (400)")
    void duplicateSeatInRequest_rejected() {

        assertThatThrownBy(() -> reservationService.createOnlineReservation(
                memberEmail, showtimeId, List.of(seatA1Id, seatA1Id)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("같은 좌석이 중복 선택되었습니다.");
    }

    @Test
    @DisplayName("최대 좌석 수를 초과해 선택하면 거부된다 (400)")
    void overMaxSeatCount_rejected() {

        List<Long> seatIds = new ArrayList<>(List.of(seatA1Id, seatB1Id));
        char row = 'C';
        for (int i = seatIds.size(); i < maxReservationSeatCount + 1; i++) {

            seatIds.add(seatService.createSeat(screenId, gradeAId, String.valueOf(row), 1, null).getId());
            row++;
        }

        assertThatThrownBy(() -> reservationService.createOnlineReservation(
                memberEmail, showtimeId, seatIds))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        String.format("예매 1회당 선택 가능한 좌석 수는 최대 %d석입니다.", maxReservationSeatCount));
    }

    @Test
    @DisplayName("탈퇴한 회원은 예약할 수 없다 (400)")
    void withdrawnMember_rejected() {

        String withdrawnEmail = "withdrawn@test.com";
        Long withdrawnMemberId = memberService.createMember(withdrawnEmail, "password1234", "탈퇴회원").getId();
        memberService.updateMember(withdrawnMemberId, "탈퇴회원", "CUSTOMER", true);

        assertThatThrownBy(() -> reservationService.createOnlineReservation(
                withdrawnEmail, showtimeId, List.of(seatA1Id)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("탈퇴한 회원정보입니다.");
    }

    @Test
    @DisplayName("비활성 상영 일정은 예약할 수 없다 (409)")
    void inactiveShowtime_rejected() {

        // 활성화하지 않아 기본값(DEACTIVATED) 유지, 기존 일정과 겹치지 않는 다른 날짜 사용(D-30)
        Long inactiveShowtimeId = showtimeService.createShowtime(movieId, screenId, at(2, 13, 0)).getId();

        assertThatThrownBy(() -> reservationService.createOnlineReservation(
                memberEmail, inactiveShowtimeId, List.of(seatA1Id)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("예매할 수 없는 상영정보입니다.");
    }

    @Test
    @DisplayName("이미 시작된 상영 일정은 예약할 수 없다 (409)")
    void startedShowtime_rejected() {

        // 겹치지 않는 다른 날짜로 생성 후 활성화(D-30)
        Long startedShowtimeId = showtimeService.createShowtime(movieId, screenId, at(3, 13, 0)).getId();
        showtimeService.updateShowtimeStatus(startedShowtimeId, "ACTIVE");

        /*
         * Showtime.updateSchedule()이 과거 시각을 거부해 서비스 메서드로는 "시작된 일정"을 만들 수 없다(D-37 규칙 2의 예외 허용).
         * 그래서 JPQL 벌크 업데이트로 시작·종료 시각만 직접 과거로 변경한다.
         * 벌크 업데이트는 영속성 컨텍스트(1차 캐시)를 거치지 않고 DB에만 반영되므로,
         * entityManager.clear()로 컨텍스트를 비워 이후 ReservationService의 조회(findByIdForUpdate)가
         * 캐시된 값이 아니라 변경된 DB 값을 다시 읽도록 한다.
         */
        LocalDateTime pastStart = LocalDateTime.now().minusHours(3);
        LocalDateTime pastEnd = LocalDateTime.now().minusHours(1);
        entityManager.createQuery(
                        "update Showtime s set s.startTime = :start, s.endTime = :end where s.id = :id")
                .setParameter("start", pastStart)
                .setParameter("end", pastEnd)
                .setParameter("id", startedShowtimeId)
                .executeUpdate();
        entityManager.clear();

        assertThatThrownBy(() -> reservationService.createOnlineReservation(
                memberEmail, startedShowtimeId, List.of(seatA1Id)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("상영정보의 예매 가능 시간이 종료되었습니다.");
    }

    @Test
    @DisplayName("상영 일정의 상영관에 속하지 않은 좌석은 예약할 수 없다 (400)")
    void seatNotBelongToScreen_rejected() {

        Long otherScreenId = createActiveScreen("조건검사 다른 상영관");
        Long otherScreenSeatId = seatService.createSeat(otherScreenId, gradeAId, "A", 1, null).getId();

        assertThatThrownBy(() -> reservationService.createOnlineReservation(
                memberEmail, showtimeId, List.of(otherScreenSeatId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상영정보와 좌석의 상영관이 일치하지 않습니다.");
    }

    @Test
    @DisplayName("비활성 좌석은 예약할 수 없다 (409)")
    void inactiveSeat_rejected() {

        Long inactiveSeatId = seatService.createSeat(screenId, gradeAId, "C", 1, null).getId();
        seatService.updateSeat(screenId, inactiveSeatId, "C", 1, gradeAId, "DEACTIVATED");

        assertThatThrownBy(() -> reservationService.createOnlineReservation(
                memberEmail, showtimeId, List.of(inactiveSeatId)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("예매할 수 없는 좌석입니다.");
    }

    @Test
    @DisplayName("다른 회원이 이미 예약한 좌석은 예약할 수 없다 (409)")
    void alreadyReservedSeat_rejected() {

        String otherEmail = "other@test.com";
        memberService.createMember(otherEmail, "password1234", "다른회원");
        reservationService.createOnlineReservation(otherEmail, showtimeId, List.of(seatA1Id));

        assertThatThrownBy(() -> reservationService.createOnlineReservation(
                memberEmail, showtimeId, List.of(seatA1Id)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 예약된 좌석입니다.");
    }

    // 테스트용 상영관 생성: 생성 시 CLOSED이므로 ACTIVE로 변경
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
