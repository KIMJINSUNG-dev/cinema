/***
 * SeatService.java를 통해 프론트엔드로 반환하는 응답 객체를 생성하는 클래스
 * 작성이력
 * -2026-09-15: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen.dto;

import com.cinema.backend.domain.screen.Screen;
import com.cinema.backend.domain.screen.Seat;
import com.cinema.backend.domain.screen.SeatGrade;
import lombok.Getter;

@Getter
public class SeatResponse {

    private Long id;
    private Long screenId;
    private String screenName;
    private Long seatGradeId;
    private String seatGradeName;
    private int row;
    private int column;
    private Seat.SeatStatus seatStatus;

    public SeatResponse(Seat seat) {

        this.id = seat.getId();
        this.screenId = seat.getScreen().getId();
        this.screenName = seat.getScreen().getName();
        this.seatGradeId = seat.getSeatGrade().getId();
        this.seatGradeName = seat.getSeatGrade().getName();
        this.row = seat.getRow();
        this.column = seat.getColumn();
        this.seatStatus = seat.getSeatStatus();
    }
}
