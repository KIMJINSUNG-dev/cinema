/***
 * 예외 처리기 테스트를 담당하는 클래스
 * 작성이력
 * -2026-10-28: 파일 생성 및 작성
 ***/
package com.cinema.backend.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("DataIntegrityViolationException은 409 및 고정 메시지 반환, DB 오류 메시지는 노출하지 않음")
    public void handleDataIntegrityViolationExceptionTest() {

        ResponseEntity<String> response =
                globalExceptionHandler.handleDataIntegrityViolationException(
                        new DataIntegrityViolationException(
                                "Duplicate entry 'x' for key 'uk_test'"
                        )
                );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .isEqualTo("요청이 현재 데이터 상태와 충돌합니다. 다시 시도해 주세요.")
                .doesNotContain("Duplicate entry", "uk_test");
    }
}
