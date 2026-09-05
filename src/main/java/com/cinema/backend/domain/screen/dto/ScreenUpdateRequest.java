/***
 * ScreenController.java에서의 ScreenUpdate 요청을 객체화하는 클래스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScreenUpdateRequest {

    private String name;
    private String activeStatus;

    public ScreenUpdateRequest(String name, String activeStatus) {

        this.name = name;
        this.activeStatus = activeStatus;
    }
}
