/***
 * Member.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-09-10: 회원가입 기능 구현을 위한 findByEmail 메서드 작성
 ***/
package com.cinema.backend.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Member findByEmail(String email);
    Member findByNickname(String nickname);
}
