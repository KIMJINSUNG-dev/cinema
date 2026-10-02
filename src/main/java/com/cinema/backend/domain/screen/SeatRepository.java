/***
 * Seat.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-09-15: findSeatByRowAndColumnAndScreen(), findByScreen() 메서드 추가
 ***/
package com.cinema.backend.domain.screen;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    Seat findSeatByRowAndColumnAndScreen(int row, int column, Screen screen);
    List<Seat> findByScreen(Screen screen);
}
