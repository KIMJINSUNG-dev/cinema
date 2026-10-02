/***
 * 프론트엔드의 API 요청에 따라 MemberService.java의 서비스 메서드를 호출하는 컨트롤러 클래스
 * 작성이력
 * -2026-09-10: 파일 생성 및 작성
 * -2026-09-14: 자신의 회원정보 조회를 위한 getMyInfo() 메서드 작성
 * -2026-09-15: updateNickname(), updateIsWithdrawn() 메서드에
 *              요청 주체와 update 대상이 일치하는지 검증 추가
 ***/
package com.cinema.backend.domain.member;

import com.cinema.backend.domain.member.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberResponse> createMember(
            @RequestBody MemberCreateRequest request
    ) {

        MemberResponse response = memberService.createMember(
                request.getEmail(),
                request.getPassword(),
                request.getNickname()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<MemberResponse>> getMembers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        return ResponseEntity.ok(memberService.findAll(pageable));
    }

    @GetMapping("/myinfo")
    public ResponseEntity<MemberResponse> getMyInfo(
            @AuthenticationPrincipal String email
    ) {

        return ResponseEntity.ok(memberService.findByEmail(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMemberById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(memberService.findById(id));
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<MemberResponse> getMemberByEmail(
            @PathVariable String email
    ) {

        return ResponseEntity.ok(memberService.findByEmail(email));
    }

    @GetMapping("/by-nickname/{nickname}")
    public ResponseEntity<MemberResponse> getMemberByNickname(
            @PathVariable String nickname
    ) {

        return ResponseEntity.ok(memberService.findByNickname(nickname));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable Long id,
            @RequestBody MemberUpdateRequest request
    ) {

        return ResponseEntity.ok(
            memberService.updateMember(
                    id,
                    request.getNickname(),
                    request.getRole(),
                    request.getIsWithdrawn()
            )
        );
    }

    @PatchMapping("/{id}/nickname")
    public ResponseEntity<MemberResponse> updateNickname(
            @PathVariable Long id,
            @RequestBody MemberNicknameUpdateRequest request,
            @AuthenticationPrincipal String email
    ) {

        return ResponseEntity.ok(memberService.updateNickname(id, request.getNickname(), email));
    }

    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<MemberResponse> updateIsWithdrawn(
            @PathVariable Long id,
            @RequestBody MemberIsWithdrawnUpdateRequest request,
            @AuthenticationPrincipal String email
    ) {

        return ResponseEntity.ok(memberService.updateIsWithdrawn(id, request.getIsWithdrawn(), email));
    }
}
