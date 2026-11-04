/***
 * ShowtimeService의 상영시간표 조회(findTimetable())와 상영관별 목록 조회(findByScreenId())의
 * SQL 실행 수를 측정하는 테스트를 담당하는 클래스
 * 작성이력
 * -2026-11-04: 파일 생성 및 작성 (MovieSearchQueryCountTest.java 구조 참고)
 *
 * findTimetable()은 처음부터 영화 · 상영관을 join + fetchJoin으로 조회하도록 구현되어 SQL 1건이다.
 * findByScreenId()는 개선 전 상영관별 목록의 영화마다 지연 로딩이 발생해(N+1) 상영관 조회 1건 +
 * 상영 일정마다 영화 조회 N건이 실행됐다. ShowtimeRepository.findByScreenOrderByStartTimeAsc()를
 * 영화 join fetch로 리워크한 뒤에는 상영관 조회 1건 + 상영 일정 목록(영화 포함) 조회 1건,
 * 총 2건으로 줄었다. 이 테스트는 그 결과를 고정해 N+1이 다시 생기지 않도록 막는다.
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.movie.MovieService;
import com.cinema.backend.domain.screen.ScreenService;
import com.cinema.backend.domain.showtime.dto.ShowtimeResponse;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 이 테스트 클래스에서만 Hibernate 통계를 켠다. application.properties는 건드리지 않는다.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
@Transactional
class ShowtimeQueryCountTest {

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private ScreenService screenService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("상영시간표: 영화 3편 · 상영관 2개에 걸친 일정 6개를 조회하면 실행되는 SQL은 1건이다")
    void findTimetable_sixShowtimesAcrossThreeMoviesTwoScreens_executesOneQuery() {

        Long movie1Id = createActiveMovie("SQC1 영화1");
        Long movie2Id = createActiveMovie("SQC1 영화2");
        Long movie3Id = createActiveMovie("SQC1 영화3");
        Long screen1Id = createActiveScreen("SQC1 1관");
        Long screen2Id = createActiveScreen("SQC1 2관");

        // 같은 상영관에서 겹치지 않도록 4시간 간격으로 배치 (러닝타임 120분 + 여유 15분보다 충분히 넓음)
        createActiveShowtime(movie1Id, screen1Id, at(8, 0));
        createActiveShowtime(movie2Id, screen1Id, at(12, 0));
        createActiveShowtime(movie3Id, screen1Id, at(16, 0));
        createActiveShowtime(movie1Id, screen2Id, at(9, 0));
        createActiveShowtime(movie2Id, screen2Id, at(13, 0));
        createActiveShowtime(movie3Id, screen2Id, at(17, 0));

        // 영속성 컨텍스트에 남아있는 영화 · 상영관 · 상영 일정 엔티티 때문에 지연 로딩 쿼리가
        // 생략되는 것을 막기 위해, 측정 전 flush() + clear()로 영속성 컨텍스트를 비운다.
        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        List<ShowtimeResponse> result = showtimeService.findTimetable(DATE, null, null);

        assertThat(result).hasSize(6);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("직원용 상영관별 목록: 영화 3편의 일정을 조회하면 실행되는 SQL은 2건이다 (상영관 조회 1 + 일정 조회 1)")
    void findByScreenId_threeMoviesOnOneScreen_executesTwoQueries() {

        Long movie1Id = createActiveMovie("SQC2 영화1");
        Long movie2Id = createActiveMovie("SQC2 영화2");
        Long movie3Id = createActiveMovie("SQC2 영화3");
        Long screenId = createActiveScreen("SQC2 1관");

        createActiveShowtime(movie1Id, screenId, at(10, 0));
        createActiveShowtime(movie2Id, screenId, at(14, 0));
        createActiveShowtime(movie3Id, screenId, at(18, 0));

        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        List<ShowtimeResponse> result = showtimeService.findByScreenId(screenId);

        assertThat(result).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    // 2099년 기준 날짜 (엔티티가 과거 시각을 거부하므로 먼 미래 날짜 사용)
    private static final LocalDate DATE = LocalDate.of(2099, 1, 1);

    private LocalDateTime at(int hour, int minute) {

        return LocalDateTime.of(2099, 1, 1, hour, minute);
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
}
