/***
 * ShowtimeController.java에서의 ShowtimeCreate 요청을 객체화하는 클래스
 * 작성이력
 * -2026-10-14: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.showtime.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ShowtimeCreateRequest {

    private Long movieId;
    private Long screenId;
    private LocalDateTime startTime;

    public ShowtimeCreateRequest(
            Long movieId,
            Long screenId,
            LocalDateTime startTime) {

        this.movieId = movieId;
        this.screenId = screenId;
        this.startTime = startTime;
    }
}
