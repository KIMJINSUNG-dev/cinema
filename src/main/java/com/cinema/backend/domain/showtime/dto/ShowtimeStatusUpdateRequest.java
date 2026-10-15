/***
 * ShowtimeController.java에서의 ShowtimeStatusUpdate 요청을 객체화하는 클래스
 * 작성이력
 * -2026-10-15: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.showtime.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ShowtimeStatusUpdateRequest {

    private String showtimeStatus;

    public ShowtimeStatusUpdateRequest(String showtimeStatus) {

        this.showtimeStatus = showtimeStatus;
    }
}
