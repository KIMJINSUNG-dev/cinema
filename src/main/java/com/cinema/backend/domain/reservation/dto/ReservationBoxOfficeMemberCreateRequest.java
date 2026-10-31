/***
 * ReservationController.java에서의 회원 대리 예약 요청을 객체화하는 클래스
 * 작성이력
 * -2026-10-31: 파일 생성 및 작성 (ReservationOnlineCreateRequest.java 구조 참고)
 *
 * 예약 주인(대상 회원)은 본문의 memberId로 받고, 처리 직원은 로그인 정보(@AuthenticationPrincipal)에서
 * 받으므로 이 DTO에는 포함하지 않는다(D-43 · D-46).
 ***/
package com.cinema.backend.domain.reservation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ReservationBoxOfficeMemberCreateRequest {

    private Long memberId;
    private Long showtimeId;
    private List<Long> seatIds;

    public ReservationBoxOfficeMemberCreateRequest(
            Long memberId,
            Long showtimeId,
            List<Long> seatIds) {

        this.memberId = memberId;
        this.showtimeId = showtimeId;
        this.seatIds = seatIds;
    }
}
