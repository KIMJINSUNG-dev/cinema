/***
 * 프론트엔드의 API 요청에 따라 SeatService.java의 서비스 메서드를 호출하는 컨트롤러 클래스
 * 작성이력
 * -2026-09-16: 파일 생성 및 작성
 * -2026-10-07: @RequestMapping URL 변경("/api/seats" => "/api/screens/{screenId}/seats")
 * -2026-10-07: 개별 API 메서드 작성
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.domain.screen.dto.SeatCreateRequest;
import com.cinema.backend.domain.screen.dto.SeatResponse;
import com.cinema.backend.domain.screen.dto.SeatUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screens/{screenId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping
    public ResponseEntity<SeatResponse> createSeat(
            @PathVariable Long screenId,
            @RequestBody SeatCreateRequest request
    ) {

        SeatResponse seatResponse = seatService.createSeat(
                screenId,
                request.getSeatGradeId(),
                request.getRow(),
                request.getColumn(),
                request.getSeatStatus()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(seatResponse);
    }

    @GetMapping
    public ResponseEntity<List<SeatResponse>> getSeats(
            @PathVariable Long screenId
    ) {

        return ResponseEntity.ok(seatService.findByScreenId(screenId));
    }

    @GetMapping("/{seatId}")
    public ResponseEntity<SeatResponse> getSeat(
            @PathVariable Long screenId,
            @PathVariable Long seatId
    ) {

        return ResponseEntity.ok(seatService.findById(screenId, seatId));
    }

    @PutMapping("/{seatId}")
    public ResponseEntity<SeatResponse> updateSeat(
            @PathVariable Long screenId,
            @PathVariable Long seatId,
            @RequestBody SeatUpdateRequest request
    ) {

        return ResponseEntity.ok(seatService.updateSeat(
                screenId,
                seatId,
                request.getRow(),
                request.getColumn(),
                request.getSeatGradeId(),
                request.getSeatStatus()
        ));
    }
}
