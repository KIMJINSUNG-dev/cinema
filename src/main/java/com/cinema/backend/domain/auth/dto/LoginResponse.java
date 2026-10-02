/***
 * AuthService.java를 통해 프론트엔드로 반환하는 응답 객체를 생성하는 클래스
 * 작성이력
 * -2026-09-11: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String nickname;

    public LoginResponse(
            String accessToken,
            String refreshToken,
            String nickname) {

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.nickname = nickname;
    }

    public LoginResponse(
            String accessToken,
            String nickname) {

        this.accessToken = accessToken;
        this.nickname = nickname;
    }
}
