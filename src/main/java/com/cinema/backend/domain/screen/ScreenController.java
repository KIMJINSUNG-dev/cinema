/***
 * 프론트엔드의 API 요청에 따라 ScreenService.java의 서비스 메서드를 호출하는 컨트롤러 클래스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.domain.screen.dto.ScreenCreateRequest;
import com.cinema.backend.domain.screen.dto.ScreenResponse;
import com.cinema.backend.domain.screen.dto.ScreenUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screens")
@RequiredArgsConstructor
public class ScreenController {

    private final ScreenService screenService;

    @PostMapping
    public ResponseEntity<ScreenResponse> createScreen(
            @RequestBody ScreenCreateRequest request
    ) {

        ScreenResponse response = screenService.createScreen(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ScreenResponse>> getScreens() {

        return ResponseEntity.ok(screenService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScreenResponse> getScreen(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(screenService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScreenResponse> updateScreen(

            @PathVariable Long id,
            @RequestBody ScreenUpdateRequest request
    ) {

        return ResponseEntity.ok(
                screenService.updateScreen(id, request.getName(), request.getActiveStatus())
        );
    }
}
