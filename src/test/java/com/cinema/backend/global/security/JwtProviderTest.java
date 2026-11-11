/***
 * JwtProvider.java의 토큰 검증(validateToken) 상태 판정을 검증하는 단위 테스트(Spring · DB 없이 실행)를 담당하는 클래스
 * [AI 초안] 인증 실패 응답(D-53) 테스트 작성 - 비밀 키는 테스트 실행마다 무작위 바이트로 생성해 코드에 고정 문자열로 남기지 않음
 * 작성이력
 * -2026-11-11: 파일 생성 및 작성
 ***/
package com.cinema.backend.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final long ACCESS_EXPIRATION = 1000L * 60 * 30;
    private static final long REFRESH_EXPIRATION = 1000L * 60 * 60 * 24 * 7;

    // HS256은 256비트(32바이트) 이상의 키가 필요하므로, Base64로 인코딩한 32바이트 무작위 값(UTF-8 기준 44바이트)을 비밀 키로 사용
    private String randomSecret() {

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    private JwtProvider newProvider(long accessExpiration, long refreshExpiration) {

        return new JwtProvider(randomSecret(), accessExpiration, refreshExpiration);
    }

    private JwtProvider newProvider() {

        return newProvider(ACCESS_EXPIRATION, REFRESH_EXPIRATION);
    }

    // 서명 부분의 "가운데" 글자를 바꿔 변조된 토큰을 만듦 (형식은 그대로 유지, 서명만 달라짐)
    // 마지막 글자는 바꾸지 않음: HS256 서명(32바이트)을 Base64URL로 인코딩하면 마지막 글자는
    // 데이터 4비트 + 디코딩에 쓰이지 않는 2비트로 구성된다. 'A' <-> 'B'처럼 그 2비트만 다른
    // 글자로 바꾸면 디코딩 결과(바이트)가 같아져 서명이 우연히 그대로 유효할 수 있다
    // (비밀 키가 매번 무작위라 간헐적으로만 재현되는 실패가 됨)
    private String tamperSignature(String token) {

        String[] parts = token.split("\\.");
        String signature = parts[2];
        int middleIndex = signature.length() / 2;
        char middleChar = signature.charAt(middleIndex);
        char replacedChar = (middleChar == 'A') ? 'B' : 'A';
        String tamperedSignature = signature.substring(0, middleIndex) + replacedChar + signature.substring(middleIndex + 1);
        return parts[0] + "." + parts[1] + "." + tamperedSignature;
    }

    @Test
    @DisplayName("정상 토큰은 VALID")
    void valid_token_returns_valid() {

        JwtProvider jwtProvider = newProvider();
        String token = jwtProvider.generateAccessToken("test@test.com", "CUSTOMER");

        assertThat(jwtProvider.validateToken(token)).isEqualTo(TokenStatus.VALID);
    }

    @Test
    @DisplayName("만료된 토큰은 EXPIRED")
    void expired_token_returns_expired() {

        JwtProvider jwtProvider = newProvider(-1, -1); // 만료 시간을 음수로 주어 발급 즉시 만료되게 함
        String token = jwtProvider.generateAccessToken("test@test.com", "CUSTOMER");

        assertThat(jwtProvider.validateToken(token)).isEqualTo(TokenStatus.EXPIRED);
    }

    @Test
    @DisplayName("서명이 변조된 토큰은 INVALID")
    void tampered_signature_token_returns_invalid() {

        JwtProvider jwtProvider = newProvider();
        String token = jwtProvider.generateAccessToken("test@test.com", "CUSTOMER");
        String tamperedToken = tamperSignature(token);

        assertThat(jwtProvider.validateToken(tamperedToken)).isEqualTo(TokenStatus.INVALID);
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰은 INVALID")
    void token_signed_with_different_key_returns_invalid() {

        JwtProvider issuer = newProvider();
        JwtProvider verifier = newProvider(); // 서로 다른 무작위 비밀 키를 가진 별도 인스턴스
        String token = issuer.generateAccessToken("test@test.com", "CUSTOMER");

        assertThat(verifier.validateToken(token)).isEqualTo(TokenStatus.INVALID);
    }

    @Test
    @DisplayName("null, 빈 문자열 토큰은 예외 없이 INVALID")
    void null_or_blank_token_returns_invalid() {

        JwtProvider jwtProvider = newProvider();

        assertThat(jwtProvider.validateToken(null)).isEqualTo(TokenStatus.INVALID);
        assertThat(jwtProvider.validateToken("")).isEqualTo(TokenStatus.INVALID);
    }

    @Test
    @DisplayName("JWT 형식이 아닌 문자열은 INVALID")
    void non_jwt_string_returns_invalid() {

        JwtProvider jwtProvider = newProvider();

        assertThat(jwtProvider.validateToken("this-is-not-a-jwt-token")).isEqualTo(TokenStatus.INVALID);
    }
}
