/***
 * Reservation.java 엔티티 데이터에 접근하는 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-10-28: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.domain.member.Member;
import com.cinema.backend.domain.member.MemberRepository;
import com.cinema.backend.domain.screen.Seat;
import com.cinema.backend.domain.screen.SeatRepository;
import com.cinema.backend.domain.showtime.Showtime;
import com.cinema.backend.domain.showtime.ShowtimeRepository;
import com.cinema.backend.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ReservationSeatRepository reservationSeatRepository;

    // 락을 적용한 뒤 예약 좌석 검사 시 최신 커밋을 체크하도록 READ_COMMITTED 지정
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long createOnlineReservation(
            String email,
            Long showtimeId,
            List<Long> seatIds
    ) {

        Member member = memberRepository.findByEmail(email);

        if (member == null) {

            throw new IllegalArgumentException("존재하지 않는 회원정보입니다.");
        }

        if (member.getIsWithdrawn()) {

            throw new IllegalArgumentException("탈퇴한 회원정보입니다.");
        }

        Showtime showtime = showtimeRepository.findByIdForUpdate(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영정보입니다."));
        // TODO: 예약 조건 검사 - 상영 일정 활성·시작 전, 좌석 수(1~8)·중복, 좌석 소속·활성

        Reservation reservation = Reservation.online(member, showtime);
        for (Long seatId : seatIds) {

            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석정보입니다."));

            if (reservationSeatRepository.existsByShowtimeIdAndSeatIdAndActiveTrue(showtimeId, seatId)) {

                throw new ConflictException(seat.getSeatCode() + " 좌석은 이미 예약된 좌석입니다.");
            }
            reservation.addSeat(seat, seat.getSeatGrade().getBasePrice());
        }
        reservationRepository.save(reservation);
        reservation.confirm();

        return reservation.getId();
    }
}
