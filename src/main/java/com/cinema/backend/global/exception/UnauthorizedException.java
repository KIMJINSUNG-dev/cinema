/***
 * 인증되지 않은 요청을 나타내는 예외 클래스
 * (HttpStatus.UNAUTHORIZED, 401)
 * 작성이력
 * -2026-11-09: 파일 생성 및 작성
 ***/
package com.cinema.backend.global.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {

        super(message);
    }
}
