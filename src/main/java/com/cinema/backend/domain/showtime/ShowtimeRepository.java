/***
 * Showtime.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-10-14: existsByScreenAndStartTimeLessThanAndEndTimeGreaterThan(),
 *              existsByScreenAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan() 메서드 추가
 * -2026-10-15: findByScreenOrderByStartTimeAsc() 메서드 추가 (상영관별 목록 조회, 시작 시각 오름차순)
 * -2026-10-28: 상영 일정 비관적 락 적용을 위한 findByIdForUpdate() 메서드 추가
 * -2026-11-04: ShowtimeRepositoryCustom 인터페이스 상속 (상영시간표 조회, 영화 · 상영관 fetch join)
 * -2026-11-04: findByScreenOrderByStartTimeAsc()의 N+1(영화 지연 로딩)을 줄이기 위해
 *              영화를 join fetch로 함께 조회하도록 변경 (상영관은 호출부에서 이미 조회해
 *              영속성 컨텍스트에 있으므로 fetch join 대상에서 제외)
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.domain.screen.Screen;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long>, ShowtimeRepositoryCustom {

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

    @Query("select s from Showtime s join fetch s.movie where s.screen = :screen order by s.startTime asc")
    List<Showtime> findByScreenOrderByStartTimeAsc(@Param("screen") Screen screen);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Showtime s where s.id = :id")
    Optional<Showtime> findByIdForUpdate(@Param("id") Long id);
}
