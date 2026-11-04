/***
 * MovieService.searchMovies()의 N+1 쿼리 수를 측정하는 테스트를 담당하는 클래스
 *
 * 개선 전에는 영화 목록 조회 1건 뒤에 영화마다 movieGenreRepository.findByMovieId()를
 * 호출해 movie_genres를 조회하고, 그 결과로 얻은 Genre를 지연 로딩해 14건(목록 1 +
 * movie_genres 10 + genres 3)이 실행됐다. 영화 id IN 일괄 조회 + 장르 fetch join
 * (movieGenreRepository.findAllWithGenreByMovieIdIn())으로 리워크한 뒤에는 2건(목록 1 +
 * 장르 일괄 조회 1)으로 줄었다. 이 테스트는 그 개선 결과를 고정해 N+1이 다시 생기지 않도록 막는다.
 * 작성이력
 * -2026-11-04: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.movie;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.movie.dto.MovieResponse;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 이 테스트 클래스에서만 Hibernate 통계를 켠다. application.properties는 건드리지 않는다.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
@Transactional
class MovieSearchQueryCountTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("ACTIVE 영화 10건(장르 2개씩 연결)을 검색하면 실행되는 SQL은 2건이다 (목록 1 + 장르 일괄 조회 1)")
    void searchMovies_tenMoviesWithTwoGenresEach_executesTwoQueries() {

        Long genreAId = createGenre("NPQ1 장르A");
        Long genreBId = createGenre("NPQ1 장르B");
        Long genreCId = createGenre("NPQ1 장르C");
        List<Long> genrePool = List.of(genreAId, genreBId, genreCId);

        for (int i = 0; i < 10; i++) {

            // 장르 풀 3개를 돌아가며 영화마다 2개씩 연결한다. (0,1) (1,2) (2,0) ...
            Long firstGenreId = genrePool.get(i % 3);
            Long secondGenreId = genrePool.get((i + 1) % 3);
            createActiveMovie("NPQ1 Movie" + i, List.of(firstGenreId, secondGenreId), Movie.MovieStatus.UPCOMING);
        }

        // 영속성 컨텍스트에 남아있는 영화 · 장르 엔티티 때문에 지연 로딩 쿼리가 생략되는 것을
        // 막기 위해, 측정 전 flush() + clear()로 영속성 컨텍스트를 비운다.
        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        Page<MovieResponse> page = movieService.searchMovies(
                "NPQ1 Movie", null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getContent())
                .allSatisfy(movie -> assertThat(movie.getGenreNames()).hasSize(2));

        // 결과(10건)가 페이지 크기(20)보다 적어 PageableExecutionUtils가 count 쿼리를 생략한다.
        // 따라서 개선 후 기대 SQL 수는 목록 조회 1건 + 장르 일괄(IN) 조회 1건 = 2건이다.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    // MovieSearchTest.createActiveMovie()와 같은 방식.
    // Movie의 displayStatus 기본값은 DEACTIVATED이므로, 생성 후 updateMovie()로 ACTIVE로 전환한다.
    private Long createActiveMovie(String name, List<Long> genreIds, Movie.MovieStatus movieStatus) {

        Long id = movieService.createMovie(name, null, null, null, 120).getId();
        movieService.updateMovie(id, name, null, movieStatus.name(), "ACTIVE", null, genreIds, 120);
        return id;
    }

    // MovieSearchTest.createGenre()와 같은 방식.
    // Genre에는 아직 서비스 계층이 없어(GenreRepository만 존재) 리포지토리로 직접 생성한다.
    private Long createGenre(String name) {

        Genre genre = new Genre();
        genre.updateName(name);
        return genreRepository.save(genre).getId();
    }
}
