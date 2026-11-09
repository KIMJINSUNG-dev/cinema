/***
 * 요청 횟수 초과를 나타내는 예외 클래스
 * (HttpStatus.TOO_MANY_REQUESTS, 429)
 * 작성이력
 * -2026-11-09: 파일 생성 및 작성
 ***/
package com.cinema.backend.global.exception;

public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {

        super(message);
    }
}
