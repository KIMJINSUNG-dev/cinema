/***
 * Seat.java 엔티티 데이터에 접근하는 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-09-15: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.domain.screen.dto.SeatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
            char row,
            int column,
            String seatStatus
    ) {

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영관입니다."));
        SeatGrade seatGrade = seatGradeRepository.findById(seatGradeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석등급입니다."));


        Seat seat = new Seat(screen);

        seat.updateSeatGrade(seatGrade);
        seat.updateSeatCode(row, column);
        seat.updateSeatStatus(seatStatus);

        if (seatRepository.findSeatByRowAndColumnAndScreen(seat.getRow(), seat.getColumn(), screen) != null)
            throw new IllegalArgumentException("이미 사용중인 좌석코드입니다.");

        seatRepository.save(seat);

        return new SeatResponse(seat);
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> findByScreenId(Long screenId) {

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영관입니다."));

        return seatRepository.findByScreen(screen)
                .stream()
                .map(SeatResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeatResponse findById(Long seatId) {

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        return new SeatResponse(seat);
    }

    @Transactional
    public SeatResponse updateSeat(
            Long seatId,
            char row,
            int column,
            Long seatGradeId,
            String seatStatus
    ) {

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        SeatGrade seatGrade = seatGradeRepository.findById(seatGradeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석등급입니다."));


        seat.updateSeatCode(row, column);
        seat.updateSeatGrade(seatGrade);
        seat.updateSeatStatus(seatStatus);

        Seat existing = seatRepository.findSeatByRowAndColumnAndScreen(seat.getRow(), seat.getColumn(), seat.getScreen());
        if (existing != null && !existing.getId().equals(seat.getId()))
            throw new IllegalArgumentException("이미 사용중인 좌석코드입니다.");

        return new SeatResponse(seat);
    }
}
