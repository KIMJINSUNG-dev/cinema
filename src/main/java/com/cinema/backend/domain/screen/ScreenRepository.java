/***
 * Screen.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenRepository extends JpaRepository<Screen, Long> {


}
