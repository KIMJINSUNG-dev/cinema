/***
 * ReservationService.java를 통해 프론트엔드로 반환하는 응답 객체를 생성하는 클래스
 * 작성이력
 * -2026-10-30: 파일 생성 및 작성 (ShowtimeResponse.java 구조 참고)
 * -2026-10-31: 예매 내역 조회 API를 위해 reservedAt(예매 일시) 필드 추가 (BaseEntity의 생성 일시 사용)
 *
 * 영화명 · 상영관명은 지연 로딩 값이라 서비스 트랜잭션 안에서(커밋 전) 이 생성자를 호출해야 한다.
 * 예약 총액은 D-42에 따라 저장하지 않고 좌석별 가격의 합으로 계산한다.
 ***/
package com.cinema.backend.domain.reservation.dto;

import com.cinema.backend.domain.reservation.Reservation;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ReservationResponse {

    private Long id;
    private Reservation.ReservationStatus reservationStatus;
    private Reservation.Channel channel;
    private Long showtimeId;
    private String movieName;
    private String screenName;
    private LocalDateTime startTime;
    private List<ReservationSeatResponse> seats;
    private int totalPrice;
    private LocalDateTime reservedAt;

    public ReservationResponse(Reservation reservation) {

        this.id = reservation.getId();
        this.reservationStatus = reservation.getReservationStatus();
        this.channel = reservation.getChannel();
        this.showtimeId = reservation.getShowtime().getId();
        this.movieName = reservation.getShowtime().getMovie().getName();
        this.screenName = reservation.getShowtime().getScreen().getName();
        this.startTime = reservation.getShowtime().getStartTime();
        this.reservedAt = reservation.getCreatedAt();
        this.seats = reservation.getReservationSeats().stream()
                .map(ReservationSeatResponse::new)
                .toList();
        this.totalPrice = this.seats.stream()
                .mapToInt(ReservationSeatResponse::getPrice)
                .sum();
    }
}
