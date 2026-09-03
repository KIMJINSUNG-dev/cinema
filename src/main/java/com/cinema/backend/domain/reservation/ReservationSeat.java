/***
 * 예매좌석정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-03: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.domain.screen.Seat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservation_seats")
@NoArgsConstructor
@Getter
public class ReservationSeat {

    public ReservationSeat(Seat seat, Reservation reservation) {

        if (seat == null) {

            throw new IllegalArgumentException("좌석은 비워둘 수 없습니다.");
        }

        if (reservation == null) {

            throw new IllegalArgumentException("예약정보는 비워둘 수 없습니다.");
        }
        this.seat = seat;
        this.reservation = reservation;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(nullable = false)
    private Integer price = 0;

    public void updatePrice(Integer price) {

        if (price == null) {

            throw new IllegalArgumentException("가격은 비워둘 수 없습니다.");
        }

        if (price < 0) {

            throw new IllegalArgumentException("올바른 가격을 입력해주세요.");
        }
        this.price = price;
    }
}
