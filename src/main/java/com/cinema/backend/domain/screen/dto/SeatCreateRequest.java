/***
 * SeatController.java에서의 SeatCreate 요청을 객체화하는 클래스
 * 작성이력
 * -2026-10-07: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SeatCreateRequest {

    private Long   seatGradeId;
    private String row;
    private int    column;
    private String seatStatus;

    public SeatCreateRequest(Long seatGradeId, String row, int column, String seatStatus) {

        this.seatGradeId = seatGradeId;
        this.row         = row;
        this.column      = column;
        this.seatStatus  = seatStatus;
    }
}
