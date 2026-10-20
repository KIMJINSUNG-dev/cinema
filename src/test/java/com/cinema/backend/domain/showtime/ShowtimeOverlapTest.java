/***
 * 상영시각 겹침 검증 테스트를 담당하는 클래스
 * 작성이력
 * -2026-10-20: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.movie.MovieService;
import com.cinema.backend.domain.screen.ScreenService;
import com.cinema.backend.domain.showtime.dto.ShowtimeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ShowtimeOverlapTest {

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private ScreenService screenService;

    private Long movieId;
    private Long screenAId;
    private Long screenBId;
    private Long existingShowtimeId;

    @BeforeEach
    void setup() {

        // 상영시간 120분 영화
        movieId = movieService.createMovie(
                "테스트 영화",
                null,
                null,
                null,
                120
        ).getId();

        screenAId = createActiveScreen("테스트 A관");
        screenBId = createActiveScreen("테스트 B관");

        // 기존 일정: A관 13:00 ~ 15:00
        existingShowtimeId = showtimeService.createShowtime(
                movieId,
                screenAId,
                at(13, 0)
        ).getId();
    }

    @Test
    @DisplayName("앞쪽 경계: 10:45 시작(12:45 종료 + 여유 15분 = 13:00)은 등록된다")
    void frontBoundary_allowed() {

        ShowtimeResponse response = showtimeService.createShowtime(
                movieId,
                screenAId,
                at(10, 45));

        assertThat(response.getId()).isNotNull();
    }

    @Test
    @DisplayName("앞쪽 경계 1분 초과: 10:46 시작은 거부된다")
    void frontBoundaryPlusOne_rejected() {

        assertThatThrownBy(() -> showtimeService.createShowtime(
                movieId,
                screenAId,
                at(10, 46)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("겹칩니다");
    }

    @Test
    @DisplayName("뒤쪽 경계: 15:15 시작(15:00 종료 + 여유 15분)은 등록된다")
    void rearBoundary_allowed() {

        ShowtimeResponse response = showtimeService.createShowtime(
                movieId,
                screenAId,
                at(15, 15)
        );

        assertThat(response.getId()).isNotNull();
    }

    @Test
    @DisplayName("뒤쪽 경계 1분 미달: 15:14 시작은 거부된다")
    void rearBoundaryMinusOne_rejected() {

        assertThatThrownBy(() -> showtimeService.createShowtime(
                movieId,
                screenAId,
                at(15, 14)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("겹칩니다");
    }

    @Test
    @DisplayName("완전히 겹침: 14:00 시작은 거부된다")
    void fullOverlap_rejected() {

        assertThatThrownBy(() -> showtimeService.createShowtime(
                movieId,
                screenAId,
                at(14, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("겹칩니다");
    }

    @Test
    @DisplayName("다른 상영관: B관의 같은 시각(13:00)은 등록된다")
    void otherScreen_allowed() {

        ShowtimeResponse response = showtimeService.createShowtime(
                movieId,
                screenBId,
                at(13, 0)
        );

        assertThat(response.getId()).isNotNull();
    }

    @Test
    @DisplayName("수정: 기존 일정을 13:05로 옮길 때 자기 자신과는 겹침으로 보지 않는다")
    void updateSelf_allowed() {

        ShowtimeResponse response = showtimeService.updateSchedule(
                existingShowtimeId,
                at(13, 5)
        );

        assertThat(response.getStartTime()).isEqualTo(at(13, 5));
    }

    // 테스트용 상영관 생성: 생성 시 CLOSED이므로 ACTIVE로 변경
    private Long createActiveScreen(String name) {

        Long id = screenService.createScreen(name).getId();
        screenService.updateScreen(id, name, "ACTIVE");
        return id;
    }

    // 2099-01-01 기준 시각 (엔티티가 과거 시각을 거부하므로 먼 미래 날짜 사용)
    private LocalDateTime at(int hour, int minute) {

        return LocalDateTime.of(2099, 1, 1, hour, minute);
    }
}
