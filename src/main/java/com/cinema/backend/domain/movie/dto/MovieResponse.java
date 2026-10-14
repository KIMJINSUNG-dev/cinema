/***
 * MovieService.java를 통해 프론트엔드로 반환하는 응답 객체를 생성하는 클래스
 * 작성이력
 * -2026-09-08: 파일 생성 및 작성
 * -2026-09-09: 영화에 종속되는 장르 구현 업데이트
 * -2026-10-14: runningTime 필드 추가 및 생성자에 반영
 ***/
package com.cinema.backend.domain.movie.dto;

import com.cinema.backend.domain.movie.Movie;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class MovieResponse {

    private Long id;
    private String name;
    private LocalDate releaseDate;
    private Movie.MovieStatus movieStatus;
    private Movie.DisplayStatus displayStatus;
    private Movie.MovieRating movieRating;
    private List<String> genreNames;
    private Integer runningTime;

    public MovieResponse(Movie movie, List<String> genreNames) {

        this.id = movie.getId();
        this.name = movie.getName();
        this.releaseDate = movie.getReleaseDate();
        this.movieStatus = movie.getMovieStatus();
        this.displayStatus = movie.getDisplayStatus();
        this.movieRating = movie.getMovieRating();
        this.genreNames = genreNames;
        this.runningTime = movie.getRunningTime();
    }
}
