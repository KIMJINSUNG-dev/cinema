/***
 * MemberController.java에서의 MemberCreate 요청을 객체화하는 클래스
 * 작성이력
 * -2026-09-10: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberCreateRequest {

    private String email;
    private String password;
    private String nickname;

    public MemberCreateRequest(String email, String password, String nickname) {

        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}
