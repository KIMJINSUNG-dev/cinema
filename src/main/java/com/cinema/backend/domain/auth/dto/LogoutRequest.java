/***
 * AuthController.java에서의 logout 요청을 객체화하는 클래스
 * 작성이력
 * -2026-09-11: 파일 생성 및 작성
 * -2026-10-15: AuthController.java의 logout() 메서드의 email 입력 방식 변경으로 email 필드 삭제
 * -2026-10-15: 코드 일관성을 위해 @NoArgsConstructor 어노테이션 추가
 ***/
package com.cinema.backend.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutRequest {

    private String deviceId;

    public LogoutRequest(String deviceId) {

        this.deviceId = deviceId;
    }
}
