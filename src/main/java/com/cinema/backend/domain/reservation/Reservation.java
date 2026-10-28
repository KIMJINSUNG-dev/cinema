/***
 * 예매정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-03: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 * -2026-10-08: 외부에서 기본 생성자로 빈 엔티티를 생성하지 못하도록 @NoArgsConstructor를 protected로 제한
 * -2026-10-23: updateReservationStatus() 메서드를
 *              개별 상태 변환 메서드(confirm(), cancel(), expire())
 *              + 전이 검사 공통 메서드(validateTransition()) 방식으로 리워크,
 *              리워크 과정에서 ReservationStatus에 한국어 label 추가(예외 메시지 출력용)
 * -2026-10-25: 예매 경로 및 예매 담당 직원 필드 추가
 *              이에 따른 예매 경로 enum 추가 및 생성자 구조 재설계
 * -2026-10-25: IllegalStateException 처리기를 ConflictException으로 대체함에 따른
 *              validateTransition() 메서드 내부의 예외 인스턴스 변경
 * -2026-10-26: Reservation이 자신에 종속되는 ReservationSeat 목록을 알아야 하는 논리에 따른
 *              ReservationSeat 목록 조회·생성 및 추가 기능 추가
 *              (실제 DB에 반영되지는 않는 편의 메서드, DB 저장은 ReservationSeat를 별도로 save() 처리)
 *              2026-10-28: 코드 리워크로 해당 주석은 더 이상 유효하지 않음
 * -2026-10-28: 예약을 DB에 저장할 때(reservationSeats)
 *              예약 객체에 포함된 ReservationSeat 목록의 예약 좌석도 저장되도록
 *              reservationSeats 필드에 cascade = CascadeType.PERSIST 적용
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.domain.member.Member;
import com.cinema.backend.domain.screen.Seat;
import com.cinema.backend.domain.showtime.Showtime;
import com.cinema.backend.global.common.BaseEntity;
import com.cinema.backend.global.exception.ConflictException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Entity
@Table(name = "reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Reservation extends BaseEntity {

    private Reservation(Member member, Showtime showtime, Channel channel, Member handledBy) {

        if (member == null) {

            throw new IllegalArgumentException("회원정보는 비워둘 수 없습니다.");
        }

        if (showtime == null) {

            throw new IllegalArgumentException("상영정보는 비워둘 수 없습니다.");
        }
        this.member = member;
        this.showtime = showtime;
        this.channel = channel;
        this.handledBy = handledBy;
    }

    public static Reservation online(Member member, Showtime showtime) {

        return new Reservation(member, showtime, Channel.ONLINE, null);
    }

    public static Reservation boxOffice(Member member, Showtime showtime, Member handledBy) {

        if (handledBy == null) {

            throw new IllegalArgumentException("매표소 예약에는 처리 직원 정보가 필요합니다.");
        }
        return new Reservation(member, showtime, Channel.BOX_OFFICE, handledBy);
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
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_id")
    private Member handledBy;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus reservationStatus = ReservationStatus.PENDING;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.PERSIST)
    private List<ReservationSeat> reservationSeats = new ArrayList<>();

    public enum Channel {

        /***
         * 예약경로코드: ONLINE-BOX_OFFICE로 예약 경로 이분화
         * ONLINE: 고객이 직접 온라인 예매하는 경우
         * BOX_OFFICE: 현장 매표소에서 고객의 요청에 따라 직원이 예매 처리를 대행하는 경우
         ***/
        ONLINE("온라인"),
        BOX_OFFICE("매표소");

        private final String label;

        Channel(String label) {

            this.label = label;
        }

        public String getLabel() {

            return label;
        }
    }

    public enum ReservationStatus {

        /***
         * 예약상태코드: PENDING-CONFIRMED-CANCELED-EXPIRED의 4단계 체계
         * PENDING: 좌석 선점중, 미결제 상태
         * CONFIRMED: 예매완료
         * CANCELED: 예매취소(회원이 직접 취소)
         * EXPIRED: 예매취소(PENDING 상태에서 선점 시간초과로 자동 취소)
         ***/
        PENDING("결제 대기"),
        CONFIRMED("예매 완료"),
        CANCELED("취소"),
        EXPIRED("만료");

        private final String label;

        ReservationStatus(String label) {

            this.label = label;
        }

        public String getLabel() {

            return label;
        }
    }

    public void confirm() {

        validateTransition("확정", EnumSet.of(ReservationStatus.PENDING));
        this.reservationStatus = ReservationStatus.CONFIRMED;
    }

    public void cancel() {

        validateTransition("취소", EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
        this.reservationStatus = ReservationStatus.CANCELED;
        this.reservationSeats.forEach(ReservationSeat::deactivate);
    }

    public void expire() {

        validateTransition("만료 처리", EnumSet.of(ReservationStatus.PENDING));
        this.reservationStatus = ReservationStatus.EXPIRED;
        this.reservationSeats.forEach(ReservationSeat::deactivate);
    }

    private void validateTransition(String action, Set<ReservationStatus> allowedFrom) {

        if (!allowedFrom.contains(this.reservationStatus)) {

            throw new ConflictException(
                    String.format(
                            "%s 상태의 예약은 %s할 수 없습니다.",
                            this.reservationStatus.getLabel(),
                            action
                    )
            );
        }
    }

    public ReservationSeat addSeat(Seat seat, int price) {

        validateTransition("좌석을 추가", EnumSet.of(ReservationStatus.PENDING));
        ReservationSeat reservationSeat = new ReservationSeat(this, seat, price);
        this.reservationSeats.add(reservationSeat);
        return reservationSeat;
    }

    // 읽기 전용 메서드
    public List<ReservationSeat> getReservationSeats() {

        return Collections.unmodifiableList(reservationSeats);
    }
}
