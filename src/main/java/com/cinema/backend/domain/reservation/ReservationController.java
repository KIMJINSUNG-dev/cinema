/***
 * 프론트엔드의 API 요청에 따라 ReservationService.java의 서비스 메서드를 호출하는 컨트롤러 클래스
 * 작성이력
 * -2026-10-30: 파일 생성 및 작성 (ShowtimeController.java 구조 참고)
 * -2026-10-31: 예매 내역 조회 · 취소 API 추가
 *              고객용(/me, 본인 예약만, email은 @AuthenticationPrincipal) ·
 *              직원용(memberId 쿼리 파라미터, 전체 회원 대상) 경로 분리 (D-13 · D-46)
 * -2026-10-31: 직원 예약(회원 대리 예약 · 현장 판매) API 추가 (D-43)
 *              처리 직원도 요청 본문이 아니라 로그인 정보(@AuthenticationPrincipal)에서 받음
 *
 * 예약 주체(회원)는 요청 본문이 아니라 로그인 정보(@AuthenticationPrincipal)에서 받는다.
 * (AuthController.logout()과 같은 방식)
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.domain.reservation.dto.ReservationBoxOfficeMemberCreateRequest;
import com.cinema.backend.domain.reservation.dto.ReservationBoxOfficeWalkInCreateRequest;
import com.cinema.backend.domain.reservation.dto.ReservationOnlineCreateRequest;
import com.cinema.backend.domain.reservation.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    // 직원용: 회원 대리 예약 (주인 = 대상 회원, 처리 직원 = 로그인한 직원)
    @PostMapping("/box-office/member")
    public ResponseEntity<ReservationResponse> createBoxOfficeMemberReservation(
            @AuthenticationPrincipal String staffEmail,
            @RequestBody ReservationBoxOfficeMemberCreateRequest request
    ) {

        ReservationResponse response = reservationService.createBoxOfficeMemberReservation(
                staffEmail,
                request.getMemberId(),
                request.getShowtimeId(),
                request.getSeatIds()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 직원용: 현장 판매 (주인 = 현장 판매 시스템 계정, 처리 직원 = 로그인한 직원)
    @PostMapping("/box-office/walk-in")
    public ResponseEntity<ReservationResponse> createBoxOfficeWalkInReservation(
            @AuthenticationPrincipal String staffEmail,
            @RequestBody ReservationBoxOfficeWalkInCreateRequest request
    ) {

        ReservationResponse response = reservationService.createBoxOfficeWalkInReservation(
                staffEmail,
                request.getShowtimeId(),
                request.getSeatIds()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 고객용: 본인 예약 목록 조회 (페이지당 10개, 예매 일시 최신순이 기본값)
    @GetMapping("/me")
    public ResponseEntity<Page<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal String email,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        return ResponseEntity.ok(reservationService.findMyReservations(email, pageable));
    }

    // 고객용: 본인 예약 단건 조회
    @GetMapping("/me/{id}")
    public ResponseEntity<ReservationResponse> getMyReservation(
            @AuthenticationPrincipal String email,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(reservationService.findMyReservation(email, id));
    }

    // 고객용: 본인 예약 취소
    @PatchMapping("/me/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancelMyReservation(
            @AuthenticationPrincipal String email,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(reservationService.cancelMyReservation(email, id));
    }

    // 직원용: 회원 id로 예약 목록 조회 (페이지당 10개, 예매 일시 최신순이 기본값)
    @GetMapping
    public ResponseEntity<Page<ReservationResponse>> getReservationsByMember(
            @RequestParam Long memberId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        return ResponseEntity.ok(reservationService.findReservationsByMemberId(memberId, pageable));
    }

    // 직원용: 예약 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(reservationService.findReservation(id));
    }

    // 직원용: 예약 취소
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(reservationService.cancelReservation(id));
    }
}
