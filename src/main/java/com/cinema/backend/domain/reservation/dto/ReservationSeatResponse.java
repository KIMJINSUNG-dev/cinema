/***
 * ReservationResponse.java에 포함되는 좌석별 응답 정보를 생성하는 클래스
 * 작성이력
 * -2026-10-30: 파일 생성 및 작성 (SeatResponse.java 구조 참고)
 * -2026-10-30: 필드명 명확화를 위해 id를 seatId로 변경
 ***/
package com.cinema.backend.domain.reservation.dto;

import com.cinema.backend.domain.reservation.ReservationSeat;
import lombok.Getter;

@Getter
public class ReservationSeatResponse {

    private Long seatId;
    private String seatCode;
    private int price;

    public ReservationSeatResponse(ReservationSeat reservationSeat) {

        this.seatId = reservationSeat.getSeat().getId();
        this.seatCode = reservationSeat.getSeat().getSeatCode();
        this.price = reservationSeat.getPrice();
    }
}
