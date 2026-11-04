/***
 * ShowtimeRepository.java 리포지토리에서 사용할 커스텀 메서드 구성을 담당하는 인터페이스
 * 작성이력
 * -2026-11-04: 파일 생성 및 작성 (MovieRepositoryCustom.java 구조 참고)
 ***/
package com.cinema.backend.domain.showtime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeRepositoryCustom {

    List<Showtime> findTimetable(LocalDate date, Long movieId, Long screenId, LocalDateTime now);
}
