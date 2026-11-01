/***
 * Screen.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-11-01: 상영관 잠금 조회 메서드 findByIdForUpdate 추가
 ***/
package com.cinema.backend.domain.screen;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScreenRepository extends JpaRepository<Screen, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Screen s where s.id = :id")
    Optional<Screen> findByIdForUpdate(@Param("id") Long id);
}
