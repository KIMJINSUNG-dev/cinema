/***
 * ReservationService의 예매 내역 목록 조회(고객용 findMyReservations(), 직원용
 * findReservationsByMemberId())의 SQL 실행 수를 측정하는 테스트를 담당하는 클래스
 * 작성이력
 * -2026-11-05: 파일 생성 및 작성 (MovieSearchQueryCountTest.java · ShowtimeQueryCountTest.java 구조 참고)
 * -2026-11-05: 클래스 상단 주석을 개선 완료 기준으로 정리 (운영 코드 개선은 개발자가 [직접 구현]으로 완료)
 *
 * 개선 전에는 findByMemberId()가 fetch join · batch fetch 없이 조회해 예약 10건 기준 30건이
 * 실행됐다 (회원 조회 1 + 예약 목록 1 + 예약마다 상영 일정 지연 로딩 5 + 영화 지연 로딩 3 +
 * 상영관 지연 로딩 2 + 예약 좌석 목록 지연 로딩 10 + 좌석 지연 로딩 8).
 * ① findByMemberId()에 @EntityGraph로 상영 일정 · 영화 · 상영관을 함께 조회하도록 개선해
 * 상영 일정 · 영화 · 상영관 지연 로딩(5 + 3 + 2 = 10건)이 없어져 20건으로 줄었다.
 * ② hibernate.default_batch_fetch_size=100으로 예약 좌석 · 좌석을 IN 조건으로 묶어 일괄
 * 로딩하도록 개선해 예약 좌석 · 좌석 지연 로딩(10 + 8 = 18건)이 각 1건으로 줄어 최종 4건이
 * 됐다. 이 테스트는 그 개선 결과를 고정해 N+1이 다시 생기지 않도록 막는다.
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
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 이 테스트 클래스에서만 Hibernate 통계를 켠다. application.properties는 건드리지 않는다.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
@Transactional
class ReservationQueryCountTest {

    @Autowired private ReservationService reservationService;
    @Autowired private MovieService movieService;
    @Autowired private ScreenService screenService;
    @Autowired private SeatGradeService seatGradeService;
    @Autowired private SeatService seatService;
    @Autowired private ShowtimeService showtimeService;
    @Autowired private MemberService memberService;
    @Autowired private EntityManager entityManager;

    private String ownerEmail;
    private Long ownerMemberId;

    /*
     * 한 회원의 예약 10건: 상영 일정 5개(영화 3편 · 상영관 2개에 분산), 예약마다 좌석 2석.
     * 상영관별 좌석 4석을 만들어 그 상영관에서 열리는 모든 상영 일정이 재사용하고(상영 일정이
     * 다르면 같은 좌석이라도 중복 예약 검사에 걸리지 않음), 상영 일정마다 좌석 2석씩 2건의
     * 예약을 만들어 5 * 2 = 10건을 채운다.
     */
    @BeforeEach
    void setUp() {

        Long movie1Id = createActiveMovie("쿼리수 영화1");
        Long movie2Id = createActiveMovie("쿼리수 영화2");
        Long movie3Id = createActiveMovie("쿼리수 영화3");
        Long screen1Id = createActiveScreen("쿼리수 1관");
        Long screen2Id = createActiveScreen("쿼리수 2관");

        Long gradeId = seatGradeService.createSeatGrade("쿼리수-일반", 10000, null).getId();

        List<Long> screen1SeatIds = List.of(
                createSeat(screen1Id, gradeId, "A", 1),
                createSeat(screen1Id, gradeId, "A", 2),
                createSeat(screen1Id, gradeId, "A", 3),
                createSeat(screen1Id, gradeId, "A", 4)
        );
        List<Long> screen2SeatIds = List.of(
                createSeat(screen2Id, gradeId, "B", 1),
                createSeat(screen2Id, gradeId, "B", 2),
                createSeat(screen2Id, gradeId, "B", 3),
                createSeat(screen2Id, gradeId, "B", 4)
        );

        // 같은 상영관에서 겹치지 않도록 4시간 간격으로 배치 (러닝타임 120분 + 여유 15분보다 충분히 넓음)
        Long showtime1Id = createActiveShowtime(movie1Id, screen1Id, at(10, 0));
        Long showtime2Id = createActiveShowtime(movie2Id, screen1Id, at(14, 0));
        Long showtime3Id = createActiveShowtime(movie3Id, screen2Id, at(10, 0));
        Long showtime4Id = createActiveShowtime(movie1Id, screen2Id, at(14, 0));
        Long showtime5Id = createActiveShowtime(movie2Id, screen2Id, at(18, 0));

        ownerEmail = "qc-owner@test.com";
        ownerMemberId = memberService.createMember(ownerEmail, "password1234", "쿼리수본인").getId();

        createReservationsForShowtime(showtime1Id, screen1SeatIds);
        createReservationsForShowtime(showtime2Id, screen1SeatIds);
        createReservationsForShowtime(showtime3Id, screen2SeatIds);
        createReservationsForShowtime(showtime4Id, screen2SeatIds);
        createReservationsForShowtime(showtime5Id, screen2SeatIds);
    }

    @Test
    @DisplayName("고객 목록: 예약 10건을 조회하면 실행되는 SQL은 4건이다 (회원 조회 1 + 예약 목록 1 + 예약 좌석 일괄 1 + 좌석 일괄 1)")
    void findMyReservations_tenReservations_executesFourQueries() {

        // 영속성 컨텍스트에 남아있는 엔티티 때문에 지연 로딩 쿼리가 생략되는 것을 막기 위해,
        // 측정 전 flush() + clear()로 영속성 컨텍스트를 비운다.
        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        // 결과(10건)가 페이지 크기(20)보다 적어 PageableExecutionUtils가 count 쿼리를 생략한다.
        Page<ReservationResponse> page = reservationService.findMyReservations(
                ownerEmail, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getContent())
                .allSatisfy(reservation -> {
                    assertThat(reservation.getSeats()).hasSize(2);
                    assertThat(reservation.getMovieName()).isNotNull();
                    assertThat(reservation.getScreenName()).isNotNull();
                });

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("직원 목록: 같은 데이터를 memberId로 조회하면 실행되는 SQL은 4건이다 (회원 존재 확인 1 + 예약 목록 1 + 예약 좌석 일괄 1 + 좌석 일괄 1)")
    void findReservationsByMemberId_tenReservations_executesFourQueries() {

        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        Page<ReservationResponse> page = reservationService.findReservationsByMemberId(
                ownerMemberId, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getContent())
                .allSatisfy(reservation -> {
                    assertThat(reservation.getSeats()).hasSize(2);
                    assertThat(reservation.getMovieName()).isNotNull();
                    assertThat(reservation.getScreenName()).isNotNull();
                });

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
    }

    // 한 상영 일정에 좌석 2석씩 예약 2건 생성 (좌석 목록의 앞 2개 · 뒤 2개로 분리해 같은 상영 일정 안에서 좌석이 겹치지 않게 함)
    private void createReservationsForShowtime(Long showtimeId, List<Long> seatIds) {

        reservationService.createOnlineReservation(ownerEmail, showtimeId, List.of(seatIds.get(0), seatIds.get(1)));
        reservationService.createOnlineReservation(ownerEmail, showtimeId, List.of(seatIds.get(2), seatIds.get(3)));
    }

    // 2099년 기준 시각 (엔티티가 과거 시각을 거부하므로 먼 미래 날짜 사용, ShowtimeQueryCountTest와 같은 방식)
    private LocalDateTime at(int hour, int minute) {

        return LocalDateTime.of(2099, 1, 1, hour, minute);
    }

    // 영화: 공개(ACTIVE) · 상영 중 (ShowtimeQueryCountTest와 같은 방식)
    private Long createActiveMovie(String name) {

        Long id = movieService.createMovie(name, null, null, null, 120).getId();
        movieService.updateMovie(id, name, null, "SHOWING", "ACTIVE", null, null, 120);
        return id;
    }

    // 테스트용 상영관 생성: 생성 시 CLOSED이므로 ACTIVE로 변경 (ShowtimeQueryCountTest와 같은 방식)
    private Long createActiveScreen(String name) {

        Long id = screenService.createScreen(name).getId();
        screenService.updateScreen(id, name, "ACTIVE");
        return id;
    }

    private Long createSeat(Long screenId, Long gradeId, String row, int column) {

        return seatService.createSeat(screenId, gradeId, row, column, null).getId();
    }

    private Long createActiveShowtime(Long movieId, Long screenId, LocalDateTime startTime) {

        Long id = showtimeService.createShowtime(movieId, screenId, startTime).getId();
        showtimeService.updateShowtimeStatus(id, "ACTIVE");
        return id;
    }
}
