/***
 * 로그인 등 인증 관련 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-09-11: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.auth;

import com.cinema.backend.domain.auth.dto.LoginResponse;
import com.cinema.backend.domain.member.Member;
import com.cinema.backend.domain.member.MemberRepository;
import com.cinema.backend.global.security.JwtProvider;
import com.cinema.backend.global.security.RateLimitService;
import com.cinema.backend.global.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RateLimitService rateLimitService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResponse login(String email, String password, String deviceId) {

        if (rateLimitService.isBlocked(email)) {

            throw new IllegalArgumentException("로그인 시도 횟수가 초과되었습니다.");
        }

        Member member = memberRepository.findByEmail(email);

        if (member == null || member.getIsWithdrawn()) {

            throw new IllegalArgumentException("아이디 또는 비밀번호가 맞지 않습니다.");
        }

        if (!passwordEncoder.matches(password, member.getPassword())) {

            rateLimitService.recordFailure(email);
            throw new IllegalArgumentException("아이디 또는 비밀번호가 맞지 않습니다.");
        }

        rateLimitService.clearFailures(email);
        String accessToken = jwtProvider.generateAccessToken(email, member.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(email);

        refreshTokenService.save(email, deviceId, refreshToken);
        return new LoginResponse(accessToken, refreshToken, member.getNickname());
    }

    @Transactional
    public String reissueAccessToken(String refreshToken, String deviceId) {

        if (!jwtProvider.validateToken(refreshToken)) {

            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        String email = jwtProvider.getEmailFromToken(refreshToken);

        Member member = memberRepository.findByEmail(email);
        if (member == null || member.getIsWithdrawn()) {

            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        String storedToken = refreshTokenService.find(email, deviceId);

        if (storedToken == null) {

            throw new IllegalArgumentException("Refresh Token이 존재하지 않습니다.");
        }

        if (!storedToken.equals(refreshToken)) {

            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }

        return jwtProvider.generateAccessToken(email, member.getRole().name());
    }

    @Transactional
    public void logout(String email, String deviceId) {

        refreshTokenService.delete(email, deviceId);
    }
}
