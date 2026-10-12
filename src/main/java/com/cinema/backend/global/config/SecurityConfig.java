/***
 * 보안 관련 설정을 담당하는 Configuration 클래스
 * 작성이력
 * -2026-09-14: 파일 생성 및 작성
 * -2026-10-07: SeatController 작성에 따른 filterChain() 메서드에의 권한 체계 추가
 * -2026-10-12: SeatGradeController 작성에 따른 filterChain() 메서드에의 권한 체계 추가
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
                                HttpMethod.POST,
                                "/api/members",
                                "/api/auths/login",
                                "/api/auths/reissue"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/movies",
                                "/api/movies/*"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/movies/**"
                        ).hasAnyRole("MANAGER")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/movies/**"
                        ).hasAnyRole("MANAGER")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/screens/**"
                        ).hasAnyRole("MANAGER")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/screens"
                        ).hasAnyRole("STAFF")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/screens/*"
                        ).hasAnyRole("STAFF")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/screens/*/seats"
                        ).hasAnyRole("STAFF")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/screens/*/seats/*"
                        ).hasAnyRole("STAFF")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/screens/**"
                        ).hasAnyRole("MANAGER")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/seat-grades"
                        ).hasAnyRole("MANAGER")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/seat-grades",
                                "/api/seat-grades/*"
                        ).hasAnyRole("STAFF")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/seat-grades/*"
                        ).hasAnyRole("MANAGER")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/members/by-email/*",
                                "/api/members/by-nickname/*"
                        ).hasAnyRole("STAFF")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/members/myinfo"
                        ).hasAnyRole("CUSTOMER")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/members/*"
                        ).hasAnyRole("STAFF")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/members/**"
                        ).hasAnyRole("CUSTOMER")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auths/logout"
                        ).hasAnyRole("CUSTOMER")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/members/*"
                        ).hasAnyRole("ADMIN")
                        .anyRequest().authenticated()
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
