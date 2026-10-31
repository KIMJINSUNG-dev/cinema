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
 * -2026-10-31: 직원 예약(회원 대리 예약 · 현장 판매, D-43) 추가
 *              createOnlineReservation()의 "회원 검사 이후" 공통 로직을 private createReservation()으로 추출하고
 *              (상영 일정 id null 검사 · 좌석 수 · 중복 검사 · 상영 일정 락 · 상태 · 시작 시각 검사 ·
 *              좌석 소속 · 활성 · 이미 예약됨 검사 · 좌석 추가 · 저장 · 확정, 검사 순서 · 예외 종류 · 메시지는 그대로 유지)
 *              createOnlineReservation() · createBoxOfficeMemberReservation() · createBoxOfficeWalkInReservation()
 *              세 진입 메서드가 Reservation 생성 방식(online/boxOffice)만 다르게 넘겨 공통으로 사용
 *              private 메서드(findActiveMemberById(), findActiveStaff()) 추가
 *              createOnlineReservation()의 회원 조회 · 검증 로직을 기존 findActiveMember()로 대체(중복 제거)
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

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

    // 현장 판매 예약의 주인(member), WalkInAccountInitializer가 앱 시작 시 이 이메일로 계정을 생성해둠 (D-43)
    @Value("${cinema.reservation.walk-in-account-email}")
    private String walkInAccountEmail;

    // 매표소 예약 처리 직원으로 허용하는 역할 (D-12 역할 계층과 같은 "허용 목록" 방식, D-41 validateTransition()과 같은 이유)
    private static final EnumSet<Member.Role> STAFF_OR_ABOVE =
            EnumSet.of(Member.Role.STAFF, Member.Role.MANAGER, Member.Role.ADMIN);

    // 락을 적용한 뒤 예약 좌석 검사 시 최신 커밋을 체크하도록 READ_COMMITTED 지정
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse createOnlineReservation(
            String email,
            Long showtimeId,
            List<Long> seatIds
    ) {

        Member member = findActiveMember(email);

        return createReservation(showtimeId, seatIds, showtime -> Reservation.online(member, showtime));
    }

    // 직원용: 회원 대리 예약 (주인 = 대상 회원, 처리 직원 = 로그인한 직원, D-43)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse createBoxOfficeMemberReservation(
            String staffEmail,
            Long targetMemberId,
            Long showtimeId,
            List<Long> seatIds
    ) {

        Member staff = findActiveStaff(staffEmail);
        Member targetMember = findActiveMemberById(targetMemberId);

        if (walkInAccountEmail.equals(targetMember.getEmail())) {

            // 대리 예약 API로 현장 판매(시스템 계정 소유)를 만드는 우회 경로 차단
            throw new IllegalArgumentException("현장 판매 시스템 계정은 대리 예약 대상이 될 수 없습니다.");
        }

        return createReservation(
                showtimeId, seatIds, showtime -> Reservation.boxOffice(targetMember, showtime, staff));
    }

    // 직원용: 현장 판매 (주인 = 현장 판매 시스템 계정, 처리 직원 = 로그인한 직원, D-43)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse createBoxOfficeWalkInReservation(
            String staffEmail,
            Long showtimeId,
            List<Long> seatIds
    ) {

        Member staff = findActiveStaff(staffEmail);

        Member walkInAccount = memberRepository.findByEmail(walkInAccountEmail);

        if (walkInAccount == null) {

            // 시스템 계정은 앱 시작 시 WalkInAccountInitializer가 자동 생성하므로, 여기서 없다면 설정값 오류 등 서버 문제다
            throw new IllegalStateException("현장 판매 시스템 계정을 찾을 수 없습니다.");
        }

        return createReservation(
                showtimeId, seatIds, showtime -> Reservation.boxOffice(walkInAccount, showtime, staff));
    }

    /*
     * 예약 공통 로직(D-39 · D-42): 상영 일정 id null 검사 · 좌석 수 · 중복 검사 ·
     * 상영 일정 락(비관적 락) · 상태 · 시작 시각 검사 · 좌석 소속 · 활성 · 이미 예약됨 검사 ·
     * 좌석 추가 · 저장 · 확정. 온라인 · 대리 · 현장 세 경로가 공통으로 사용하며,
     * 예약 객체 생성 방식(online/boxOffice)만 reservationFactory로 경로별로 다르게 받는다.
     * 검사 순서와 예외 종류 · 메시지는 기존 createOnlineReservation()과 같다.
     *
     * 주의: 이 메서드에는 @Transactional을 붙이지 않는다. private 메서드 호출은 프록시를 거치지 않아
     * 클래스 메서드 호출이 되므로 격리 수준(READ_COMMITTED, D-39)이 적용되지 않는다.
     * 따라서 이 메서드를 호출하는 공개 진입 메서드(createOnlineReservation() 등) 각각에 붙인다.
     */
    private ReservationResponse createReservation(
            Long showtimeId,
            List<Long> seatIds,
            Function<Showtime, Reservation> reservationFactory
    ) {

        if (showtimeId == null) {

            throw new IllegalArgumentException("잘못된 상영정보입니다.");
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

        Reservation reservation = reservationFactory.apply(showtime);

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

    // 회원 존재 · 탈퇴 검사 (findActiveMember()와 같은 메시지, id 기준. 대리 예약 대상 회원 확인에 사용)
    private Member findActiveMemberById(Long memberId) {

        if (memberId == null) {

            throw new IllegalArgumentException("잘못된 회원정보입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원정보입니다."));

        if (member.getIsWithdrawn()) {

            throw new IllegalArgumentException("탈퇴한 회원정보입니다.");
        }
        return member;
    }

    /*
     * 매표소 예약 처리 직원 확인(D-43): 토큰의 role은 발급 시점 기준이라 강등 · 탈퇴 후에도
     * Access Token이 만료되기 전까지는 요청이 통과한다(D-18과 같은 한계).
     * 매표소 예약은 처리 직원(handledBy)이 기록으로 남으므로, 여기서는 토큰이 아니라
     * DB의 최신 상태(존재 여부 · 탈퇴 여부 · 역할)를 다시 확인한다.
     */
    private Member findActiveStaff(String email) {

        Member staff = memberRepository.findByEmail(email);

        if (staff == null || staff.getIsWithdrawn() || !STAFF_OR_ABOVE.contains(staff.getRole())) {

            throw new AccessDeniedException("권한이 없습니다.");
        }
        return staff;
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
