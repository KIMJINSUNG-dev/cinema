/***
 * AuthController.java에서의 login 요청을 객체화하는 클래스
 * 작성이력
 * -2026-09-11: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.auth.dto;

import lombok.Getter;

@Getter
public class LogoutRequest {

    private String email;
    private String deviceId;

    public LogoutRequest(String email, String deviceId) {

        this.email = email;
        this.deviceId = deviceId;
    }
}
