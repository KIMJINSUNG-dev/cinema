/***
 * Member.java 엔티티 데이터에 접근하는 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-09-10: 파일 생성 및 작성
 * -2026-09-15: updateNickname(), updateIsWithdrawn() 메서드에
 *              요청 주체와 update 대상이 일치하는지 검증 추가
 ***/
package com.cinema.backend.domain.member;

import com.cinema.backend.domain.member.dto.MemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse createMember(
            String email,
            String password,
            String nickname) {

        Member member = new Member();

        if (memberRepository.findByEmail(email) != null) {

            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }

        if (memberRepository.findByNickname(nickname) != null) {

            throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
        }

        member.updateEmail(email);
        if (password == null || password.isBlank()) {

            throw new IllegalArgumentException("비밀번호는 비워둘 수 없습니다.");
        }
        member.updatePassword(passwordEncoder.encode(password));
        member.updateNickname(nickname);
        memberRepository.save(member);

        return new MemberResponse(member);
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> findAll(Pageable pageable) {

        return memberRepository.findAll(pageable)
                .map(MemberResponse::new);
    }

    @Transactional(readOnly = true)
    public MemberResponse findById(Long id) {

        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return new MemberResponse(member);
    }

    @Transactional(readOnly = true)
    public MemberResponse findByEmail(String email) {

        Member member = memberRepository.findByEmail(email);
        if (member == null) throw new IllegalArgumentException("존재하지 않는 회원입니다.");

        return new MemberResponse(member);
    }

    @Transactional(readOnly = true)
    public MemberResponse findByNickname(String nickname) {

        Member member = memberRepository.findByNickname(nickname);
        if (member == null) throw new IllegalArgumentException("존재하지 않는 회원입니다.");

        return new MemberResponse(member);
    }

    @Transactional
    public MemberResponse updateMember(
            Long id,
            String nickname,
            String role,
            Boolean isWithdrawn) {

        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        member.updateNickname(nickname);
        member.updateRole(role);
        member.updateIsWithdrawn(isWithdrawn);

        return new MemberResponse(member);
    }

    @Transactional
    public MemberResponse updateNickname(Long id, String nickname, String email) {

        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!member.getEmail().equals(email)) {

            throw new AccessDeniedException("권한이 없습니다.");
        }

        member.updateNickname(nickname);

        return new MemberResponse(member);
    }

    @Transactional
    public MemberResponse updateIsWithdrawn(Long id, Boolean isWithdrawn, String email) {

        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!member.getEmail().equals(email)) {

            throw new AccessDeniedException("권한이 없습니다.");
        }

        member.updateIsWithdrawn(isWithdrawn);

        return new MemberResponse(member);
    }
}
