/***
 * 현재 상태와 충돌하는 요청을 나타내는 예외 클래스
 * (HttpStatus.CONFLICT, 409)
 * 작성이력
 * -2026-10-25: 파일 생성 및 작성
 ***/
package com.cinema.backend.global.exception;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {

        super(message);
    }
}
