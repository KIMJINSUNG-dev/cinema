/***
 * ShowtimeService.java를 통해 프론트엔드로 반환하는 고객용 좌석 목록 응답 객체를 생성하는 클래스
 * 작성이력
 * -2026-11-11: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.showtime.dto;

import com.cinema.backend.domain.showtime.Showtime;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ShowtimeSeatsResponse {

    private Long showtimeId;
    private String movieName;
    private String screenName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<SeatAvailabilityResponse> seats;

    public ShowtimeSeatsResponse(Showtime showtime, List<SeatAvailabilityResponse> seats) {

        this.showtimeId = showtime.getId();
        this.movieName = showtime.getMovie().getName();
        this.screenName = showtime.getScreen().getName();
        this.startTime = showtime.getStartTime();
        this.endTime = showtime.getEndTime();
        this.seats = seats;
    }
}
