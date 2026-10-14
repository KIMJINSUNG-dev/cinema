/***
 * ShowtimeService.java를 통해 프론트엔드로 반환하는 응답 객체를 생성하는 클래스
 * 작성이력
 * -2026-10-14: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.showtime.dto;

import com.cinema.backend.domain.showtime.Showtime;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ShowtimeResponse {

    private Long id;
    private Long movieId;
    private Long screenId;
    private String movieName;
    private String screenName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Showtime.ShowtimeStatus showtimeStatus;

    public ShowtimeResponse(Showtime showtime) {

        this.id = showtime.getId();
        this.movieId = showtime.getMovie().getId();
        this.screenId = showtime.getScreen().getId();
        this.movieName = showtime.getMovie().getName();
        this.screenName = showtime.getScreen().getName();
        this.startTime = showtime.getStartTime();
        this.endTime = showtime.getEndTime();
        this.showtimeStatus = showtime.getShowtimeStatus();
    }
}
