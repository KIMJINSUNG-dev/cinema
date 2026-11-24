/***
 * MemberService.java의 이메일 · 닉네임 정규화 및 중복 검사 규칙을 검증하는 통합 테스트를 담당하는 클래스
 * [AI 초안] 회원 이메일 · 닉네임 정규화(D-57 예정) 테스트 작성
 * 작성이력
 * -2026-11-24: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.member;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.member.dto.MemberResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class MemberNormalizationTest {

    @Autowired
    private MemberService memberService;

    @Test
    @DisplayName("가입 - 대소문자 · 공백이 섞인 이메일은 정규화되어 저장된다")
    void createMember_normalizesEmailBeforeStoring() {

        MemberResponse response = memberService.createMember(" TEST@Test.com ", "password", "nick1");

        assertThat(response.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("가입 - 기존 이메일을 대소문자만 바꿔 가입하면 400")
    void createMember_emailDifferingOnlyByCase_rejected() {

        memberService.createMember("test@test.com", "password", "nick1");

        assertThatThrownBy(() -> memberService.createMember("TEST@TEST.com", "password", "nick2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 이메일입니다.");
    }

    @Test
    @DisplayName("가입 - 닉네임 끝 공백은 다른 값으로 취급되지 않고 중복으로 거부된다")
    void createMember_nicknameTrailingWhitespace_treatedAsDuplicate() {

        memberService.createMember("a@test.com", "password", "nick");

        assertThatThrownBy(() -> memberService.createMember("b@test.com", "password", "nick "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 닉네임입니다.");
    }

    @Test
    @DisplayName("가입 - 끝 공백이 trim되어 저장된 닉네임도 이후 가입의 중복 검사에 걸린다")
    void createMember_nicknameStoredTrimmed_rejectsLaterDuplicate() {

        // "nick "으로 가입하면 Member.updateNickname()이 trim하여 "nick"으로 저장됨 (Member.java 참고)
        memberService.createMember("a@test.com", "password", "nick ");

        assertThatThrownBy(() -> memberService.createMember("b@test.com", "password", "nick"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 닉네임입니다.");
    }

    @Test
    @DisplayName("본인 닉네임 변경 - 다른 회원 닉네임(끝 공백 포함)으로 변경하면 400")
    void updateNickname_toAnotherMembersNicknameWithTrailingWhitespace_rejected() {

        MemberResponse self = memberService.createMember("self@test.com", "password", "selfNick");
        memberService.createMember("other@test.com", "password", "otherNick");

        assertThatThrownBy(() -> memberService.updateNickname(self.getId(), "otherNick ", self.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 닉네임입니다.");
    }

    @Test
    @DisplayName("본인 닉네임 변경 - 현재 닉네임을 그대로 다시 지정하면 성공한다")
    void updateNickname_keepingSameNickname_succeeds() {

        MemberResponse self = memberService.createMember("self@test.com", "password", "selfNick");

        MemberResponse response = memberService.updateNickname(self.getId(), "selfNick", self.getEmail());

        assertThat(response.getNickname()).isEqualTo("selfNick");
    }

    @Test
    @DisplayName("본인 닉네임 변경 - 다른 회원 id로 변경을 시도하면 403, 본인 확인이 닉네임 중복 검사보다 먼저 이뤄진다")
    void updateNickname_byAnotherMembersId_rejectedBeforeDuplicateCheck() {

        MemberResponse self = memberService.createMember("self@test.com", "password", "selfNick");
        MemberResponse other = memberService.createMember("other@test.com", "password", "otherNick");

        // self 소유의 닉네임("selfNick")을 그대로 넘겨도, 닉네임 중복 검사(자기 자신 제외)를 먼저 했다면
        // other 입장에서는 selfNick이 다른 회원의 닉네임이라 400(중복)이 나야 한다.
        // 하지만 본인 확인(소유권 검사)이 먼저이므로, 중복 검사에 도달하지 않고 403이 발생한다.
        assertThatThrownBy(() -> memberService.updateNickname(other.getId(), self.getNickname(), self.getEmail()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("권한이 없습니다.");
    }

    @Test
    @DisplayName("관리자 수정 - 다른 회원이 쓰는 닉네임으로 변경하면 400")
    void updateMember_toAnotherMembersNickname_rejected() {

        MemberResponse target = memberService.createMember("target@test.com", "password", "targetNick");
        memberService.createMember("other@test.com", "password", "otherNick");

        assertThatThrownBy(() -> memberService.updateMember(
                target.getId(), "otherNick", target.getRole().name(), target.getIsWithdrawn()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 닉네임입니다.");
    }

    @Test
    @DisplayName("관리자 수정 - 대상 회원의 현재 닉네임을 그대로 지정하면 성공한다")
    void updateMember_keepingTargetsOwnNickname_succeeds() {

        MemberResponse target = memberService.createMember("target@test.com", "password", "targetNick");

        MemberResponse response = memberService.updateMember(
                target.getId(), "targetNick", target.getRole().name(), target.getIsWithdrawn());

        assertThat(response.getNickname()).isEqualTo("targetNick");
    }
}
