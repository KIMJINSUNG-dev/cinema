/***
 * 로그인 등 인증 관련 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-09-11: 파일 생성 및 작성
 * -2026-11-09: 예외 처리 방식을 TooManyRequestsException / UnauthorizedException 기반으로 변경
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
import com.cinema.backend.global.security.TokenStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final MemberRepository memberRepository;
    private final RateLimitService rateLimitService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    private static final String TOKEN_REISSUE_FAILED_MESSAGE = "토큰 재발급에 실패했습니다.";

    @Transactional
    public LoginResponse login(String email, String password, String deviceId) {

        if (rateLimitService.isBlocked(email)) {

            throw new TooManyRequestsException("로그인 시도 횟수가 초과되었습니다.");
        }

        Member member = memberRepository.findByEmail(email);

        if (member == null || member.getIsWithdrawn()) {

            throw new UnauthorizedException("아이디 또는 비밀번호가 맞지 않습니다.");
        }

        if (!passwordEncoder.matches(password, member.getPassword())) {

            rateLimitService.recordFailure(email);
            throw new UnauthorizedException("아이디 또는 비밀번호가 맞지 않습니다.");
        }

        rateLimitService.clearFailures(email);
        String accessToken = jwtProvider.generateAccessToken(email, member.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(email);

        refreshTokenService.save(email, deviceId, refreshToken);
        return new LoginResponse(accessToken, refreshToken, member.getNickname());
    }

    @Transactional
    public String reissueAccessToken(String refreshToken, String deviceId) {

        TokenStatus validation = jwtProvider.validateToken(refreshToken);

        if (validation == TokenStatus.EXPIRED) {

            log.info("만료된 토큰입니다: deviceId={}", deviceId);
            throw new UnauthorizedException(TOKEN_REISSUE_FAILED_MESSAGE);
        }

        if (validation == TokenStatus.INVALID) {

            log.warn("잘못된 토큰입니다: deviceId={}", deviceId);
            throw new UnauthorizedException(TOKEN_REISSUE_FAILED_MESSAGE);
        }

        String email = jwtProvider.getEmailFromToken(refreshToken);
        Member member = memberRepository.findByEmail(email);

        if (member == null || member.getIsWithdrawn()) {

            log.warn("존재하지 않는 사용자입니다: email={}, deviceId={}", email, deviceId);
            throw new UnauthorizedException(TOKEN_REISSUE_FAILED_MESSAGE);
        }

        String storedToken = refreshTokenService.find(email, deviceId);

        if (storedToken == null) {

            log.info("Refresh Token이 존재하지 않습니다: email={}, deviceId={}", email, deviceId);
            throw new UnauthorizedException(TOKEN_REISSUE_FAILED_MESSAGE);
        }

        if (!storedToken.equals(refreshToken)) {

            log.warn("Refresh Token이 일치하지 않습니다: email={}, deviceId={}", email, deviceId);
            throw new UnauthorizedException(TOKEN_REISSUE_FAILED_MESSAGE);
        }

        return jwtProvider.generateAccessToken(email, member.getRole().name());
    }

    @Transactional
    public void logout(String email, String deviceId) {

        refreshTokenService.delete(email, deviceId);
    }
}
