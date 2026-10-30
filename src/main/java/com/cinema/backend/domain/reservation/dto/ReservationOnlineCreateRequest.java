/***
 * ReservationController.java에서의 온라인 예매 요청을 객체화하는 클래스
 * 작성이력
 * -2026-10-30: 파일 생성 및 작성 (ShowtimeCreateRequest.java 구조 참고)
 *
 * 예약 주체(회원)는 요청 본문이 아니라 로그인 정보(@AuthenticationPrincipal)에서 받으므로
 * 이 DTO에는 포함하지 않는다.
 ***/
package com.cinema.backend.domain.reservation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ReservationOnlineCreateRequest {

    private Long showtimeId;
    private List<Long> seatIds;

    public ReservationOnlineCreateRequest(
            Long showtimeId,
            List<Long> seatIds) {

        this.showtimeId = showtimeId;
        this.seatIds = seatIds;
    }
}
