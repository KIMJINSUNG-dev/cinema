/***
 * Reservation.java 엔티티 데이터에 접근하는 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-10-28: 파일 생성 및 작성
 * -2026-10-29: createOnlineReservation() 메서드에 조건 검사 논리 추가 구현
 * -2026-10-30: createOnlineReservation() 반환 타입을 Long에서 ReservationResponse로 변경
 *              (다른 서비스와의 일관성, 영화명 · 상영관명은 트랜잭션 안에서 DTO로 변환)
 *              맨 앞에 상영 일정 id null 검사 추가(ShowtimeService의 null 검사 방식 참고)
 * -2026-10-31: 예매 내역 조회 · 취소 메서드 추가(고객용 findMyReservations() · findMyReservation() ·
 *              cancelMyReservation(), 직원용 findReservationsByMemberId() · findReservation() ·
 *              cancelReservation()), 공통으로 쓰는 private 메서드(findActiveMember(), findReservationEntity(),
 *              validateOwnership(), validateCancelDeadline()) 추가
 *              기존 createOnlineReservation()은 변경하지 않음(CLAUDE.md 기존 코드 보호 규칙)
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.domain.member.Member;
import com.cinema.backend.domain.member.MemberRepository;
import com.cinema.backend.domain.reservation.dto.ReservationResponse;
import com.cinema.backend.domain.screen.Seat;
import com.cinema.backend.domain.screen.SeatRepository;
import com.cinema.backend.domain.showtime.Showtime;
import com.cinema.backend.domain.showtime.ShowtimeRepository;
import com.cinema.backend.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ReservationSeatRepository reservationSeatRepository;

    @Value("${cinema.reservation.max-reservation-seat-count}")
    private int maxReservationSeatCount;

    // 취소 마감: 상영 시작 시각 - 이 값(분) 이후에는 취소 불가 (createOnlineReservation()의 max-reservation-seat-count와 같은 방식)
    @Value("${cinema.reservation.cancel-deadline-minutes}")
    private int cancelDeadlineMinutes;

    // 락을 적용한 뒤 예약 좌석 검사 시 최신 커밋을 체크하도록 READ_COMMITTED 지정
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse createOnlineReservation(
            String email,
            Long showtimeId,
            List<Long> seatIds
    ) {

        if (showtimeId == null) {

            throw new IllegalArgumentException("잘못된 상영정보입니다.");
        }

        Member member = memberRepository.findByEmail(email);

        if (member == null) {

            throw new IllegalArgumentException("존재하지 않는 회원정보입니다.");
        }

        if (member.getIsWithdrawn()) {

            throw new IllegalArgumentException("탈퇴한 회원정보입니다.");
        }

        if (seatIds == null || seatIds.isEmpty()) {

            throw new IllegalArgumentException("예매할 좌석을 선택해주세요.");
        }

        if (seatIds.size() != new HashSet<Long>(seatIds).size()) {

            throw new IllegalArgumentException("같은 좌석이 중복 선택되었습니다.");
        }

        if (seatIds.size() > maxReservationSeatCount) {

            throw new IllegalArgumentException(
                    String.format("예매 1회당 선택 가능한 좌석 수는 최대 %d석입니다.", maxReservationSeatCount)
            );
        }

        Showtime showtime = showtimeRepository.findByIdForUpdate(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영정보입니다."));

        Long screenId = showtime.getScreen().getId();

        if (showtime.getShowtimeStatus() != Showtime.ShowtimeStatus.ACTIVE) {

            throw new ConflictException("예매할 수 없는 상영정보입니다.");
        }

        if (!showtime.getStartTime().isAfter(LocalDateTime.now())) {

            throw new ConflictException("상영정보의 예매 가능 시간이 종료되었습니다.");
        }

        Reservation reservation = Reservation.online(member, showtime);

        for (Long seatId : seatIds) {

            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석정보입니다."));

            if (!screenId.equals(seat.getScreen().getId())) {

                throw new IllegalArgumentException("상영정보와 좌석의 상영관이 일치하지 않습니다.");
            }

            if (seat.getSeatStatus() != Seat.SeatStatus.ACTIVE) {

                throw new ConflictException("예매할 수 없는 좌석입니다.");
            }

            if (reservationSeatRepository.existsByShowtimeIdAndSeatIdAndActiveTrue(showtimeId, seatId)) {

                throw new ConflictException(seat.getSeatCode() + " 좌석은 이미 예약된 좌석입니다.");
            }
            reservation.addSeat(seat, seat.getSeatGrade().getBasePrice());
        }
        reservationRepository.save(reservation);
        reservation.confirm();

        return new ReservationResponse(reservation);
    }

    // 고객용: 본인 예약 목록 조회 (취소된 예약 포함, 정렬 · 페이지 크기는 컨트롤러의 Pageable 기본값을 따름)
    @Transactional(readOnly = true)
    public Page<ReservationResponse> findMyReservations(String email, Pageable pageable) {

        Member member = findActiveMember(email);

        return reservationRepository.findByMemberId(member.getId(), pageable)
                .map(ReservationResponse::new);
    }

    // 고객용: 본인 예약 단건 조회
    @Transactional(readOnly = true)
    public ReservationResponse findMyReservation(String email, Long id) {

        Member member = findActiveMember(email);
        Reservation reservation = findReservationEntity(id);
        validateOwnership(reservation, member);

        return new ReservationResponse(reservation);
    }

    // 고객용: 본인 예약 취소
    @Transactional
    public ReservationResponse cancelMyReservation(String email, Long id) {

        Member member = findActiveMember(email);
        Reservation reservation = findReservationEntity(id);
        validateOwnership(reservation, member);
        validateCancelDeadline(reservation);
        reservation.cancel();

        return new ReservationResponse(reservation);
    }

    // 직원용: 회원 id로 예약 목록 조회 (권한 확인은 SecurityConfig가 담당, 서비스에서는 역할을 확인하지 않음)
    @Transactional(readOnly = true)
    public Page<ReservationResponse> findReservationsByMemberId(Long memberId, Pageable pageable) {

        if (memberId == null) {

            throw new IllegalArgumentException("잘못된 회원정보입니다.");
        }

        if (!memberRepository.existsById(memberId)) {

            throw new IllegalArgumentException("존재하지 않는 회원정보입니다.");
        }

        return reservationRepository.findByMemberId(memberId, pageable)
                .map(ReservationResponse::new);
    }

    // 직원용: 예약 단건 조회
    @Transactional(readOnly = true)
    public ReservationResponse findReservation(Long id) {

        return new ReservationResponse(findReservationEntity(id));
    }

    // 직원용: 예약 취소
    @Transactional
    public ReservationResponse cancelReservation(Long id) {

        Reservation reservation = findReservationEntity(id);
        validateCancelDeadline(reservation);
        reservation.cancel();

        return new ReservationResponse(reservation);
    }

    // 회원 존재 · 탈퇴 검사 (createOnlineReservation()과 같은 메시지, 고객용 조회 · 취소 메서드가 공통으로 사용)
    private Member findActiveMember(String email) {

        Member member = memberRepository.findByEmail(email);

        if (member == null) {

            throw new IllegalArgumentException("존재하지 않는 회원정보입니다.");
        }

        if (member.getIsWithdrawn()) {

            throw new IllegalArgumentException("탈퇴한 회원정보입니다.");
        }
        return member;
    }

    // 예약 존재 검사 (기존 서비스들의 "존재하지 않는 ~" 메시지 방식)
    private Reservation findReservationEntity(Long id) {

        return reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약정보입니다."));
    }

    // 본인 확인 (D-14, MemberService.updateNickname() 등과 같은 방식 · 메시지)
    private void validateOwnership(Reservation reservation, Member member) {

        if (!reservation.getMember().getId().equals(member.getId())) {

            throw new AccessDeniedException("권한이 없습니다.");
        }
    }

    // 취소 마감 검사: 현재 시각이 "상영 시작 시각 - cancelDeadlineMinutes"와 같거나 그 이후면 거부 (고객용 · 직원용 취소가 공통으로 사용)
    private void validateCancelDeadline(Reservation reservation) {

        LocalDateTime deadline = reservation.getShowtime().getStartTime().minusMinutes(cancelDeadlineMinutes);

        if (!LocalDateTime.now().isBefore(deadline)) {

            throw new ConflictException("취소 가능 시간이 지난 예약입니다.");
        }
    }
}
