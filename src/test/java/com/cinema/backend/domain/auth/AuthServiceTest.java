/***
 * AuthService.java의 로그인 · 토큰 재발급 실패 응답(D-53)을 검증하는 단위 테스트(Spring · DB 없이 실행)를 담당하는 클래스
 * [AI 초안] 인증 실패 응답(D-53) 테스트 작성
 * - MemberRepository · RateLimitService · RefreshTokenService · PasswordEncoder는 Mockito mock()으로 대체
 * - JwtProvider는 실제 객체를 사용해 토큰 발급 · 검증 로직까지 함께 검증함. 비밀 키는 테스트 실행마다 무작위 바이트로 생성
 * 작성이력
 * -2026-11-11: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.auth;

import com.cinema.backend.domain.auth.dto.LoginResponse;
import com.cinema.backend.domain.member.Member;
import com.cinema.backend.domain.member.MemberRepository;
import com.cinema.backend.global.exception.TooManyRequestsException;
import com.cinema.backend.global.exception.UnauthorizedException;
import com.cinema.backend.global.security.JwtProvider;
import com.cinema.backend.global.security.RateLimitService;
import com.cinema.backend.global.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final long ACCESS_EXPIRATION = 1000L * 60 * 30;
    private static final long REFRESH_EXPIRATION = 1000L * 60 * 60 * 24 * 7;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD = "password";
    private static final String DEVICE_ID = "device-1";

    // AuthService의 고정 메시지와 같은 문구(가입 여부 · 실패 사유를 알려 주지 않기 위함, D-53)
    private static final String LOGIN_FAILED_MESSAGE = "아이디 또는 비밀번호가 맞지 않습니다.";
    private static final String TOKEN_REISSUE_FAILED_MESSAGE = "토큰 재발급에 실패했습니다.";

    private MemberRepository memberRepository;
    private RateLimitService rateLimitService;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;
    private String jwtSecret;
    private JwtProvider jwtProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {

        memberRepository = mock(MemberRepository.class);
        rateLimitService = mock(RateLimitService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        refreshTokenService = mock(RefreshTokenService.class);

        // 비밀 키를 필드로 저장해, 만료된 토큰 테스트에서 같은 키로 서명하되 만료 시간만 다른 JwtProvider를 만드는 데 사용
        jwtSecret = randomSecret();
        jwtProvider = new JwtProvider(jwtSecret, ACCESS_EXPIRATION, REFRESH_EXPIRATION);

        authService = new AuthService(memberRepository, rateLimitService, passwordEncoder, jwtProvider, refreshTokenService);
    }

    // HS256은 256비트(32바이트) 이상의 키가 필요하므로, Base64로 인코딩한 32바이트 무작위 값(UTF-8 기준 44바이트)을 비밀 키로 사용
    private String randomSecret() {

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
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

    private Member activeMember() {

        Member member = mock(Member.class);
        when(member.getIsWithdrawn()).thenReturn(false);
        when(member.getPassword()).thenReturn("encoded-password");
        when(member.getRole()).thenReturn(Member.Role.CUSTOMER);
        when(member.getNickname()).thenReturn("tester");
        return member;
    }

    @Test
    @DisplayName("로그인 - 시도 횟수 초과 시 TooManyRequestsException")
    void login_blocked_rejected() {

        when(rateLimitService.isBlocked(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, DEVICE_ID))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("로그인 시도 횟수가 초과되었습니다.");
    }

    @Test
    @DisplayName("로그인 - 회원 없음은 UnauthorizedException, 비밀번호 불일치와 같은 메시지")
    void login_member_not_found_rejected() {

        when(rateLimitService.isBlocked(EMAIL)).thenReturn(false);
        when(memberRepository.findByEmail(EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, DEVICE_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(LOGIN_FAILED_MESSAGE);
    }

    @Test
    @DisplayName("로그인 - 탈퇴 회원은 UnauthorizedException, 비밀번호 불일치와 같은 메시지")
    void login_withdrawn_member_rejected() {

        Member member = mock(Member.class);
        when(member.getIsWithdrawn()).thenReturn(true);

        when(rateLimitService.isBlocked(EMAIL)).thenReturn(false);
        when(memberRepository.findByEmail(EMAIL)).thenReturn(member);

        assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, DEVICE_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(LOGIN_FAILED_MESSAGE);
    }

    @Test
    @DisplayName("로그인 - 비밀번호 불일치는 UnauthorizedException과 실패 기록, 회원 없음과 같은 메시지")
    void login_wrong_password_rejected() {

        Member member = activeMember();

        when(rateLimitService.isBlocked(EMAIL)).thenReturn(false);
        when(memberRepository.findByEmail(EMAIL)).thenReturn(member);
        when(passwordEncoder.matches(PASSWORD, member.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, DEVICE_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(LOGIN_FAILED_MESSAGE);

        verify(rateLimitService).recordFailure(EMAIL);
    }

    @Test
    @DisplayName("로그인 - 성공 시 토큰 반환, 실패 기록 초기화, Refresh Token 저장")
    void login_success() {

        Member member = activeMember();

        when(rateLimitService.isBlocked(EMAIL)).thenReturn(false);
        when(memberRepository.findByEmail(EMAIL)).thenReturn(member);
        when(passwordEncoder.matches(PASSWORD, member.getPassword())).thenReturn(true);

        LoginResponse response = authService.login(EMAIL, PASSWORD, DEVICE_ID);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getNickname()).isEqualTo("tester");
        assertThat(jwtProvider.getEmailFromToken(response.getAccessToken())).isEqualTo(EMAIL);

        verify(rateLimitService).clearFailures(EMAIL);
        verify(refreshTokenService).save(EMAIL, DEVICE_ID, response.getRefreshToken());
    }

    @Test
    @DisplayName("재발급 - 정상 Refresh Token이면 새 Access Token 반환")
    void reissue_success() {

        Member member = activeMember();
        String refreshToken = jwtProvider.generateRefreshToken(EMAIL);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(member);
        when(refreshTokenService.find(EMAIL, DEVICE_ID)).thenReturn(refreshToken);

        String newAccessToken = authService.reissueAccessToken(refreshToken, DEVICE_ID);

        assertThat(newAccessToken).isNotBlank();
        assertThat(jwtProvider.getEmailFromToken(newAccessToken)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("재발급 - 만료된 Refresh Token은 UnauthorizedException, 회원 조회 미호출")
    void reissue_expired_refresh_token_rejected() {

        // 같은 비밀 키로 서명하되 만료 시간만 음수로 준 JwtProvider로 만료된 토큰을 생성
        JwtProvider expiredTokenProvider = new JwtProvider(jwtSecret, -1, -1);
        String expiredRefreshToken = expiredTokenProvider.generateRefreshToken(EMAIL);

        assertThatThrownBy(() -> authService.reissueAccessToken(expiredRefreshToken, DEVICE_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(TOKEN_REISSUE_FAILED_MESSAGE);

        // AuthService는 토큰 검증 → 이메일 추출 순서로 동작한다(D-53 구현 메모).
        // 검증에서 거부되면 이메일을 추출하지 않으므로 회원 조회(findByEmail)가 전혀 호출되지 않아야 한다.
        verify(memberRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("재발급 - 서명이 변조된 Refresh Token은 UnauthorizedException, 회원 조회 미호출")
    void reissue_tampered_signature_refresh_token_rejected() {

        String refreshToken = jwtProvider.generateRefreshToken(EMAIL);
        String tamperedToken = tamperSignature(refreshToken);

        assertThatThrownBy(() -> authService.reissueAccessToken(tamperedToken, DEVICE_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(TOKEN_REISSUE_FAILED_MESSAGE);

        // 위와 같은 이유로(검증 → 이메일 추출 순서) 회원 조회가 호출되지 않아야 한다.
        verify(memberRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("재발급 - 회원 없음은 UnauthorizedException, 고정 메시지")
    void reissue_member_not_found_rejected() {

        String refreshToken = jwtProvider.generateRefreshToken(EMAIL);
        when(memberRepository.findByEmail(EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> authService.reissueAccessToken(refreshToken, DEVICE_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(TOKEN_REISSUE_FAILED_MESSAGE);
    }

    @Test
    @DisplayName("재발급 - 탈퇴 회원은 UnauthorizedException, 고정 메시지")
    void reissue_withdrawn_member_rejected() {

        Member member = mock(Member.class);
        when(member.getIsWithdrawn()).thenReturn(true);

        String refreshToken = jwtProvider.generateRefreshToken(EMAIL);
        when(memberRepository.findByEmail(EMAIL)).thenReturn(member);

        assertThatThrownBy(() -> authService.reissueAccessToken(refreshToken, DEVICE_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(TOKEN_REISSUE_FAILED_MESSAGE);
    }

    @Test
    @DisplayName("재발급 - 저장된 토큰이 없으면 UnauthorizedException, 고정 메시지")
    void reissue_no_stored_token_rejected() {

        Member member = activeMember();
        String refreshToken = jwtProvider.generateRefreshToken(EMAIL);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(member);
        when(refreshTokenService.find(EMAIL, DEVICE_ID)).thenReturn(null);

        assertThatThrownBy(() -> authService.reissueAccessToken(refreshToken, DEVICE_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(TOKEN_REISSUE_FAILED_MESSAGE);
    }

    @Test
    @DisplayName("재발급 - 저장된 토큰과 불일치하면 UnauthorizedException, 고정 메시지")
    void reissue_token_mismatch_rejected() {

        Member member = activeMember();
        String refreshToken = jwtProvider.generateRefreshToken(EMAIL);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(member);
        when(refreshTokenService.find(EMAIL, DEVICE_ID)).thenReturn("stored-different-token");

        assertThatThrownBy(() -> authService.reissueAccessToken(refreshToken, DEVICE_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(TOKEN_REISSUE_FAILED_MESSAGE);
    }
}
