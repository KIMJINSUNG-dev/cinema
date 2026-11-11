/***
 * 예외 처리기 테스트를 담당하는 클래스
 * 작성이력
 * -2026-10-28: 파일 생성 및 작성
 * -2026-11-02: 잘못된 정렬 파라미터 전달 시 의도한 응답 및 메시지를 반환하는지,
 *              엔티티 구조를 메시지에 포함하지 않았는지 확인하는
 *              handlePropertyReferenceExceptionTest 추가
 * -2026-11-11: UnauthorizedException, TooManyRequestsException, MissingRequestCookieException
 *              핸들러(D-53) 테스트 추가
 ***/
package com.cinema.backend.global.exception;

import com.cinema.backend.domain.reservation.Reservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestCookieException;

import java.lang.reflect.Method;
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
    @DisplayName("PropertyReferenceException은 400 및 고정 메시지 반환, 엔티티 구조는 노출하지 않음")
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

    @Test
    @DisplayName("UnauthorizedException은 401 및 예외 메시지 그대로 반환")
    public void handleUnauthorizedExceptionTest() {

        String message = "아이디 또는 비밀번호가 맞지 않습니다.";

        ResponseEntity<String> response =
                globalExceptionHandler.handleUnauthorizedException(new UnauthorizedException(message));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(message);
    }

    @Test
    @DisplayName("TooManyRequestsException은 429 및 예외 메시지 그대로 반환")
    public void handleTooManyRequestsExceptionTest() {

        String message = "로그인 시도 횟수가 초과되었습니다.";

        ResponseEntity<String> response =
                globalExceptionHandler.handleTooManyRequestsException(new TooManyRequestsException(message));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isEqualTo(message);
    }

    // MissingRequestCookieException 생성에 필요한 MethodParameter는 리플렉션으로 메서드 정보를 얻어 만듦
    // (쿠키 이름만 쓰이므로 파라미터에 특별한 애노테이션은 필요 없음)
    private void dummyMethodWithCookieParameter(String refreshToken) {
    }

    @Test
    @DisplayName("MissingRequestCookieException은 401 및 AuthService의 재발급 실패 메시지와 같은 고정 메시지 반환")
    public void handleMissingRequestCookieExceptionTest() throws NoSuchMethodException {

        Method dummyMethod = GlobalExceptionHandlerTest.class
                .getDeclaredMethod("dummyMethodWithCookieParameter", String.class);
        MethodParameter methodParameter = new MethodParameter(dummyMethod, 0);

        ResponseEntity<String> response =
                globalExceptionHandler.handleMissingRequestCookieException(
                        new MissingRequestCookieException("refreshToken", methodParameter)
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // AuthService.TOKEN_REISSUE_FAILED_MESSAGE와 같은 문구
        assertThat(response.getBody()).isEqualTo("토큰 재발급에 실패했습니다.");
    }
}
