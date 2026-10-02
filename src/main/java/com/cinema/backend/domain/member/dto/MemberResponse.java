/***
 * MemberService.java를 통해 프론트엔드로 반환하는 응답 객체를 생성하는 클래스
 * 작성이력
 * -2026-09-10: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.member.dto;

import com.cinema.backend.domain.member.Member;
import lombok.Getter;

@Getter
public class MemberResponse {

    private Long id;
    private String email;
    private String nickname;
    private Member.Role role;
    private Boolean emailVerified;
    private Boolean isWithdrawn;

    public MemberResponse(Member member) {

        this.id = member.getId();
        this.email = member.getEmail();
        this.nickname = member.getNickname();
        this.role = member.getRole();
        this.emailVerified = member.getEmailVerified();
        this.isWithdrawn = member.getIsWithdrawn();
    }
}
