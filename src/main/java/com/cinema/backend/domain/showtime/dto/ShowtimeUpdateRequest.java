/***
 * ShowtimeController.java에서의 ShowtimeUpdate 요청을 객체화하는 클래스
 * 작성이력
 * -2026-10-14: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.showtime.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ShowtimeUpdateRequest {

    private LocalDateTime startTime;
    private String showtimeStatus;

    public ShowtimeUpdateRequest(
            LocalDateTime startTime,
            String showtimeStatus) {

        this.startTime = startTime;
        this.showtimeStatus = showtimeStatus;
    }
}
