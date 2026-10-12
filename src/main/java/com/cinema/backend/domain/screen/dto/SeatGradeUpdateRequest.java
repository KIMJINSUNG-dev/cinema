/***
 * SeatGradeController.java에서의 SeatGradeUpdate 요청을 객체화하는 클래스
 * 작성이력
 * -2026-10-12: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SeatGradeUpdateRequest {

    private String  name;
    private Integer basePrice;
    private String  seatGradeStatus;

    public SeatGradeUpdateRequest(String name, Integer basePrice, String seatGradeStatus) {

        this.name            = name;
        this.basePrice       = basePrice;
        this.seatGradeStatus = seatGradeStatus;
    }
}
