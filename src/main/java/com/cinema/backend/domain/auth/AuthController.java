/***
 * 프론트엔드의 API 요청에 따라 LoginService.java의 서비스 메서드를 호출하는 컨트롤러 클래스
 * 작성이력
 * -2026-09-11: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.auth;

import com.cinema.backend.domain.auth.dto.LoginRequest;
import com.cinema.backend.domain.auth.dto.LoginResponse;
import com.cinema.backend.domain.auth.dto.LogoutRequest;
import com.cinema.backend.domain.auth.dto.ReissueRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auths")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        LoginResponse loginResponse = authService.login(
                loginRequest.getEmail(),
                loginRequest.getPassword(),
                loginRequest.getDeviceId());

        ResponseCookie cookie = ResponseCookie.from(
                "refreshToken",
                loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false) // 배포 시 true로 변경
                .sameSite("Lax")
                .path("/api/auths")
                .maxAge(Duration.ofMillis(refreshExpiration))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new LoginResponse(loginResponse.getAccessToken(), loginResponse.getNickname()));
    }

    @PostMapping("/reissue")
    public ResponseEntity<String> reissue(
            @CookieValue("refreshToken") String refreshToken,
            @RequestBody ReissueRequest request
    ) {

        return ResponseEntity.ok(
                authService.reissueAccessToken(
                        refreshToken,
                        request.getDeviceId()
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody LogoutRequest logoutRequest
    ) {

        authService.logout(logoutRequest.getEmail(), logoutRequest.getDeviceId());
        return ResponseEntity.ok().build();
    }
}
