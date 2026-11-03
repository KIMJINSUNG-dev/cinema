/***
 * Movie.java 엔티티 데이터에 접근하는 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-09-08: 파일 생성 및 작성
 * -2026-09-09: 영화에 종속되는 장르 구현 작업
 * -2026-10-14: createMovie(), updateMovie() 메서드에 runningTime 반영
 * -2026-11-03: QueryDSL 적용에 따라 findAll(), findByName() 메서드를 searchMovies() 메서드로 리워크
 * -2026-11-03: updateMovie()에서 기존 장르와 겹치는 장르로 수정 시 유니크 제약 위반(409)이 발생하는 버그 수정
 *              (IDENTITY 즉시 INSERT가 지연된 DELETE보다 먼저 실행되던 문제 → deleteAll() 직후 flush() 추가)
 ***/
package com.cinema.backend.domain.movie;

import com.cinema.backend.domain.movie.dto.MovieResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;

    @Transactional
    public MovieResponse createMovie(
            String name,
            LocalDate releaseDate,
            String movieRating,
            List<Long> genreIds,
            Integer runningTime) {

        Movie movie = new Movie();

        movie.updateName(name);
        movie.updateReleaseDate(releaseDate);
        movie.updateMovieRating(movieRating);
        movie.updateRunningTime(runningTime);

        movieRepository.save(movie);

        List<String> genreNames = new ArrayList<>();

        if (genreIds != null && !genreIds.isEmpty()) {

            for(Long genreId : genreIds) {

                Genre genre = genreRepository.findById(genreId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장르입니다."));

                MovieGenre movieGenre = new MovieGenre(movie, genre);
                movieGenreRepository.save(movieGenre);
                genreNames.add(genre.getName());
            }
        }

        return new MovieResponse(movie, genreNames);
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> searchMovies(
            String keyword,
            Long genreId,
            Movie.MovieStatus movieStatus,
            Pageable pageable) {

        return movieRepository.search(keyword, genreId, movieStatus, pageable)
                .map(movie -> new MovieResponse(
                        movie,
                        toGenreNames(movieGenreRepository.findByMovieId(movie.getId()))
                ));
    }

    @Transactional(readOnly = true)
    public MovieResponse findById(Long id) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 영화입니다."));

        return new MovieResponse(
                movie,
                toGenreNames(movieGenreRepository.findByMovieId(movie.getId())));
    }

    @Transactional
    public MovieResponse updateMovie(
            Long id,
            String name,
            LocalDate releaseDate,
            String movieStatus,
            String displayStatus,
            String movieRating,
            List<Long> genreIds,
            Integer runningTime) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 영화입니다."));

        movie.updateName(name);
        movie.updateReleaseDate(releaseDate);
        movie.updateMovieStatus(movieStatus);
        movie.updateDisplayStatus(displayStatus);
        movie.updateMovieRating(movieRating);
        movie.updateRunningTime(runningTime);

        movieGenreRepository.deleteAll(movieGenreRepository.findByMovieId(id));
        // IDENTITY 전략에서 save()는 INSERT를 즉시 실행하지만 deleteAll()의 DELETE는 플러시까지 지연된다.
        // 기존 장르와 겹치는 장르를 다시 저장할 때 유니크 제약 위반이 나지 않도록 DELETE를 먼저 반영한다.
        movieGenreRepository.flush();

        List<String> genreNames = new ArrayList<>();

        if (genreIds != null && !genreIds.isEmpty()) {

            for (Long genreId : genreIds) {

                Genre genre = genreRepository.findById(genreId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장르입니다."));

                MovieGenre movieGenre = new MovieGenre(movie, genre);
                movieGenreRepository.save(movieGenre);
                genreNames.add(genre.getName());
            }
        }

        return new MovieResponse(movie, genreNames);
    }

    private List<String> toGenreNames(List<MovieGenre> movieGenres) {

        return movieGenres.stream()
                .map(genres -> genres.getGenre().getName())
                .toList();
    }
}
