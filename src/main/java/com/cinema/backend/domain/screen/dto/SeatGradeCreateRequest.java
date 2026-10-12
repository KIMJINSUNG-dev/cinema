/***
 * SeatGradeController.java에서의 SeatGradeCreate 요청을 객체화하는 클래스
 * 작성이력
 * -2026-10-12: 파일 생성 및 작성
 *
 * basePrice는 누락(요청에 필드 자체가 없음)과 명시적 0을 구분하기 위해
 * int가 아닌 Integer로 선언함 (SeatCreateRequest 참고, 다만 Seat 쪽은 이 구분이 필요 없어 int 사용)
 ***/
package com.cinema.backend.domain.screen.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SeatGradeCreateRequest {

    private String  name;
    private Integer basePrice;
    private String  seatGradeStatus;

    public SeatGradeCreateRequest(String name, Integer basePrice, String seatGradeStatus) {

        this.name            = name;
        this.basePrice       = basePrice;
        this.seatGradeStatus = seatGradeStatus;
    }
}
