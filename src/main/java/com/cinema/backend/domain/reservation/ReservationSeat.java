/***
 * 예매좌석정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-03: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 * -2026-10-08: 외부에서 기본 생성자로 빈 엔티티를 생성하지 못하도록 @NoArgsConstructor를 protected로 제한
 * -2026-10-26: 상영정보·예약 활성화 플래그 필드 추가 및 가격 update 관련 의사 결정에 따른
 *              생성자 구조 변경 및 updatePrice() 메서드 삭제, deactivate() 메서드 추가
 * -2026-10-26: 상영정보, 좌석, 예약 활성화 플래그로 DB 유니크 제약 생성
 * -2026-10-28: deactivate() 메서드에 호출 관련 안내 주석 추가
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.domain.screen.Seat;
import com.cinema.backend.domain.showtime.Showtime;
import com.cinema.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "reservation_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_seats_showtime_seat_active",
                columnNames = {"showtime_id", "seat_id", "active_flag"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ReservationSeat extends BaseEntity {

    ReservationSeat(Reservation reservation, Seat seat, int price) {

        if (seat == null) {

            throw new IllegalArgumentException("좌석은 비워둘 수 없습니다.");
        }

        if (reservation == null) {

            throw new IllegalArgumentException("예약정보는 비워둘 수 없습니다.");
        }

        if (price < 0) {

            throw new IllegalArgumentException("가격은 0보다 작을 수 없습니다.");
        }
        this.reservation = reservation;
        this.showtime = reservation.getShowtime();
        this.seat = seat;
        this.price = price;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @Column(nullable = false)
    private Integer price = 0;

    /***
     * 예약좌석이 활성화된 경우는 중복을 허용해서는 안 되고
     * 비활성화된 경우에는 중복을 허용해야 함
     * 활성화된 경우의 중복을 막기 위해
     * DB 유니크 제약(showtime_id, seat_id, active_flag 기반)을 적용하였으며
     * active_flag 값이 true인 경우에는 의도대로 중복을 허용하지 않음
     * 그러나 active_flag가 비활성화 상태인 경우끼리는 중복을 허용하여야 하므로
     * active 필드의 타입을 래퍼 타입으로 선언한 뒤
     * 비활성화하는 경우 active 값을 false가 아니라 null로 설정함
     * MySQL의 경우 DB 유니크 제약 검사에서 null은 중복으로 판별하지 않기 때문
     * 따라서 deactivate() 메서드 내부의 active 값을 null로 설정하는 코드에서
     * false로 설정 시 유니크 제약 위반이 발생하게 되니
     * 대입하는 값을 false로 설정하지 말 것
     ***/
    @Column(name = "active_flag")
    private Boolean active = true;

    // 엔티티 계층 호출용 메서드, 서비스 계층에서 호출 금지
    void deactivate() {

        this.active = null;
    }
}
