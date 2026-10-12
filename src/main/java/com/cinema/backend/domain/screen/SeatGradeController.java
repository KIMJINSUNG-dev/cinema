/***
 * 프론트엔드의 API 요청에 따라 SeatGradeService.java의 서비스 메서드를 호출하는 컨트롤러 클래스
 * 작성이력
 * -2026-10-12: 파일 생성 및 작성 (SeatController.java 구조 참고)
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.domain.screen.dto.SeatGradeCreateRequest;
import com.cinema.backend.domain.screen.dto.SeatGradeResponse;
import com.cinema.backend.domain.screen.dto.SeatGradeUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seat-grades")
@RequiredArgsConstructor
public class SeatGradeController {

    private final SeatGradeService seatGradeService;

    @PostMapping
    public ResponseEntity<SeatGradeResponse> createSeatGrade(
            @RequestBody SeatGradeCreateRequest request
    ) {

        SeatGradeResponse response = seatGradeService.createSeatGrade(
                request.getName(),
                request.getBasePrice(),
                request.getSeatGradeStatus()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SeatGradeResponse>> getSeatGrades() {

        return ResponseEntity.ok(seatGradeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatGradeResponse> getSeatGrade(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(seatGradeService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeatGradeResponse> updateSeatGrade(
            @PathVariable Long id,
            @RequestBody SeatGradeUpdateRequest request
    ) {

        return ResponseEntity.ok(seatGradeService.updateSeatGrade(
                id,
                request.getName(),
                request.getBasePrice(),
                request.getSeatGradeStatus()
        ));
    }
}
