/***
 * SeatGradeService.java를 통해 프론트엔드로 반환하는 응답 객체를 생성하는 클래스
 * 작성이력
 * -2026-10-12: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen.dto;

import com.cinema.backend.domain.screen.SeatGrade;
import lombok.Getter;

@Getter
public class SeatGradeResponse {

    private Long    id;
    private String  name;
    private int     basePrice;
    private SeatGrade.SeatGradeStatus seatGradeStatus;

    public SeatGradeResponse(SeatGrade seatGrade) {

        this.id              = seatGrade.getId();
        this.name            = seatGrade.getName();
        this.basePrice       = seatGrade.getBasePrice();
        this.seatGradeStatus = seatGrade.getSeatGradeStatus();
    }
}
