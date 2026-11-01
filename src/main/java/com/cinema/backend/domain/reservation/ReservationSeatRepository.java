/***
 * ReservationSeat.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-10-28: 예매 기능 구현을 위한 메서드 작성
 *              -countByShowtimeIdAndSeatIdAndActiveTrue
 *              -existsByShowtimeIdAndSeatIdAndActiveTrue
 * -2026-11-01: 상영 일정 변경 제한을 위한 메서드 작성
 *              -existsByShowtimeIdAndActiveTrue
 ***/
package com.cinema.backend.domain.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {

    long countByShowtimeIdAndSeatIdAndActiveTrue(Long showtimeId, Long seatId);
    boolean existsByShowtimeIdAndSeatIdAndActiveTrue(Long showtimeId, Long seatId);
    boolean existsByShowtimeIdAndActiveTrue(Long showtimeId);
}
