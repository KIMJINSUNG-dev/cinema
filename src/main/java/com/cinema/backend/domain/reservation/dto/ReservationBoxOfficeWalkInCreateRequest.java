/***
 * ReservationController.java에서의 현장 판매 요청을 객체화하는 클래스
 * 작성이력
 * -2026-10-31: 파일 생성 및 작성 (ReservationOnlineCreateRequest.java 구조 참고)
 *
 * 예약 주인은 현장 판매 시스템 계정으로 서버가 정하고, 처리 직원은 로그인 정보
 * (@AuthenticationPrincipal)에서 받으므로 이 DTO에는 포함하지 않는다(D-43 · D-46).
 ***/
package com.cinema.backend.domain.reservation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ReservationBoxOfficeWalkInCreateRequest {

    private Long showtimeId;
    private List<Long> seatIds;

    public ReservationBoxOfficeWalkInCreateRequest(
            Long showtimeId,
            List<Long> seatIds) {

        this.showtimeId = showtimeId;
        this.seatIds = seatIds;
    }
}
