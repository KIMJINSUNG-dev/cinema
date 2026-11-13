/***
 * ShowtimeService.findShowtimeSeats()(고객용 좌석 조회, D-55) 통합 테스트를 담당하는 클래스
 * 작성이력
 * -2026-11-13: 파일 생성 및 작성 [AI 초안] (ShowtimeTimetableTest.java · ReservationConditionTest.java 구조 참고)
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.member.MemberService;
import com.cinema.backend.domain.movie.MovieService;
import com.cinema.backend.domain.reservation.ReservationService;
import com.cinema.backend.domain.screen.ScreenService;
import com.cinema.backend.domain.screen.SeatGradeService;
import com.cinema.backend.domain.screen.SeatService;
import com.cinema.backend.domain.showtime.dto.SeatAvailabilityResponse;
import com.cinema.backend.domain.showtime.dto.ShowtimeSeatsResponse;
import com.cinema.backend.global.exception.ConflictException;
import jakarta.persistence.EntityManager;
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
import static org.assertj.core.api.Assertions.tuple;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ShowtimeSeatsTest {

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private ScreenService screenService;

    @Autowired
    private SeatGradeService seatGradeService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("상태 3종: 사용 중지 좌석은 UNAVAILABLE, 활성 예약 좌석은 RESERVED, 나머지는 AVAILABLE로 응답되고 사용 중지 좌석도 목록에서 빠지지 않는다")
    void threeStatuses_unavailableSeatIncludedInResponse() {

        Long movieId = createActiveMovie("SS1 영화");
        Long screenId = createActiveScreen("SS1 1관");
        Long gradeId = createGrade("SS1 일반", 10000);

        Long unavailableSeatId = createSeat(screenId, gradeId, "A", 1);
        Long reservedSeatId = createSeat(screenId, gradeId, "A", 2);
        Long availableSeatId = createSeat(screenId, gradeId, "A", 3);

        seatService.updateSeat(screenId, unavailableSeatId, "A", 1, gradeId, "DEACTIVATED");

        Long showtimeId = createActiveShowtime(movieId, screenId, at(1, 10, 0));
        createMember("ss1@test.com", "SS1회원");
        reservationService.createOnlineReservation("ss1@test.com", showtimeId, List.of(reservedSeatId));

        ShowtimeSeatsResponse response = showtimeService.findShowtimeSeats(showtimeId);

        assertThat(response.getSeats())
                .extracting(
                        SeatAvailabilityResponse::getSeatId,
                        SeatAvailabilityResponse::getStatus)
                .containsExactlyInAnyOrder(
                        tuple(unavailableSeatId, SeatAvailabilityResponse.SeatAvailabilityStatus.UNAVAILABLE),
                        tuple(reservedSeatId, SeatAvailabilityResponse.SeatAvailabilityStatus.RESERVED),
                        tuple(availableSeatId, SeatAvailabilityResponse.SeatAvailabilityStatus.AVAILABLE)
                );
    }

    @Test
    @DisplayName("정렬: 좌석을 행 · 열 순서와 다르게 등록해도 응답은 행 → 열 순(숫자 기준)으로 정렬되어 A10이 A2보다 뒤에 온다")
    void sorting_byRowThenColumn_notLexical() {

        Long movieId = createActiveMovie("SS2 영화");
        Long screenId = createActiveScreen("SS2 1관");
        Long gradeId = createGrade("SS2 일반", 10000);

        // 등록 순서를 정렬 결과와 다르게 섞어, 응답이 등록 순서가 아니라 행 · 열 순으로 정렬됨을 확인한다.
        createSeat(screenId, gradeId, "A", 10);
        createSeat(screenId, gradeId, "B", 1);
        createSeat(screenId, gradeId, "A", 2);
        createSeat(screenId, gradeId, "A", 1);
        createSeat(screenId, gradeId, "B", 5);

        Long showtimeId = createActiveShowtime(movieId, screenId, at(2, 10, 0));

        ShowtimeSeatsResponse response = showtimeService.findShowtimeSeats(showtimeId);

        assertThat(response.getSeats())
                .extracting(SeatAvailabilityResponse::getSeatCode)
                .containsExactly("A1", "A2", "A10", "B1", "B5");
    }

    @Test
    @DisplayName("응답 값: 좌석 필드(seatId · seatCode · row · column · seatGradeName · price)와 상영 일정 요약(showtimeId · movieName · screenName · startTime · endTime)이 올바르게 채워진다")
    void responseFields_seatAndShowtimeSummary() {

        Long movieId = createActiveMovie("SS3 영화");
        Long screenId = createActiveScreen("SS3 1관");
        Long gradeAId = createGrade("SS3-일반", 10000);
        Long gradeBId = createGrade("SS3-VIP", 20000);

        Long seatAId = createSeat(screenId, gradeAId, "A", 1);
        Long seatBId = createSeat(screenId, gradeBId, "B", 2);

        LocalDateTime startTime = at(3, 10, 0);
        Long showtimeId = createActiveShowtime(movieId, screenId, startTime);

        ShowtimeSeatsResponse response = showtimeService.findShowtimeSeats(showtimeId);

        assertThat(response.getShowtimeId()).isEqualTo(showtimeId);
        assertThat(response.getMovieName()).isEqualTo("SS3 영화");
        assertThat(response.getScreenName()).isEqualTo("SS3 1관");
        assertThat(response.getStartTime()).isEqualTo(startTime);
        assertThat(response.getEndTime()).isEqualTo(startTime.plusMinutes(120));

        assertThat(response.getSeats())
                .extracting(
                        SeatAvailabilityResponse::getSeatId,
                        SeatAvailabilityResponse::getSeatCode,
                        SeatAvailabilityResponse::getRow,
                        SeatAvailabilityResponse::getColumn,
                        SeatAvailabilityResponse::getSeatGradeName,
                        SeatAvailabilityResponse::getPrice)
                .containsExactly(
                        tuple(seatAId, "A1", 1, 1, "SS3-일반", 10000),
                        tuple(seatBId, "B2", 2, 2, "SS3-VIP", 20000)
                );
    }

    @Test
    @DisplayName("취소한 예약의 좌석은 AVAILABLE로 응답된다")
    void canceledReservationSeat_available() {

        Long movieId = createActiveMovie("SS4 영화");
        Long screenId = createActiveScreen("SS4 1관");
        Long gradeId = createGrade("SS4 일반", 10000);
        Long seatId = createSeat(screenId, gradeId, "A", 1);

        Long showtimeId = createActiveShowtime(movieId, screenId, at(4, 10, 0));
        createMember("ss4@test.com", "SS4회원");
        Long reservationId = reservationService.createOnlineReservation(
                "ss4@test.com", showtimeId, List.of(seatId)).getId();
        reservationService.cancelMyReservation("ss4@test.com", reservationId);

        ShowtimeSeatsResponse response = showtimeService.findShowtimeSeats(showtimeId);

        assertThat(response.getSeats())
                .extracting(SeatAvailabilityResponse::getSeatId, SeatAvailabilityResponse::getStatus)
                .containsExactly(tuple(seatId, SeatAvailabilityResponse.SeatAvailabilityStatus.AVAILABLE));
    }

    @Test
    @DisplayName("사용 중지 좌석이면서 활성 예약도 있는 좌석은 UNAVAILABLE로 응답된다 (예약 후 좌석을 비활성화)")
    void deactivatedSeatWithActiveReservation_unavailable() {

        Long movieId = createActiveMovie("SS5 영화");
        Long screenId = createActiveScreen("SS5 1관");
        Long gradeId = createGrade("SS5 일반", 10000);
        Long seatId = createSeat(screenId, gradeId, "A", 1);

        Long showtimeId = createActiveShowtime(movieId, screenId, at(5, 10, 0));
        createMember("ss5@test.com", "SS5회원");
        reservationService.createOnlineReservation("ss5@test.com", showtimeId, List.of(seatId));

        // 예약 후 좌석을 비활성화해, 활성 예약과 사용 중지가 함께 있는 상황을 만든다.
        seatService.updateSeat(screenId, seatId, "A", 1, gradeId, "DEACTIVATED");

        ShowtimeSeatsResponse response = showtimeService.findShowtimeSeats(showtimeId);

        assertThat(response.getSeats())
                .extracting(SeatAvailabilityResponse::getSeatId, SeatAvailabilityResponse::getStatus)
                .containsExactly(tuple(seatId, SeatAvailabilityResponse.SeatAvailabilityStatus.UNAVAILABLE));
    }

    @Test
    @DisplayName("등급이 비활성인 좌석은 AVAILABLE로 응답된다 (D-27 · D-42와 일관)")
    void deactivatedGradeSeat_available() {

        Long movieId = createActiveMovie("SS6 영화");
        Long screenId = createActiveScreen("SS6 1관");
        Long gradeId = createGrade("SS6 일반", 10000);
        Long seatId = createSeat(screenId, gradeId, "A", 1);

        seatGradeService.updateSeatGrade(gradeId, "SS6 일반", 10000, "DEACTIVATED");

        Long showtimeId = createActiveShowtime(movieId, screenId, at(6, 10, 0));

        ShowtimeSeatsResponse response = showtimeService.findShowtimeSeats(showtimeId);

        assertThat(response.getSeats())
                .extracting(SeatAvailabilityResponse::getSeatId, SeatAvailabilityResponse::getStatus)
                .containsExactly(tuple(seatId, SeatAvailabilityResponse.SeatAvailabilityStatus.AVAILABLE));
    }

    @Test
    @DisplayName("같은 상영관의 다른 상영 일정에 있는 예약은 이 일정의 좌석 상태에 영향을 주지 않는다")
    void otherShowtimeReservation_noEffect() {

        Long movieId = createActiveMovie("SS7 영화");
        Long screenId = createActiveScreen("SS7 1관");
        Long gradeId = createGrade("SS7 일반", 10000);
        Long seatId = createSeat(screenId, gradeId, "A", 1);

        Long showtime1Id = createActiveShowtime(movieId, screenId, at(7, 8, 0));
        Long showtime2Id = createActiveShowtime(movieId, screenId, at(7, 20, 0));

        createMember("ss7@test.com", "SS7회원");
        reservationService.createOnlineReservation("ss7@test.com", showtime1Id, List.of(seatId));

        ShowtimeSeatsResponse response = showtimeService.findShowtimeSeats(showtime2Id);

        assertThat(response.getSeats())
                .extracting(SeatAvailabilityResponse::getSeatId, SeatAvailabilityResponse::getStatus)
                .containsExactly(tuple(seatId, SeatAvailabilityResponse.SeatAvailabilityStatus.AVAILABLE));
    }

    @Test
    @DisplayName("다른 상영관의 좌석은 응답에 포함되지 않는다")
    void otherScreenSeat_excluded() {

        Long movieId = createActiveMovie("SS8 영화");
        Long screen1Id = createActiveScreen("SS8 1관");
        Long screen2Id = createActiveScreen("SS8 2관");
        Long gradeId = createGrade("SS8 일반", 10000);

        Long seat1Id = createSeat(screen1Id, gradeId, "A", 1);
        createSeat(screen2Id, gradeId, "A", 1);

        Long showtimeId = createActiveShowtime(movieId, screen1Id, at(8, 10, 0));

        ShowtimeSeatsResponse response = showtimeService.findShowtimeSeats(showtimeId);

        assertThat(response.getSeats())
                .extracting(SeatAvailabilityResponse::getSeatId)
                .containsExactly(seat1Id);
    }

    @Test
    @DisplayName("예약이 하나도 없는 일정은 활성 좌석이 전부 AVAILABLE로 응답된다")
    void noReservation_allAvailable() {

        Long movieId = createActiveMovie("SS9 영화");
        Long screenId = createActiveScreen("SS9 1관");
        Long gradeId = createGrade("SS9 일반", 10000);

        createSeat(screenId, gradeId, "A", 1);
        createSeat(screenId, gradeId, "A", 2);

        Long showtimeId = createActiveShowtime(movieId, screenId, at(9, 10, 0));

        ShowtimeSeatsResponse response = showtimeService.findShowtimeSeats(showtimeId);

        assertThat(response.getSeats())
                .extracting(SeatAvailabilityResponse::getStatus)
                .containsOnly(SeatAvailabilityResponse.SeatAvailabilityStatus.AVAILABLE);
    }

    @Test
    @DisplayName("비활성 상영 일정은 조회할 수 없다 (409)")
    void inactiveShowtime_rejected() {

        Long movieId = createActiveMovie("SS10 영화");
        Long screenId = createActiveScreen("SS10 1관");

        // 활성화하지 않아 기본값(DEACTIVATED)을 유지
        Long showtimeId = showtimeService.createShowtime(movieId, screenId, at(10, 10, 0)).getId();

        assertThatThrownBy(() -> showtimeService.findShowtimeSeats(showtimeId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("예매할 수 없는 상영정보입니다.");
    }

    @Test
    @DisplayName("비공개 영화의 활성 일정은 조회할 수 없다 (409)")
    void privateMovieActiveShowtime_rejected() {

        Long movieId = createActiveMovie("SS11 영화");
        Long screenId = createActiveScreen("SS11 1관");

        Long showtimeId = createActiveShowtime(movieId, screenId, at(11, 10, 0));
        // 일정을 활성화한 뒤 영화를 비공개로 전환 (일정 상태는 그대로 ACTIVE, ShowtimeTimetableTest와 같은 방식)
        movieService.updateMovie(movieId, "SS11 영화", null, "SHOWING", "DEACTIVATED", null, null, 120);

        assertThatThrownBy(() -> showtimeService.findShowtimeSeats(showtimeId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("예매할 수 없는 상영정보입니다.");
    }

    @Test
    @DisplayName("이미 시작된 상영 일정은 조회할 수 없다 (409)")
    void startedShowtime_rejected() {

        Long movieId = createActiveMovie("SS12 영화");
        Long screenId = createActiveScreen("SS12 1관");
        Long showtimeId = createActiveShowtime(movieId, screenId, at(12, 10, 0));

        /*
         * Showtime.updateSchedule()이 과거 시각을 거부해 서비스 메서드로는 "시작된 일정"을 만들 수 없다(D-37 규칙 2의 예외 허용).
         * ReservationConditionTest.startedShowtime_rejected()와 같은 방식으로 JPQL 벌크 업데이트로
         * 시작 · 종료 시각만 직접 과거로 변경한 뒤, entityManager.clear()로 영속성 컨텍스트를 비워
         * 이후 조회가 캐시된 값이 아니라 변경된 DB 값을 다시 읽도록 한다.
         */
        LocalDateTime pastStart = LocalDateTime.now().minusHours(3);
        LocalDateTime pastEnd = LocalDateTime.now().minusHours(1);
        entityManager.createQuery(
                        "update Showtime s set s.startTime = :start, s.endTime = :end where s.id = :id")
                .setParameter("start", pastStart)
                .setParameter("end", pastEnd)
                .setParameter("id", showtimeId)
                .executeUpdate();
        entityManager.clear();

        assertThatThrownBy(() -> showtimeService.findShowtimeSeats(showtimeId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("상영정보의 예매 가능 시간이 종료되었습니다.");
    }

    @Test
    @DisplayName("상영정보 id가 null이면 IllegalArgumentException이 발생한다 (400)")
    void nullShowtimeId_rejected() {

        assertThatThrownBy(() -> showtimeService.findShowtimeSeats(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 상영정보입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 상영정보 id로 조회하면 IllegalArgumentException이 발생한다 (400)")
    void nonExistentShowtimeId_rejected() {

        assertThatThrownBy(() -> showtimeService.findShowtimeSeats(999_999_999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 상영정보입니다.");
    }

    // 영화: 공개(ACTIVE) · 상영 중 (ShowtimeChangeRestrictionTest와 같은 준비 방식)
    private Long createActiveMovie(String name) {

        Long id = movieService.createMovie(name, null, null, null, 120).getId();
        movieService.updateMovie(id, name, null, "SHOWING", "ACTIVE", null, null, 120);
        return id;
    }

    // 테스트용 상영관 생성: 생성 시 CLOSED이므로 ACTIVE로 변경 (ShowtimeOverlapTest와 같은 방식)
    private Long createActiveScreen(String name) {

        Long id = screenService.createScreen(name).getId();
        screenService.updateScreen(id, name, "ACTIVE");
        return id;
    }

    private Long createActiveShowtime(Long movieId, Long screenId, LocalDateTime startTime) {

        Long id = showtimeService.createShowtime(movieId, screenId, startTime).getId();
        showtimeService.updateShowtimeStatus(id, "ACTIVE");
        return id;
    }

    private Long createGrade(String name, int price) {

        return seatGradeService.createSeatGrade(name, price, null).getId();
    }

    private Long createSeat(Long screenId, Long gradeId, String row, int column) {

        return seatService.createSeat(screenId, gradeId, row, column, null).getId();
    }

    private void createMember(String email, String nickname) {

        memberService.createMember(email, "password1234", nickname);
    }

    // 2099년 기준 시각 (엔티티가 과거 시각을 거부하므로 먼 미래 날짜 사용, day로 일정 간 겹침을 피함)
    private LocalDateTime at(int day, int hour, int minute) {

        return LocalDateTime.of(2099, 1, day, hour, minute);
    }
}
