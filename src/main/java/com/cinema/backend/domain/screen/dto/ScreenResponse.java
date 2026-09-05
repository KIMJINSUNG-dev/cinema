/***
 * ScreenService.java를 통해 프론트엔드로 반환하는 응답 객체를 생성하는 클래스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen.dto;

import com.cinema.backend.domain.screen.Screen;
import lombok.Getter;

@Getter
public class ScreenResponse {

    private Long id;
    private String name;
    private Screen.ActiveStatus activeStatus;

    public ScreenResponse(Screen screen) {

        this.id = screen.getId();
        this.name = screen.getName();
        this.activeStatus = screen.getActiveStatus();
    }
}
