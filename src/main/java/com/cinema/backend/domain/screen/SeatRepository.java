/***
 * Seat.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-09-15: findSeatByRowAndColumnAndScreen(), findByScreen() 메서드 추가
 * -2026-11-11: findAllWithGradeByScreenId() 메서드 추가 (고객용 좌석 조회 API 제공을 위한 메서드)
 ***/
package com.cinema.backend.domain.screen;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    Seat findSeatByRowAndColumnAndScreen(int row, int column, Screen screen);
    List<Seat> findByScreen(Screen screen);

    @Query("select s from Seat s join fetch s.seatGrade where s.screen.id = :screenId order by s.row asc, s.column asc")
    List<Seat> findAllWithGradeByScreenId(@Param("screenId") Long screenId);
}
