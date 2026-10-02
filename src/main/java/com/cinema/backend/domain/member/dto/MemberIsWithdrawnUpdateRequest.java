/***
 * MemberController.java에서의 MemberIsWithdrawnUpdate 요청을 객체화하는 클래스
 * 작성이력
 * -2026-09-10: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberIsWithdrawnUpdateRequest {

    private Boolean isWithdrawn;

    public MemberIsWithdrawnUpdateRequest(Boolean isWithdrawn) {

        this.isWithdrawn = isWithdrawn;
    }
}
