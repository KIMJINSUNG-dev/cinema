/***
 * ShowtimeService.findTimetable()(QueryDSL 기반 고객용 상영시간표 조회,
 * ShowtimeRepositoryCustomImpl.findTimetable() 사용) 통합 테스트를 담당하는 클래스
 * 작성이력
 * -2026-11-04: 파일 생성 및 작성 (MovieSearchTest.java · ShowtimeChangeRestrictionTest.java 구조 참고)
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.movie.MovieService;
import com.cinema.backend.domain.screen.ScreenService;
import com.cinema.backend.domain.showtime.dto.ShowtimeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ShowtimeTimetableTest {

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieService movieService;

    @Autowired
    private ScreenService screenService;

    @Test
    @DisplayName("date만 조건: 그날의 여러 영화 일정이 영화 이름순 → 시작 시각순으로 조회된다")
    void dateOnly_sortedByMovieNameThenStartTime() {

        Long movieAId = createActiveMovie("TMT1 A영화");
        Long movieBId = createActiveMovie("TMT1 B영화");
        Long screen1Id = createActiveScreen("TMT1 1관");
        Long screen2Id = createActiveScreen("TMT1 2관");
        Long screen3Id = createActiveScreen("TMT1 3관");

        // A영화를 B영화보다 늦은 시각에 상영해, 이름순이 시작 시각순보다 먼저 적용되는지 확인한다.
        Long showtimeA1 = createActiveShowtime(movieAId, screen1Id, at(1, 8, 0));
        Long showtimeA2 = createActiveShowtime(movieAId, screen2Id, at(1, 20, 0));
        Long showtimeB1 = createActiveShowtime(movieBId, screen3Id, at(1, 9, 0));

        List<ShowtimeResponse> result = showtimeService.findTimetable(date(1), null, null);

        assertThat(result)
                .extracting(ShowtimeResponse::getId)
                .containsExactly(showtimeA1, showtimeA2, showtimeB1);
    }

    @Test
    @DisplayName("movieId + date 조건: 해당 영화의 일정만 조회된다")
    void movieIdAndDate_filtersOnlyMatchingMovie() {

        Long movieAId = createActiveMovie("TMT2 A영화");
        Long movieBId = createActiveMovie("TMT2 B영화");
        Long screen1Id = createActiveScreen("TMT2 1관");
        Long screen2Id = createActiveScreen("TMT2 2관");

        Long showtimeAId = createActiveShowtime(movieAId, screen1Id, at(2, 10, 0));
        createActiveShowtime(movieBId, screen2Id, at(2, 11, 0));

        List<ShowtimeResponse> result = showtimeService.findTimetable(date(2), movieAId, null);

        assertThat(result)
                .extracting(ShowtimeResponse::getId)
                .containsExactly(showtimeAId);
    }

    @Test
    @DisplayName("screenId + date 조건: 해당 상영관의 일정만 조회된다")
    void screenIdAndDate_filtersOnlyMatchingScreen() {

        Long movieId = createActiveMovie("TMT3 영화");
        Long screen1Id = createActiveScreen("TMT3 1관");
        Long screen2Id = createActiveScreen("TMT3 2관");

        Long showtime1Id = createActiveShowtime(movieId, screen1Id, at(3, 10, 0));
        createActiveShowtime(movieId, screen2Id, at(3, 11, 0));

        List<ShowtimeResponse> result = showtimeService.findTimetable(date(3), null, screen1Id);

        assertThat(result)
                .extracting(ShowtimeResponse::getId)
                .containsExactly(showtime1Id);
    }

    @Test
    @DisplayName("DEACTIVATED 상영 일정은 제외된다")
    void deactivatedShowtime_excluded() {

        Long movieId = createActiveMovie("TMT4 영화");
        Long screenId = createActiveScreen("TMT4 1관");

        // 활성화하지 않아 기본값(DEACTIVATED)을 유지
        showtimeService.createShowtime(movieId, screenId, at(4, 10, 0));

        List<ShowtimeResponse> result = showtimeService.findTimetable(date(4), null, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("비공개(DEACTIVATED) 영화의 상영 일정은 제외된다")
    void movieDeactivatedAfterActivation_excluded() {

        Long movieId = createActiveMovie("TMT5 영화");
        Long screenId = createActiveScreen("TMT5 1관");

        Long showtimeId = createActiveShowtime(movieId, screenId, at(5, 10, 0));
        // 일정을 활성화한 뒤 영화를 비공개로 전환 (일정 상태는 그대로 ACTIVE)
        movieService.updateMovie(movieId, "TMT5 영화", null, "SHOWING", "DEACTIVATED", null, null, 120);

        List<ShowtimeResponse> result = showtimeService.findTimetable(date(5), null, null);

        assertThat(result)
                .extracting(ShowtimeResponse::getId)
                .doesNotContain(showtimeId);
    }

    @Test
    @DisplayName("이미 시작한 일정은 제외된다 (리포지토리에 now를 일정 시작 시각 이후로 넘겨 검증)")
    void alreadyStartedShowtime_excludedByNow() {

        Long movieId = createActiveMovie("TMT6 영화");
        Long screenId = createActiveScreen("TMT6 1관");

        Long showtimeId = createActiveShowtime(movieId, screenId, at(6, 10, 0));

        // 서비스로는 과거 일정을 만들 수 없으므로, 리포지토리를 직접 호출해 now를
        // 일정 시작 시각 이후로 넘겨 "이미 시작한 일정"을 재현한다.
        List<Showtime> excluded = showtimeRepository.findTimetable(
                date(6), null, null, at(6, 10, 1));

        assertThat(excluded)
                .extracting(Showtime::getId)
                .doesNotContain(showtimeId);

        // 대조 검증: now가 시작 시각 1분 전이면(아직 시작 전) 포함된다.
        // 포함 · 제외가 now와 시작 시각의 관계로만 갈리는 것을 증명한다.
        List<Showtime> included = showtimeRepository.findTimetable(
                date(6), null, null, at(6, 9, 59));

        assertThat(included)
                .extracting(Showtime::getId)
                .contains(showtimeId);
    }

    @Test
    @DisplayName("날짜 경계: date 00:00 · 23:30 시작 일정은 포함되고, date+1일 00:00 시작 일정은 제외된다")
    void dateBoundary_includesStartAndEndOfDay_excludesNextDay() {

        Long movieId = createActiveMovie("TMT7 영화");
        Long screen1Id = createActiveScreen("TMT7 1관");
        Long screen2Id = createActiveScreen("TMT7 2관");
        Long screen3Id = createActiveScreen("TMT7 3관");

        Long startOfDayId = createActiveShowtime(movieId, screen1Id, at(7, 0, 0));
        Long endOfDayId = createActiveShowtime(movieId, screen2Id, at(7, 23, 30));
        Long nextDayId = createActiveShowtime(movieId, screen3Id, at(8, 0, 0));

        List<ShowtimeResponse> result = showtimeService.findTimetable(date(7), null, null);

        assertThat(result)
                .extracting(ShowtimeResponse::getId)
                .containsExactly(startOfDayId, endOfDayId)
                .doesNotContain(nextDayId);
    }

    @Test
    @DisplayName("존재하지 않는 movieId로 조회하면 빈 목록이 반환된다")
    void nonExistentMovieId_emptyList() {

        List<ShowtimeResponse> result = showtimeService.findTimetable(date(8), 999_999_999L, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("date가 null이면 IllegalArgumentException이 발생한다")
    void nullDate_rejected() {

        assertThatThrownBy(() -> showtimeService.findTimetable(null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("조회 날짜는 비워둘 수 없습니다.");
    }

    // 영화: 공개(ACTIVE) · 상영 중 (ShowtimeChangeRestrictionTest와 같은 준비 방식)
    private Long createActiveMovie(String name) {

        Long id = movieService.createMovie(name, null, null, null, 120).getId();
        movieService.updateMovie(id, name, null, "SHOWING", "ACTIVE", null, null, 120);
        return id;
    }

    // 테스트용 상영관 생성: 생성 시 CLOSED이므로 ACTIVE로 변경 (ShowtimeOverlapTest와 같은 방식)
    private Long createActiveScreen(String name) {

        Long id = screenService.createScreen(name).getId();
        screenService.updateScreen(id, name, "ACTIVE");
        return id;
    }

    private Long createActiveShowtime(Long movieId, Long screenId, LocalDateTime startTime) {

        Long id = showtimeService.createShowtime(movieId, screenId, startTime).getId();
        showtimeService.updateShowtimeStatus(id, "ACTIVE");
        return id;
    }

    // 2099년 기준 날짜 (엔티티가 과거 시각을 거부하므로 먼 미래 날짜 사용, day로 일정 간 겹침을 피함)
    private LocalDate date(int day) {

        return LocalDate.of(2099, 1, day);
    }

    private LocalDateTime at(int day, int hour, int minute) {

        return LocalDateTime.of(2099, 1, day, hour, minute);
    }
}
