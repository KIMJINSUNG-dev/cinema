/***
 * 프론트엔드의 API 요청에 따라 ShowtimeService.java의 서비스 메서드를 호출하는 컨트롤러 클래스
 * 작성이력
 * -2026-10-15: 파일 생성 및 작성 (SeatGradeController.java 구조 참고)
 *
 * 상영관별 목록 조회는 screenId를 필수 쿼리 파라미터로 받는다.
 * (상영관 하나를 지정하지 않은 전체 조회는 현재 범위에 없음)
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.domain.showtime.dto.ShowtimeCreateRequest;
import com.cinema.backend.domain.showtime.dto.ShowtimeResponse;
import com.cinema.backend.domain.showtime.dto.ShowtimeStatusUpdateRequest;
import com.cinema.backend.domain.showtime.dto.ShowtimeUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @PostMapping
    public ResponseEntity<ShowtimeResponse> createShowtime(
            @RequestBody ShowtimeCreateRequest request
    ) {

        ShowtimeResponse response = showtimeService.createShowtime(
                request.getMovieId(),
                request.getScreenId(),
                request.getStartTime()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByScreen(
            @RequestParam Long screenId
    ) {

        return ResponseEntity.ok(showtimeService.findByScreenId(screenId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> getShowtime(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(showtimeService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> updateShowtime(
            @PathVariable Long id,
            @RequestBody ShowtimeUpdateRequest request
    ) {

        return ResponseEntity.ok(showtimeService.updateSchedule(id, request.getStartTime()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ShowtimeResponse> updateShowtimeStatus(
            @PathVariable Long id,
            @RequestBody ShowtimeStatusUpdateRequest request
    ) {

        return ResponseEntity.ok(showtimeService.updateShowtimeStatus(id, request.getShowtimeStatus()));
    }
}
