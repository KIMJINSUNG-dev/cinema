/***
 * Redis를 활용해 로그인 시도 제한을 담당하는 클래스
 * 작성이력
 * -2026-09-10: 파일 생성 및 작성
 * -2026-11-02: redisTemplate.expire() API 메서드 deprecated 문제로 새 구조로 대체
 *              기존 구조(deprecated):
 *              expire(key, BLOCK_DURATION_MINUTES, TimeUnit.MINUTES)
 *              새 구조:
 *              expire(key, Duration.ofMinutes(BLOCK_DURATION_MINUTES))
 ***/
package com.cinema.backend.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    // 최대 허용 실패 횟수
    private static final int MAX_ATTEMPTS = 5;

    // 차단 시간 (분)
    private static final int BLOCK_DURATION_MINUTES = 5;

    private String buildKey(String email) {

        return "login-fail:" + email;
    }

    public boolean isBlocked(String email) {

        String value = redisTemplate.opsForValue().get(buildKey(email));
        if (value == null) return false;
        return Integer.parseInt(value) >= MAX_ATTEMPTS;
    }

    public void recordFailure(String email) {

        String key = buildKey(email);
        Long count = redisTemplate.opsForValue().increment(key);

        // 첫 번째 실패일 때만 TTL 설정
        if (count != null && count == 1) {

            redisTemplate.expire(key, Duration.ofMinutes(BLOCK_DURATION_MINUTES));
        }
    }

    public void clearFailures(String email) {

        redisTemplate.delete(buildKey(email));
    }
}