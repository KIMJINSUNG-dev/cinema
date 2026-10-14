/***
 * MovieController.java에서의 MovieUpdate 요청을 객체화하는 클래스
 * 작성이력
 * -2026-09-08: 파일 생성 및 작성
 * -2026-09-09: 영화에 종속되는 장르 구현 작업
 * -2026-10-14: runningTime 필드 추가 및 생성자에 반영
 ***/
package com.cinema.backend.domain.movie.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class MovieUpdateRequest {

    private String name;
    private LocalDate releaseDate;
    private String movieStatus;
    private String displayStatus;
    private String movieRating;
    private List<Long> genreIds;
    private Integer runningTime;

    public MovieUpdateRequest(
            String name,
            LocalDate releaseDate,
            String movieStatus,
            String displayStatus,
            String movieRating,
            List<Long> genreIds,
            Integer runningTime) {

        this.name = name;
        this.releaseDate = releaseDate;
        this.movieStatus = movieStatus;
        this.displayStatus = displayStatus;
        this.movieRating = movieRating;
        this.genreIds = genreIds;
        this.runningTime = runningTime;
    }
}
