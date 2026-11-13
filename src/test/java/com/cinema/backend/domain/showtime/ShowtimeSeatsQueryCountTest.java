/***
 * ShowtimeService.findShowtimeSeats()(고객용 좌석 조회, D-55)의 SQL 실행 수를 측정하는 테스트를 담당하는 클래스
 * 작성이력
 * -2026-11-13: 파일 생성 및 작성 [AI 초안] (ShowtimeQueryCountTest.java 구조 참고)
 *
 * D-55 설계대로 findShowtimeSeats()는 좌석 수와 무관하게 SQL 3건(상영 일정 + 영화 · 상영관 조회,
 * 좌석 + 등급 조회, 예약된 좌석 id 조회)만 실행되어야 한다. 이 테스트는 그 결과를 고정해
 * 이후 변경으로 N+1이 생기지 않도록 막는다.
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
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 이 테스트 클래스에서만 Hibernate 통계를 켠다. application.properties는 건드리지 않는다.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
@Transactional
class ShowtimeSeatsQueryCountTest {

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
    @DisplayName("좌석 2행 · 등급 2종, 활성 예약 · 취소된 예약이 섞인 상영 일정을 조회하면 실행되는 SQL은 3건이다")
    void findShowtimeSeats_mixedSeatsAndReservations_executesThreeQueries() {

        Long movieId = createActiveMovie("SSQ1 영화");
        Long screenId = createActiveScreen("SSQ1 1관");
        Long gradeAId = seatGradeService.createSeatGrade("SSQ1-일반", 10000, null).getId();
        Long gradeBId = seatGradeService.createSeatGrade("SSQ1-VIP", 20000, null).getId();

        Long seatA1Id = createSeat(screenId, gradeAId, "A", 1);
        Long seatA2Id = createSeat(screenId, gradeBId, "A", 2);
        createSeat(screenId, gradeAId, "B", 1);
        createSeat(screenId, gradeBId, "B", 2);

        Long showtimeId = createActiveShowtime(movieId, screenId, at(1, 10, 0));

        memberService.createMember("ssq1-a@test.com", "password1234", "SSQ1회원A");
        memberService.createMember("ssq1-b@test.com", "password1234", "SSQ1회원B");

        // 활성 예약 좌석(A1)과 취소된 예약 좌석(A2)을 함께 둔다.
        reservationService.createOnlineReservation("ssq1-a@test.com", showtimeId, List.of(seatA1Id));
        Long canceledReservationId = reservationService.createOnlineReservation(
                "ssq1-b@test.com", showtimeId, List.of(seatA2Id)).getId();
        reservationService.cancelMyReservation("ssq1-b@test.com", canceledReservationId);

        // 영속성 컨텍스트에 남아있는 엔티티 때문에 지연 로딩 쿼리가 생략되는 것을 막기 위해,
        // 측정 전 flush() + clear()로 영속성 컨텍스트를 비운다.
        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        ShowtimeSeatsResponse response = showtimeService.findShowtimeSeats(showtimeId);

        assertThat(response.getSeats()).hasSize(4);

        // 응답의 등급 이름까지 읽어, 지연 로딩이 추가로 일어나지 않는지 함께 확인한다.
        List<String> seatGradeNames = response.getSeats().stream()
                .map(SeatAvailabilityResponse::getSeatGradeName)
                .toList();
        assertThat(seatGradeNames).containsExactly(
                "SSQ1-일반", "SSQ1-VIP", "SSQ1-일반", "SSQ1-VIP");

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    private LocalDateTime at(int day, int hour, int minute) {

        return LocalDateTime.of(2099, 1, day, hour, minute);
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

    private Long createSeat(Long screenId, Long gradeId, String row, int column) {

        return seatService.createSeat(screenId, gradeId, row, column, null).getId();
    }
}
