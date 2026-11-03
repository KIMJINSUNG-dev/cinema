/***
 * MovieService.searchMovies()(QueryDSL 기반 동적 검색, MovieRepositoryCustomImpl.search() 사용)
 * 통합 테스트를 담당하는 클래스
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class MovieSearchTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private GenreRepository genreRepository;

    @Test
    @DisplayName("조건 없음: ACTIVE 영화는 포함되고 DEACTIVATED 영화는 포함되지 않는다")
    void noCondition_includesActiveAndExcludesDeactivated() {

        Long activeId = createActiveMovie("SRCH1 공개영화", null, Movie.MovieStatus.UPCOMING);
        Long deactivatedId = createDeactivatedMovie("SRCH1 비공개영화");

        Page<MovieResponse> page = movieService.searchMovies(null, null, null, PageRequest.of(0, 1000));

        assertThat(page.getContent())
                .extracting(MovieResponse::getId)
                .contains(activeId)
                .doesNotContain(deactivatedId);
    }

    @Test
    @DisplayName("keyword: 부분 일치, 대소문자를 무시하고 검색된다")
    void keyword_partialMatch_caseInsensitive() {

        Long matchedId = createActiveMovie("SRCH2 AvengersFinale", null, Movie.MovieStatus.UPCOMING);
        Long notMatchedId = createActiveMovie("SRCH2 OtherTitle", null, Movie.MovieStatus.UPCOMING);

        Page<MovieResponse> page = movieService.searchMovies("avengersfin", null, null, PageRequest.of(0, 100));

        assertThat(page.getContent())
                .extracting(MovieResponse::getId)
                .contains(matchedId)
                .doesNotContain(notMatchedId);
    }

    @Test
    @DisplayName("keyword가 공백 문자열이면 조건 없음과 동일하게 동작한다")
    void blankKeyword_sameAsNoCondition() {

        Long activeId = createActiveMovie("SRCH3 BlankCheckMovie", null, Movie.MovieStatus.UPCOMING);

        Page<MovieResponse> page = movieService.searchMovies("   ", null, null, PageRequest.of(0, 1000));

        assertThat(page.getContent())
                .extracting(MovieResponse::getId)
                .contains(activeId);
    }

    @Test
    @DisplayName("genreId: 해당 장르가 연결된 영화만 조회된다")
    void genreId_filtersOnlyMoviesLinkedToGenre() {

        Long genreId = createGenre("SRCH4 장르");
        Long withGenreId = createActiveMovie("SRCH4 장르있는영화", List.of(genreId), Movie.MovieStatus.UPCOMING);
        Long withoutGenreId = createActiveMovie("SRCH4 장르없는영화", null, Movie.MovieStatus.UPCOMING);

        Page<MovieResponse> page = movieService.searchMovies(null, genreId, null, PageRequest.of(0, 100));

        assertThat(page.getContent())
                .extracting(MovieResponse::getId)
                .contains(withGenreId)
                .doesNotContain(withoutGenreId);
    }

    @Test
    @DisplayName("장르가 2개 연결된 영화는 genreId 검색에서 중복 없이 1건으로 나오고 totalElements도 1이다")
    void movieWithTwoGenres_genreIdSearch_returnsOneUniqueResult() {

        Long genreAId = createGenre("SRCH5 장르A");
        Long genreBId = createGenre("SRCH5 장르B");
        Long movieId = createActiveMovie("SRCH5 두장르영화", List.of(genreAId, genreBId), Movie.MovieStatus.UPCOMING);

        Page<MovieResponse> page = movieService.searchMovies(null, genreAId, null, PageRequest.of(0, 100));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent())
                .extracting(MovieResponse::getId)
                .containsExactly(movieId);
    }

    @Test
    @DisplayName("존재하지 않는 genreId로 검색하면 빈 페이지가 반환되고 totalElements는 0이다")
    void nonExistentGenreId_emptyPage_totalElementsZero() {

        Page<MovieResponse> page = movieService.searchMovies(null, 999_999_999L, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("movieStatus: 해당 상태의 영화만 조회된다")
    void movieStatus_filtersOnlyMatchingStatus() {

        Long showingId = createActiveMovie("SRCH7 상영중영화", null, Movie.MovieStatus.SHOWING);
        Long endedId = createActiveMovie("SRCH7 상영종료영화", null, Movie.MovieStatus.ENDED);

        Page<MovieResponse> page = movieService.searchMovies(
                null, null, Movie.MovieStatus.SHOWING, PageRequest.of(0, 100));

        assertThat(page.getContent())
                .extracting(MovieResponse::getId)
                .contains(showingId)
                .doesNotContain(endedId);
    }

    @Test
    @DisplayName("keyword, genreId, movieStatus를 조합하면 모든 조건을 만족하는 영화만 조회된다")
    void combinedConditions_allMustMatch() {

        Long genreId = createGenre("SRCH8 장르");

        // 모든 조건(keyword "Target" 포함 · 장르 연결 · SHOWING)을 만족하는 영화
        Long targetId = createActiveMovie("SRCH8 TargetMovie", List.of(genreId), Movie.MovieStatus.SHOWING);
        // keyword만 불일치
        Long otherNameId = createActiveMovie("SRCH8 OtherName", List.of(genreId), Movie.MovieStatus.SHOWING);
        // genreId만 불일치(장르 미연결)
        Long noGenreId = createActiveMovie("SRCH8 Target2Movie", null, Movie.MovieStatus.SHOWING);
        // movieStatus만 불일치(ENDED)
        Long endedId = createActiveMovie("SRCH8 Target3Movie", List.of(genreId), Movie.MovieStatus.ENDED);

        Page<MovieResponse> page = movieService.searchMovies(
                "Target", genreId, Movie.MovieStatus.SHOWING, PageRequest.of(0, 100));

        assertThat(page.getContent())
                .extracting(MovieResponse::getId)
                .containsExactly(targetId);
        assertThat(page.getContent())
                .extracting(MovieResponse::getId)
                .doesNotContain(otherNameId, noGenreId, endedId);
    }

    @Test
    @DisplayName("정렬: 나중에 생성한 영화가 먼저 나온다 (createdAt DESC, id DESC)")
    void sorting_newerCreatedMovieComesFirst() {

        Long firstId = createActiveMovie("SRCH9 Movie1", null, Movie.MovieStatus.UPCOMING);
        Long secondId = createActiveMovie("SRCH9 Movie2", null, Movie.MovieStatus.UPCOMING);

        Page<MovieResponse> page = movieService.searchMovies("SRCH9", null, null, PageRequest.of(0, 100));

        assertThat(page.getContent())
                .extracting(MovieResponse::getId)
                .containsExactly(secondId, firstId);
    }

    @Test
    @DisplayName("페이징: 5건을 size 2로 조회하면 첫 페이지 2건, 마지막 페이지 1건이고 totalPages는 3이다")
    void paging_fiveItems_sizeTwo() {

        for (int i = 1; i <= 5; i++) {

            createActiveMovie("SRCH10Paging Movie" + i, null, Movie.MovieStatus.UPCOMING);
        }

        Page<MovieResponse> firstPage = movieService.searchMovies(
                "SRCH10Paging", null, null, PageRequest.of(0, 2));
        Page<MovieResponse> lastPage = movieService.searchMovies(
                "SRCH10Paging", null, null, PageRequest.of(2, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(lastPage.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("응답의 genreNames에 연결된 장르 이름이 담긴다")
    void genreNames_includedInResponse() {

        Long genreXId = createGenre("SRCH11 장르X");
        Long genreYId = createGenre("SRCH11 장르Y");
        createActiveMovie("SRCH11 Movie", List.of(genreXId, genreYId), Movie.MovieStatus.UPCOMING);

        Page<MovieResponse> page = movieService.searchMovies("SRCH11 Movie", null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getGenreNames())
                .containsExactlyInAnyOrder("SRCH11 장르X", "SRCH11 장르Y");
    }

    // Movie의 displayStatus 기본값은 DEACTIVATED이므로, 생성 후 updateMovie()로 ACTIVE로 전환한다.
    private Long createActiveMovie(String name, List<Long> genreIds, Movie.MovieStatus movieStatus) {

        Long id = movieService.createMovie(name, null, null, null, 120).getId();
        movieService.updateMovie(id, name, null, movieStatus.name(), "ACTIVE", null, genreIds, 120);
        return id;
    }

    // Movie의 displayStatus 기본값(DEACTIVATED)을 그대로 유지하기 위해 updateMovie()를 호출하지 않는다.
    private Long createDeactivatedMovie(String name) {

        return movieService.createMovie(name, null, null, null, 120).getId();
    }

    // Genre에는 아직 서비스 계층이 없어(GenreRepository만 존재) 리포지토리로 직접 생성한다.
    private Long createGenre(String name) {

        Genre genre = new Genre();
        genre.updateName(name);
        return genreRepository.save(genre).getId();
    }
}
