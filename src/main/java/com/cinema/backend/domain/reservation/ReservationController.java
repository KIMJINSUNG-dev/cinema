/***
 * 프론트엔드의 API 요청에 따라 ReservationService.java의 서비스 메서드를 호출하는 컨트롤러 클래스
 * 작성이력
 * -2026-10-30: 파일 생성 및 작성 (ShowtimeController.java 구조 참고)
 *
 * 예약 주체(회원)는 요청 본문이 아니라 로그인 정보(@AuthenticationPrincipal)에서 받는다.
 * (AuthController.logout()과 같은 방식)
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.domain.reservation.dto.ReservationOnlineCreateRequest;
import com.cinema.backend.domain.reservation.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/online")
    public ResponseEntity<ReservationResponse> createOnlineReservation(
            @AuthenticationPrincipal String email,
            @RequestBody ReservationOnlineCreateRequest request
    ) {

        ReservationResponse response = reservationService.createOnlineReservation(
                email,
                request.getShowtimeId(),
                request.getSeatIds()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
