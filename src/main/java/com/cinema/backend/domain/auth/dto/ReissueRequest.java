/***
 * AuthController.java에서의 reissue 요청을 객체화하는 클래스
 * 작성이력
 * -2026-09-11: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.auth.dto;

import lombok.Getter;

@Getter
public class ReissueRequest {

    private String deviceId;

    public ReissueRequest(String deviceId) {

        this.deviceId = deviceId;
    }
}
