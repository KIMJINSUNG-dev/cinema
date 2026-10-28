/***
 * Controller의 예외를 모아서 분류 처리하는 클래스
 * 작성이력
 * -2026-09-10: 파일 생성 및 작성
 * -2026-09-14: handleException() 메서드에 오류 검증 코드 추가
 * -2026-10-15: 요청 본문 파싱 실패 · 필수 파라미터 누락 · 파라미터 타입 불일치를 400으로 처리하는 핸들러 추가
 *              (ShowtimeController의 @RequestParam screenId 등에서 발생 가능)
 *              내부 예외 클래스명·스택은 로그로만 남기고 응답 본문에는 노출하지 않음
 * -2026-10-15: AccessDeniedException 핸들러 추가
 * -2026-10-23: IllegalStateException 핸들러 추가
 * -2026-10-25: IllegalStateException 핸들러가 라이브러리의 오류까지 같이 처리하는 문제를 해결하기 위해
 *              해당 핸들러를 ConflictException 핸들러로 교체
 *              (의도하지 않은 IllegalStateException은 handleException() 메서드에서 500 응답으로 처리)
 * -2026-10-28: DataIntegrityViolationException 핸들러 추가 및
 *              내부 메시지가 외부에 노출되지 않도록 작업, 개발자는 확인할 수 있도록 log.warn() 코드 추가
 ***/
package com.cinema.backend.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<String> handleConflictException(ConflictException e) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolationException(DataIntegrityViolationException e) {

        log.warn("요청과 데이터 상태 간 충돌 오류", e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body("요청이 현재 데이터 상태와 충돌합니다. 다시 시도해 주세요.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {

        log.warn("요청 본문 파싱 실패", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("요청 본문의 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("필수 요청 파라미터가 누락되었습니다: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("요청 파라미터의 형식이 올바르지 않습니다: " + e.getName());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException e) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("권한이 없습니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {

        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 내부 오류가 발생했습니다.");
    }
}
