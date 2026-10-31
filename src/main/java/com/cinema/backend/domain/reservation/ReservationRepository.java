/***
 * Reservation.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-10-31: 예매 내역 조회 API를 위한 findByMemberId() 메서드 작성
 *              (고객용 · 직원용 목록 조회가 공통으로 사용, 정렬 · 페이지 크기는 Pageable로 전달받음)
 ***/
package com.cinema.backend.domain.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByMemberId(Long memberId, Pageable pageable);
}
