/***
 * 예외 처리기 테스트를 담당하는 클래스
 * 작성이력
 * -2026-10-28: 파일 생성 및 작성
 * -2026-11-02: 잘못된 정렬 파라미터 전달 시 의도한 응답 및 메시지를 반환하는지,
 *              DB 오류 메시지를 포함하지 않았는지 확인하는
 *              handlePropertyReferenceExceptionTest 추가
 ***/
package com.cinema.backend.global.exception;

import com.cinema.backend.domain.reservation.Reservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

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

    @Test
    @DisplayName("PropertyReferenceException은 400 및 고정 메시지 반환, DB 오류 메시지는 노출하지 않음")
    public void handlePropertyReferenceExceptionTest() {

        String wrongPropertyName = "wrongPropertyName";
        TypeInformation<?> searchedType = TypeInformation.of(Reservation.class);
        List<? extends PropertyPath> emptyPropertyPath = Collections.emptyList();

        ResponseEntity<String> response =
                globalExceptionHandler.handlePropertyReferenceException(
                        new PropertyReferenceException(
                                wrongPropertyName,
                                searchedType,
                                emptyPropertyPath
                        )
                );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .doesNotContain("Reservation", "wrongPropertyName")
                .isEqualTo("정렬 기준이 올바르지 않습니다.");
    }
}
