/***
 * SeatGrade.java 엔티티 데이터에 접근하는 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-10-12: 파일 생성 및 작성 (SeatService.java 구조 참고)
 *
 * 이름 중복 검사 관련 설계
 * - 생성: SeatGrade 생성자가 내부적으로 updateName()을 호출해 트리밍·공백 검증을 마친 뒤이므로,
 *         생성된 엔티티의 getName()(트리밍 완료 값)으로 중복 검사 (createSeat()의 좌석코드 중복 검사와 동일한 순서)
 * - 수정: 기존 엔티티를 변경하기 전에 새 이름(트리밍 값)으로 먼저 중복 검사를 수행하고,
 *         조회된 결과가 수정 대상 자신이면 중복으로 보지 않음 (updateSeat()의 좌석코드 중복 검사 순서와 동일)
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.domain.screen.dto.SeatGradeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatGradeService {

    private final SeatGradeRepository seatGradeRepository;

    @Transactional
    public SeatGradeResponse createSeatGrade(
            String name,
            Integer basePrice,
            String seatGradeStatus
    ) {

        if (basePrice == null) {

            throw new IllegalArgumentException("가격 정보는 비워둘 수 없습니다.");
        }

        SeatGrade seatGrade = new SeatGrade(name, basePrice);
        if (seatGradeStatus != null && !seatGradeStatus.isBlank()) {

            seatGrade.updateSeatGradeStatus(seatGradeStatus);
        }

        if (seatGradeRepository.findByName(seatGrade.getName()) != null) {

            throw new IllegalArgumentException("이미 사용중인 좌석등급명입니다.");
        }

        seatGradeRepository.save(seatGrade);

        return new SeatGradeResponse(seatGrade);
    }

    @Transactional(readOnly = true)
    public List<SeatGradeResponse> findAll() {

        return seatGradeRepository.findAll()
                .stream()
                .map(SeatGradeResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeatGradeResponse findById(Long id) {

        SeatGrade seatGrade = seatGradeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석등급입니다."));

        return new SeatGradeResponse(seatGrade);
    }

    @Transactional
    public SeatGradeResponse updateSeatGrade(
            Long id,
            String name,
            Integer basePrice,
            String seatGradeStatus
    ) {

        if (basePrice == null) {

            throw new IllegalArgumentException("가격 정보는 비워둘 수 없습니다.");
        }

        SeatGrade seatGrade = seatGradeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석등급입니다."));

        String trimmedName = (name != null) ? name.trim() : null;
        if (trimmedName != null && !trimmedName.isBlank()) {

            SeatGrade existing = seatGradeRepository.findByName(trimmedName);
            if (existing != null && !existing.getId().equals(seatGrade.getId())) {

                throw new IllegalArgumentException("이미 사용중인 좌석등급명입니다.");
            }
        }

        seatGrade.updateName(name);
        seatGrade.updateBasePrice(basePrice);
        seatGrade.updateSeatGradeStatus(seatGradeStatus);

        return new SeatGradeResponse(seatGrade);
    }
}
