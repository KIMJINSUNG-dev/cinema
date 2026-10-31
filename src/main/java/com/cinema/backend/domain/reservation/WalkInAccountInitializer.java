/***
 * 앱 시작 시 현장 판매(매표소) 전용 시스템 계정을 초기화하는 컴포넌트
 * 작성이력
 * -2026-10-31: 파일 생성 및 작성
 *
 * 현장 판매 예약의 주인(member)은 근무 직원 계정이 아니라 이 시스템 계정이다(D-43).
 * 설정값(cinema.reservation.walk-in-account-email)의 이메일로 회원이 없을 때만 생성해
 * 재시작해도 중복 생성되지 않는다. 생성은 기존 MemberService.createMember()를 그대로 사용해
 * 다른 회원가입과 같은 검증(이메일 · 닉네임 중복, 비밀번호 비어있음 등)을 거친다.
 * 비밀번호는 아무도 몰라야 하므로(D-43) 무작위 값을 생성해 로그에 남기지 않는다.
 * 역할은 Member의 기본값(CUSTOMER)을 그대로 둔다. 이 계정은 로그인하지 않고
 * ReservationService가 예약의 주인(member)으로만 참조하므로 역할 자체가 의미를 갖지 않는다.
 *
 * ApplicationRunner로 둔 이유: Spring Boot가 파싱한 ApplicationArguments를 받는 관용적인
 * 1회성 초기화 지점이고, 다른 Bean이 모두 준비된 뒤(컨텍스트 완전 초기화 후) 실행되어
 * MemberService · MemberRepository를 그대로 주입받아 쓸 수 있다.
 *
 * 패키지 위치: 생성 결과는 Member 엔티티이지만 "현장 판매 시스템 계정"이라는 개념 자체가
 * 매표소 예약(D-43)에 종속된 예약 도메인 개념이고, ReservationService도 같은 설정값
 * (cinema.reservation.walk-in-account-email)으로 이 계정을 조회하므로 domain.member가 아닌
 * domain.reservation 패키지에 둔다.
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.domain.member.MemberRepository;
import com.cinema.backend.domain.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalkInAccountInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @Value("${cinema.reservation.walk-in-account-email}")
    private String walkInAccountEmail;

    @Override
    public void run(ApplicationArguments args) {

        if (memberRepository.findByEmail(walkInAccountEmail) != null) {

            return;
        }

        // 비밀번호는 아무도 몰라야 하므로(D-43) 무작위 값을 생성하고 로그로 남기지 않는다
        String randomPassword = UUID.randomUUID().toString() + UUID.randomUUID();
        memberService.createMember(walkInAccountEmail, randomPassword, "[시스템] 현장판매");
    }
}
