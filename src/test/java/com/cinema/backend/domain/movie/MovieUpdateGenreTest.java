/***
 * MovieService.updateMovie()의 장르 재연결 회귀 테스트를 담당하는 클래스
 *
 * 과거에 있었던 버그(원인: IDENTITY 즉시 INSERT와 지연 DELETE의 충돌로, 기존 MovieGenre를
 * deleteAll()한 뒤 겹치는 genreId를 다시 save()하면 아직 지워지지 않은 기존 행과 충돌해
 * movie_genres 유니크 제약(movie_id, genre_id)을 위반함)과, 그 재발을 막기 위한 회귀 테스트다.
 * 작성이력
 * -2026-11-03: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.movie;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.movie.dto.MovieResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class MovieUpdateGenreTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieGenreRepository movieGenreRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Test
    @DisplayName("같은 장르 목록으로 수정하면 예외 없이 성공하고 장르가 유지된다 ([A, B] → [A, B])")
    void sameGenreList_succeeds_andKeepsGenres() {

        Long genreAId = createGenre("MUG1 장르A");
        Long genreBId = createGenre("MUG1 장르B");

        Long movieId = movieService.createMovie(
                "MUG1 영화", null, null, List.of(genreAId, genreBId), 120).getId();

        MovieResponse response = movieService.updateMovie(
                movieId, "MUG1 영화", null, "UPCOMING", "DEACTIVATED", null,
                List.of(genreAId, genreBId), 120);

        assertThat(response.getGenreNames()).containsExactlyInAnyOrder("MUG1 장르A", "MUG1 장르B");
        assertThat(movieGenreRepository.findByMovieId(movieId))
                .extracting(movieGenre -> movieGenre.getGenre().getId())
                .containsExactlyInAnyOrder(genreAId, genreBId);
    }

    @Test
    @DisplayName("일부 겹치는 장르 목록으로 수정하면 성공하고 장르가 교체된다 ([A, B] → [B, C])")
    void partiallyOverlappingGenreList_succeeds_andReplacesGenres() {

        Long genreAId = createGenre("MUG2 장르A");
        Long genreBId = createGenre("MUG2 장르B");
        Long genreCId = createGenre("MUG2 장르C");

        Long movieId = movieService.createMovie(
                "MUG2 영화", null, null, List.of(genreAId, genreBId), 120).getId();

        MovieResponse response = movieService.updateMovie(
                movieId, "MUG2 영화", null, "UPCOMING", "DEACTIVATED", null,
                List.of(genreBId, genreCId), 120);

        assertThat(response.getGenreNames()).containsExactlyInAnyOrder("MUG2 장르B", "MUG2 장르C");
        assertThat(movieGenreRepository.findByMovieId(movieId))
                .extracting(movieGenre -> movieGenre.getGenre().getId())
                .containsExactlyInAnyOrder(genreBId, genreCId);
    }

    @Test
    @DisplayName("전부 다른 장르 목록으로 수정하면 성공하고 장르가 교체된다 ([A] → [B])")
    void fullyDifferentGenreList_succeeds_andReplacesGenres() {

        Long genreAId = createGenre("MUG3 장르A");
        Long genreBId = createGenre("MUG3 장르B");

        Long movieId = movieService.createMovie(
                "MUG3 영화", null, null, List.of(genreAId), 120).getId();

        MovieResponse response = movieService.updateMovie(
                movieId, "MUG3 영화", null, "UPCOMING", "DEACTIVATED", null,
                List.of(genreBId), 120);

        assertThat(response.getGenreNames()).containsExactly("MUG3 장르B");
        assertThat(movieGenreRepository.findByMovieId(movieId))
                .extracting(movieGenre -> movieGenre.getGenre().getId())
                .containsExactly(genreBId);
    }

    @Test
    @DisplayName("장르 목록을 null로 수정하면 연결된 장르가 모두 없어진다 ([A, B] → null)")
    void nullGenreList_succeeds_andRemovesAllGenres() {

        Long genreAId = createGenre("MUG4 장르A");
        Long genreBId = createGenre("MUG4 장르B");

        Long movieId = movieService.createMovie(
                "MUG4 영화", null, null, List.of(genreAId, genreBId), 120).getId();

        MovieResponse response = movieService.updateMovie(
                movieId, "MUG4 영화", null, "UPCOMING", "DEACTIVATED", null,
                null, 120);

        assertThat(response.getGenreNames()).isEmpty();
        assertThat(movieGenreRepository.findByMovieId(movieId)).isEmpty();
    }

    @Test
    @DisplayName("장르 목록을 빈 리스트로 수정하면 연결된 장르가 모두 없어진다 ([A, B] → 빈 리스트)")
    void emptyGenreList_succeeds_andRemovesAllGenres() {

        Long genreAId = createGenre("MUG5 장르A");
        Long genreBId = createGenre("MUG5 장르B");

        Long movieId = movieService.createMovie(
                "MUG5 영화", null, null, List.of(genreAId, genreBId), 120).getId();

        MovieResponse response = movieService.updateMovie(
                movieId, "MUG5 영화", null, "UPCOMING", "DEACTIVATED", null,
                List.of(), 120);

        assertThat(response.getGenreNames()).isEmpty();
        assertThat(movieGenreRepository.findByMovieId(movieId)).isEmpty();
    }

    // Genre에는 아직 서비스 계층이 없어(GenreRepository만 존재) 리포지토리로 직접 생성한다.
    // (MovieSearchTest.createGenre()와 같은 방식)
    private Long createGenre(String name) {

        Genre genre = new Genre();
        genre.updateName(name);
        return genreRepository.save(genre).getId();
    }
}
