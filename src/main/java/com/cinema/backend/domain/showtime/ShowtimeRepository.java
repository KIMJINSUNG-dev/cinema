/***
 * Showtime.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-10-14: existsByScreenAndStartTimeLessThanAndEndTimeGreaterThan(),
 *              existsByScreenAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan() 메서드 추가
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.domain.screen.Screen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    boolean existsByScreenAndStartTimeLessThanAndEndTimeGreaterThan(
            Screen screen,
            LocalDateTime newEndTime,
            LocalDateTime newStartTime
    );
    boolean existsByScreenAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
            Screen screen,
            Long id,
            LocalDateTime newEndTime,
            LocalDateTime newStartTime
    );
}
