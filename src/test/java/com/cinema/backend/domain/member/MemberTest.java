/***
 * Member.java의 이메일 · 닉네임 정규화 규칙을 검증하는 단위 테스트(Spring · DB 없이 실행)를 담당하는 클래스
 * [AI 초안] 회원 이메일 · 닉네임 정규화(D-57 예정) 테스트 작성
 * 작성이력
 * -2026-11-24: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.member;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    private final Locale defaultLocale = Locale.getDefault();

    // 터키어 기본 Locale 테스트에서 바꾼 기본 Locale을 원복
    @AfterEach
    void restoreDefaultLocale() {

        Locale.setDefault(defaultLocale);
    }

    @Test
    @DisplayName("normalizeEmail - null이면 null")
    void normalizeEmail_null_returnsNull() {

        assertThat(Member.normalizeEmail(null)).isNull();
    }

    @Test
    @DisplayName("normalizeEmail - 앞뒤 공백 제거, 소문자로 변환")
    void normalizeEmail_trimsAndLowercases() {

        assertThat(Member.normalizeEmail(" A@B.Com ")).isEqualTo("a@b.com");
    }

    @Test
    @DisplayName("normalizeEmail - 기본 Locale이 터키어여도 로케일 비의존 소문자 변환")
    void normalizeEmail_turkishDefaultLocale_stillRootLocaleLowercase() {

        // 터키어 로케일에서 String.toLowerCase()를 쓰면 'I' -> 'ı'(점 없는 i)로 변환되는 문제가 있어,
        // normalizeEmail()이 Locale.ROOT를 명시해 이 문제를 피하는지 검증
        Locale.setDefault(new Locale("tr"));

        assertThat(Member.normalizeEmail("TITLE@X.COM")).isEqualTo("title@x.com");
    }

    @Test
    @DisplayName("updateNickname - 앞뒤 공백을 제거하고 저장")
    void updateNickname_trimsWhitespace() {

        Member member = new Member();

        member.updateNickname(" nick ");

        assertThat(member.getNickname()).isEqualTo("nick");
    }

    @Test
    @DisplayName("updateEmail - 정규화(trim · 소문자)해서 저장")
    void updateEmail_normalizesBeforeStoring() {

        Member member = new Member();

        member.updateEmail(" A@B.Com ");

        assertThat(member.getEmail()).isEqualTo("a@b.com");
    }
}
