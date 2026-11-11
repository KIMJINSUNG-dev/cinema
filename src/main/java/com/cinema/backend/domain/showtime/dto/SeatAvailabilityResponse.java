/***
 * ShowtimeService.java를 통해 프론트엔드로 반환하는 고객용 좌석 응답 객체를 생성하는 클래스
 * 작성이력
 * -2026-11-11: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.showtime.dto;

import com.cinema.backend.domain.screen.Seat;
import lombok.Getter;

@Getter
public class SeatAvailabilityResponse {

    public enum SeatAvailabilityStatus {

        AVAILABLE, RESERVED, UNAVAILABLE
    }

    private Long seatId;
    private String seatCode;
    private int row;
    private int column;
    private String seatGradeName;
    private int price;
    private SeatAvailabilityStatus status;

    public SeatAvailabilityResponse(Seat seat, SeatAvailabilityStatus status) {

        this.seatId = seat.getId();
        this.seatCode = seat.getSeatCode();
        this.row = seat.getRow();
        this.column = seat.getColumn();
        this.seatGradeName = seat.getSeatGrade().getName();
        this.price = seat.getSeatGrade().getBasePrice();
        this.status = status;
    }
}
