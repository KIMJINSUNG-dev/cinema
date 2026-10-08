/***
 * Seat.java 엔티티 데이터에 접근하는 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-09-15: 파일 생성 및 작성
 * -2026-10-07: Seat.java의 생성자 구조 변경 및 createSeat() 메서드 구조 변경
 *              -new Seat() 생성자 호출에 따른 인자 변경
 *              -updateSeatGrade/updateSeatCode 호출 삭제
 *              -updateSeatStatus 호출을 seatStatus를 받는 경우에만 수행하도록 변경
 * -2026-10-07: 상영관과 좌석의 종속관계 검증을 위한 findById(), updateSeat() 메서드 구조 변경
 * -2026-10-07: DTO 객체 및 컨트롤러에서의 요청 편의를 위해 row 파라미터를 String 타입으로 변경,
 *              각 메서드 내부에서 직접 문자열 검사 및 char로 변환
 * -2026-10-07: updateSeat() 메서드의 중복 검사를 엔티티 변경 전, 새 위치 기준으로 수행하도록 변경
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.domain.screen.dto.SeatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cinema.backend.domain.screen.Seat.toRowNum;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;
    private final SeatGradeRepository seatGradeRepository;

    @Transactional
    public SeatResponse createSeat(
            Long screenId,
            Long seatGradeId,
            String row,
            int column,
            String seatStatus
    ) {

        if (screenId == null) {

            throw new IllegalArgumentException("잘못된 상영관 정보입니다.");
        }

        if (seatGradeId == null) {

            throw new IllegalArgumentException("잘못된 좌석등급 정보입니다.");
        }

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영관입니다."));
        SeatGrade seatGrade = seatGradeRepository.findById(seatGradeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석등급입니다."));

        if (row == null || row.length() != 1) {

            throw new IllegalArgumentException("잘못된 행 코드입니다.");
        }

        Seat seat = new Seat(screen, seatGrade, row.charAt(0), column);
        if (seatStatus != null && !seatStatus.isBlank()) {

            seat.updateSeatStatus(seatStatus);
        }

        if (seatRepository.findSeatByRowAndColumnAndScreen(seat.getRow(), seat.getColumn(), screen) != null)
            throw new IllegalArgumentException("이미 사용중인 좌석코드입니다.");

        seatRepository.save(seat);

        return new SeatResponse(seat);
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> findByScreenId(Long screenId) {

        if (screenId == null) {

            throw new IllegalArgumentException("잘못된 상영관 정보입니다.");
        }

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영관입니다."));

        return seatRepository.findByScreen(screen)
                .stream()
                .map(SeatResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeatResponse findById(Long screenId, Long seatId) {

        if (screenId == null) {

            throw new IllegalArgumentException("잘못된 상영관 정보입니다.");
        }

        if (seatId == null) {

            throw new IllegalArgumentException("잘못된 좌석 정보입니다.");
        }

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        if (!seat.getScreen().getId().equals(screenId)) {

            throw new IllegalArgumentException("상영관 정보와 좌석 정보가 일치하지 않습니다.");
        }

        return new SeatResponse(seat);
    }

    @Transactional
    public SeatResponse updateSeat(
            Long screenId,
            Long seatId,
            String row,
            int column,
            Long seatGradeId,
            String seatStatus
    ) {

        if (screenId == null) {

            throw new IllegalArgumentException("잘못된 상영관 정보입니다.");
        }

        if (seatId == null) {

            throw new IllegalArgumentException("잘못된 좌석 정보입니다.");
        }

        if (seatGradeId == null) {

            throw new IllegalArgumentException("잘못된 좌석등급 정보입니다.");
        }

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        SeatGrade seatGrade = seatGradeRepository.findById(seatGradeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석등급입니다."));

        if (!seat.getScreen().getId().equals(screenId)) {

            throw new IllegalArgumentException("상영관 정보와 좌석 정보가 일치하지 않습니다.");
        }
        if (row == null || row.length() != 1) {

            throw new IllegalArgumentException("잘못된 행 코드입니다.");
        }

        Seat existing = seatRepository.findSeatByRowAndColumnAndScreen(toRowNum(row.charAt(0)), column, seat.getScreen());
        if (existing != null && !existing.getId().equals(seat.getId()))
            throw new IllegalArgumentException("이미 사용중인 좌석코드입니다.");

        seat.updateSeatCode(row.charAt(0), column);
        seat.updateSeatGrade(seatGrade);
        seat.updateSeatStatus(seatStatus);

        return new SeatResponse(seat);
    }
}
