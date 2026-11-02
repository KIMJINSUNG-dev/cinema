/***
 * Redis에서의 RefreshToken 저장/조회/삭제를 담당하는 클래스
 * 작성이력
 * -2026-09-11: 파일 생성 및 작성
 * -2026-11-02: redisTemplate.opsForValue().set() API 메서드 deprecated 문제로 새 구조로 대체
 *              기존 구조(deprecated):
 *              opsForValue()
 *                  .set(
 *                      buildKey(email, deviceId),
 *                      refreshToken,
 *                      refreshExpiration,
 *                      TimeUnit.MILLISECONDS
 *                  )
 *              새 구조:
 *              opsForValue()
 *                  .set(
 *                      buildKey(email, deviceId),
 *                      refreshToken,
 *                      Duration.ofMillis(refreshExpiration)
 *                  )
 ***/
package com.cinema.backend.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;


    private String buildKey(String email, String deviceId) {

        return "refresh:" + email + ":" + deviceId;
    }

    public void save(String email, String deviceId, String refreshToken) {

        redisTemplate.opsForValue().set(
                buildKey(email, deviceId),
                refreshToken,
                Duration.ofMillis(refreshExpiration)
        );
    }

    public String find(String email, String deviceId) {

        return redisTemplate.opsForValue().get(buildKey(email, deviceId));
    }

    public void delete(String email, String deviceId) {

        redisTemplate.delete(buildKey(email, deviceId));
    }
}