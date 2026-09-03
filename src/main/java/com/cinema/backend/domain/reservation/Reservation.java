/***
 * 예매정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-03: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.domain.member.Member;
import com.cinema.backend.domain.showtime.Showtime;
import com.cinema.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservations")
@NoArgsConstructor
@Getter
public class Reservation extends BaseEntity {

    public Reservation(Member member, Showtime showtime) {

        if (member == null) {

            throw new IllegalArgumentException("회원정보는 비워둘 수 없습니다.");
        }

        if (showtime == null) {

            throw new IllegalArgumentException("상영정보는 비워둘 수 없습니다.");
        }
        this.member = member;
        this.showtime = showtime;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @Column(nullable = false)
    private ReservationStatus reservationStatus = ReservationStatus.PENDING;

    public enum ReservationStatus {

        /***
         * 예약상태코드: PENDING-CONFIRMED-CANCELED-EXPIRED의 4단계 체계
         * PENDING: 좌석 선점중, 미결제 상태
         * CONFIRMED: 예매완료
         * CANCELED: 예매취소(회원이 직접 취소)
         * EXPIRED: 예매취소(PENDING 상태에서 선점 시간초과로 자동 취소)
         ***/
        PENDING, CONFIRMED, CANCELED, EXPIRED
    }

    public void updateReservationStatus(ReservationStatus reservationStatus) {

        if (reservationStatus == null) {

            throw new IllegalArgumentException("예약상태는 비워둘 수 없습니다.");
        }
        this.reservationStatus = reservationStatus;
    }
}
