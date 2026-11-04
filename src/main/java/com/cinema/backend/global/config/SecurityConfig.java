/***
 * 보안 관련 설정을 담당하는 Configuration 클래스
 * 작성이력
 * -2026-09-14: 파일 생성 및 작성
 * -2026-10-07: SeatController 작성에 따른 filterChain() 메서드에의 권한 체계 추가
 * -2026-10-12: SeatGradeController 작성에 따른 filterChain() 메서드에의 권한 체계 추가
 * -2026-10-15: ShowtimeController 작성에 따른 filterChain() 메서드에의 권한 체계 추가
 * -2026-10-15: filterChain() 메서드 권한 처리를 denyAll() 기반으로 재정비
 * -2026-10-30: ReservationController 작성에 따른 filterChain() 메서드에의 권한 체계 추가
 * -2026-10-31: 예매 내역 조회 · 취소 API 권한 추가
 *              GET /api/reservations/*가 /api/reservations/me와도 일치하므로(D-34와 같은 문제),
 *              고객용 /me 규칙(GET · PATCH)을 직원용 규칙보다 위에 둠
 * -2026-10-31: 직원 예약(회원 대리 예약 · 현장 판매) API 권한 추가 (D-43)
 * -2026-11-04: 고객용 상영시간표 조회(GET /api/showtimes/timetable) permitAll 추가.
 *              GET /api/showtimes/**가 STAFF 전용이라(D-34와 같은 문제), 비로그인 허용 규칙을
 *              그보다 앞선 permitAll 블록(movies와 함께)에 둬서 먼저 매칭되게 함
 ***/
package com.cinema.backend.global.config;

import com.cinema.backend.global.security.JwtAuthenticationFilter;
import com.cinema.backend.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    @Bean
    static RoleHierarchy roleHierarchy() {

        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("MANAGER")
                .role("MANAGER").implies("STAFF")
                .role("STAFF").implies("CUSTOMER")
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/error"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/members",
                                "/api/auths/login",
                                "/api/auths/reissue"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/movies",
                                "/api/movies/*",
                                "/api/showtimes/timetable"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/members/myinfo",
                                "/api/reservations/me",
                                "/api/reservations/me/*"
                        ).hasAnyRole("CUSTOMER")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auths/logout",
                                "/api/reservations/online"
                        ).hasAnyRole("CUSTOMER")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/reservations/box-office/**"
                        ).hasAnyRole("STAFF")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/members/*/nickname",
                                "/api/members/*/withdraw",
                                "/api/reservations/me/*/cancel"
                        ).hasAnyRole("CUSTOMER")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/screens/**",
                                "/api/seat-grades/**",
                                "/api/showtimes/**",
                                "/api/members/*",
                                "/api/members/by-email/*",
                                "/api/members/by-nickname/*",
                                "/api/reservations",
                                "/api/reservations/*"
                        ).hasAnyRole("STAFF")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/reservations/*/cancel"
                        ).hasAnyRole("STAFF")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/members"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/movies",
                                "/api/screens/**",
                                "/api/seat-grades",
                                "/api/showtimes"
                        ).hasAnyRole("MANAGER")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/movies/*",
                                "/api/screens/**",
                                "/api/seat-grades/*",
                                "/api/showtimes/*"
                        ).hasAnyRole("MANAGER")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/showtimes/*/status"
                        ).hasAnyRole("MANAGER")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/members/*"
                        ).hasRole("ADMIN")
                        .anyRequest().denyAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("text/plain;charset=utf-8");
                            response.getWriter().println("인증이 필요합니다.");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("text/plain;charset=utf-8");
                            response.getWriter().println("권한이 없습니다.");
                        })
                )
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService() {

        return username -> {

            throw new UsernameNotFoundException("JWT 인증을 사용합니다.");
        };
    }
}
