/***
 * 회원 정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-03: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 * -2026-09-10: 외부 접근 허용 메서드의 enum 파라미터를 String 기반으로 교체, 탈퇴여부 필드 추가
 * -2026-10-08: 외부에서 기본 생성자로 빈 엔티티를 생성하지 못하도록 @NoArgsConstructor를 protected로 제한
 * -2026-10-25: 사용자 등급 체계에 대한 의사 결정에 따른 등급별 권한에 대한 주석 내용 보완
 ***/
package com.cinema.backend.domain.member;

import com.cinema.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이메일의 경우 긴 이메일을 고려하여 length = 100으로 설정
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 비밀번호의 경우 BCrypt 암호화를 고려하여(암호화 결과값의 길이 = 60자) length = 100으로 설정
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(nullable = false)
    private Boolean isWithdrawn = false;

    public enum Role {

        /***
         * 사용자 등급 체계: ADMIN-MANAGER-STAFF-CUSTOMER의 4단계 체계
         * 상위 등급의 권한은 하위 등급의 권한을 포함함
         * ADMIN: MANAGER, STAFF 등급 계정 생성 및 권한 변경 권한
         * MANAGER: 영화 및 상영시간표 편집 권한
         * STAFF: 예매 등록(회원 대리 예약·현장 판매) 및 전체 회원 대상 예매내역 조회, 취소 권한
         * CUSTOMER: 영화 및 상영시간표 조회, 자기 자신의 예매 등록 및 예매내역 조회, 취소 권한
         * ※ 전 등급 공통으로 예매 변경은 update 방식이 아니라 취소 후 재등록하는 방식
         ***/
        ADMIN, MANAGER, STAFF, CUSTOMER
    }

    public void updateEmail(String email) {

        if (email == null || email.isBlank()) {

            throw new IllegalArgumentException("이메일은 비워둘 수 없습니다.");
        }
        this.email = email;
    }

    public void updatePassword(String password) {

        if (password == null || password.isBlank()) {

            throw new IllegalArgumentException("비밀번호는 비워둘 수 없습니다.");
        }
        this.password = password;
    }

    public void updateNickname(String nickname) {

        if (nickname == null || nickname.isBlank()) {

            throw new IllegalArgumentException("닉네임은 비워둘 수 없습니다.");
        }
        this.nickname = nickname;
    }

    public void updateRole(String role) {

        if (role == null || role.isBlank()) {

            throw new IllegalArgumentException("사용자 등급은 비워둘 수 없습니다.");
        }

        try {

            this.role = Role.valueOf(role);
        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException("잘못된 형식의 사용자 등급코드입니다: " + role);
        }
    }

    public void updateEmailVerified(Boolean emailVerified) {

        if (emailVerified == null) {

            throw new IllegalArgumentException("이메일 인증 여부는 비워둘 수 없습니다.");
        }
        this.emailVerified = emailVerified;
    }

    public void updateIsWithdrawn(Boolean isWithdrawn) {

        if (isWithdrawn == null) {

            throw new IllegalArgumentException("회원탈퇴 여부는 비워둘 수 없습니다.");
        }
        this.isWithdrawn = isWithdrawn;
    }
}
