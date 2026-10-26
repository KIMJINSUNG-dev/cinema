/***
 * 예약 상태 전이 및 생성 규칙을 검증하는 단위 테스트(Spring · DB 없이 실행)를 담당하는 클래스
 * 작성이력
 * -2026-10-23: 파일 생성 및 작성
 * -2026-10-25: Reservation.java의 예매 경로 및 예매 담당 직원 필드 추가에 따른 검증 메서드 추가
 *              online_without_handled_by_success(),
 *              box_office_handled_by_is_same_as_staff(),
 *              box_office_without_handled_by_rejected()
 * -2026-10-25: IllegalStateException 처리기를 ConflictException으로 대체함에 따른
 *              예외 인스턴스 검사 코드 수정
 * -2026-10-26: ReservationSeat.java의 DB 유니크 제약 기능 구현 및
 *              Reservation.java, ReservationSeat.java 사이의
 *              양방향 관계 구현에 따른 검증 메서드 추가
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.domain.member.Member;
import com.cinema.backend.domain.screen.Seat;
import com.cinema.backend.domain.showtime.Showtime;
import com.cinema.backend.global.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ReservationTest {

    // 상태 전이 로직은 회원·상영 일정을 쓰지 않으므로 가짜 객체로 생성
    private Reservation newReservation() {

        return Reservation.online(mock(Member.class), mock(Showtime.class));
    }

    @Test
    @DisplayName("결제 대기 → 확정: 허용")
    void pending_confirm_allowed() {

        Reservation reservation = newReservation();

        reservation.confirm();

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("예매 완료 → 확정: 거부되고 상태는 그대로")
    void confirmed_confirm_rejected() {

        Reservation reservation = newReservation();
        reservation.confirm(); // 준비: 예매 완료 상태로 만들기

        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("예매 완료 상태의 예약은 확정할 수 없습니다");

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("취소 → 확정: 거부되고 상태는 그대로")
    void canceled_confirm_rejected() {

        Reservation reservation = newReservation();
        reservation.cancel(); // 준비: 취소 상태로 만들기

        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("취소 상태의 예약은 확정할 수 없습니다");

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CANCELED);
    }

    @Test
    @DisplayName("만료 → 확정: 거부되고 상태는 그대로")
    void expired_confirm_rejected() {

        Reservation reservation = newReservation();
        reservation.expire(); // 준비: 만료 상태로 만들기

        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("만료 상태의 예약은 확정할 수 없습니다");

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.EXPIRED);
    }

    @Test
    @DisplayName("결제 대기 → 취소: 허용")
    void pending_cancel_allowed() {

        Reservation reservation = newReservation();

        reservation.cancel();

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CANCELED);
    }

    @Test
    @DisplayName("예매 완료 → 취소: 허용")
    void confirmed_cancel_allowed() {

        Reservation reservation = newReservation();
        reservation.confirm(); // 준비: 예매 완료 상태로 만들기

        reservation.cancel();

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CANCELED);
    }

    @Test
    @DisplayName("취소 → 취소: 거부되고 상태는 그대로")
    void canceled_cancel_rejected() {

        Reservation reservation = newReservation();
        reservation.cancel(); // 준비: 취소 상태로 만들기

        assertThatThrownBy(reservation::cancel)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("취소 상태의 예약은 취소할 수 없습니다");

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CANCELED);
    }

    @Test
    @DisplayName("만료 → 취소: 거부되고 상태는 그대로")
    void expired_cancel_rejected() {

        Reservation reservation = newReservation();
        reservation.expire(); // 준비: 만료 상태로 만들기

        assertThatThrownBy(reservation::cancel)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("만료 상태의 예약은 취소할 수 없습니다");

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.EXPIRED);
    }

    @Test
    @DisplayName("결제 대기 → 만료: 허용")
    void pending_expire_allowed() {

        Reservation reservation = newReservation();

        reservation.expire();

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.EXPIRED);
    }

    @Test
    @DisplayName("예매 완료 → 만료: 거부되고 상태는 그대로")
    void confirmed_expire_rejected() {

        Reservation reservation = newReservation();
        reservation.confirm(); // 준비: 예매 완료 상태로 만들기

        assertThatThrownBy(reservation::expire)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("예매 완료 상태의 예약은 만료 처리할 수 없습니다");

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("취소 → 만료: 거부되고 상태는 그대로")
    void canceled_expire_rejected() {

        Reservation reservation = newReservation();
        reservation.cancel(); // 준비: 취소 상태로 만들기

        assertThatThrownBy(reservation::expire)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("취소 상태의 예약은 만료 처리할 수 없습니다");

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CANCELED);
    }

    @Test
    @DisplayName("만료 → 만료: 거부되고 상태는 그대로")
    void expired_expire_rejected() {

        Reservation reservation = newReservation();
        reservation.expire(); // 준비: 만료 상태로 만들기

        assertThatThrownBy(reservation::expire)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("만료 상태의 예약은 만료 처리할 수 없습니다");

        assertThat(reservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.EXPIRED);
    }

    @Test
    @DisplayName("온라인 예매 시 처리 직원은 항상 비어 있음")
    void online_without_handled_by_success() {

        Reservation reservation = Reservation.online(
                mock(Member.class),
                mock(Showtime.class)
        );

        assertThat(reservation.getChannel()).isEqualTo(Reservation.Channel.ONLINE);
        assertThat(reservation.getHandledBy()).isNull();
    }

    @Test
    @DisplayName("매표소 예매 시 예매 처리 주체는 매표소 담당 직원과 같음")
    void box_office_handled_by_is_same_as_staff() {

        Member staff = mock(Member.class);

        Reservation reservation = Reservation.boxOffice(
                mock(Member.class),
                mock(Showtime.class),
                staff
        );

        assertThat(reservation.getChannel()).isEqualTo(Reservation.Channel.BOX_OFFICE);
        assertThat(reservation.getHandledBy()).isSameAs(staff);
    }

    @Test
    @DisplayName("매표소 예매 시 처리 직원이 비어 있으면 예매 실패")
    void box_office_without_handled_by_rejected() {

        assertThatThrownBy(
                () -> Reservation.boxOffice(
                        mock(Member.class),
                        mock(Showtime.class),
                        null
                )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("매표소 예약에는 처리 직원 정보가 필요합니다.");
    }

    @Test
    @DisplayName("예매와 예매좌석의 상영정보가 일치, 예매좌석의 활성화 초기값은 활성화, 가격은 설정한 대로, 목록이 추가한 예매좌석을 정확히 포함")
    void reservation_add_seat_data_expected() {

        Reservation reservation = newReservation();

        int price = 10000;
        ReservationSeat reservationSeat = reservation.addSeat(mock(Seat.class), price);
        assertThat(reservation.getShowtime()).isSameAs(reservationSeat.getShowtime());
        assertThat(reservationSeat.getActive()).isTrue();
        assertThat(reservationSeat.getPrice()).isEqualTo(price);
        assertThat(reservation.getReservationSeats()).containsExactly(reservationSeat);
    }

    @Test
    @DisplayName("예매 취소 시 좌석 활성화 해제")
    void reservation_cancel_seat_deactivate() {

        Reservation reservation = newReservation();

        ReservationSeat reservationSeat1 = reservation.addSeat(mock(Seat.class), 0);
        ReservationSeat reservationSeat2 = reservation.addSeat(mock(Seat.class), 0);
        reservation.cancel();
        assertThat(reservationSeat1.getActive()).isNull();
        assertThat(reservationSeat2.getActive()).isNull();
    }

    @Test
    @DisplayName("예매 만료 시 좌석 활성화 해제")
    void reservation_expire_seat_deactivate() {

        Reservation reservation = newReservation();

        ReservationSeat reservationSeat1 = reservation.addSeat(mock(Seat.class), 0);
        ReservationSeat reservationSeat2 = reservation.addSeat(mock(Seat.class), 0);
        reservation.expire();
        assertThat(reservationSeat1.getActive()).isNull();
        assertThat(reservationSeat2.getActive()).isNull();
    }

    @Test
    @DisplayName("예매 확정 시 좌석 활성화 유지")
    void reservation_confirm_seat_stays_active() {

        Reservation reservation = newReservation();

        ReservationSeat reservationSeat = reservation.addSeat(mock(Seat.class), 0);
        reservation.confirm();
        assertThat(reservationSeat.getActive()).isTrue();
    }

    @Test
    @DisplayName("예매 완료 상태에서 좌석 추가 거부")
    void confirmed_reservation_add_seat_rejected() {

        Reservation reservation = newReservation();

        ReservationSeat reservationSeat = reservation.addSeat(mock(Seat.class), 0);
        reservation.confirm();

        assertThatThrownBy(() -> reservation.addSeat(mock(Seat.class), 0))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("예매 완료 상태의 예약은 좌석을 추가할 수 없습니다.");
        assertThat(reservation.getReservationSeats()).containsExactly(reservationSeat);
    }

    @Test
    @DisplayName("예매 좌석 추가 시 좌석에 null 할당 거부")
    void reservation_add_seat_seat_data_null_rejected() {

        Reservation reservation = newReservation();

        assertThatThrownBy(() -> reservation.addSeat(null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("좌석은 비워둘 수 없습니다.");
    }

    @Test
    @DisplayName("예매 좌석 추가 시 가격에 음수 할당 거부")
    void reservation_add_seat_price_negative_number_rejected() {

        Reservation reservation = newReservation();

        assertThatThrownBy(() -> reservation.addSeat(mock(Seat.class), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("가격은 0보다 작을 수 없습니다.");
    }

    @Test
    @DisplayName("예매 좌석 목록 직접 수정 거부")
    void reservation_seats_update_rejected() {

        Reservation reservation = newReservation();

        assertThatThrownBy(() -> reservation.getReservationSeats().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
