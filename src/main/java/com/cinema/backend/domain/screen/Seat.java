/***
 * 좌석 정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-01: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seats")
@NoArgsConstructor
@Getter
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id")
    private Screen screen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_grade_id")
    private SeatGrade seatGrade;

    @Column(nullable = false)
    private int row;

    @Column(nullable = false)
    private int column;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus seatStatus = SeatStatus.ACTIVE;

    public enum SeatStatus {

        ACTIVE, DEACTIVATED
    }

    public void updateSeatCode(char row, int column) {

        int rowNum = (int)Character.toUpperCase(row) - 64;
        if (rowNum < 1) {

            throw new IllegalArgumentException("올바른 행 코드를 입력해주세요.");
        }
        if (column < 1) {

            throw new IllegalArgumentException("올바른 열 코드를 입력해주세요.");
        }
        this.row = rowNum;
        this.column = column;
    }

    public void updateSeatGrade(SeatGrade seatGrade) {

        if (seatGrade == null) {

            throw new IllegalArgumentException("좌석타입은 비워둘 수 없습니다.");
        }
        this.seatGrade = seatGrade;
    }

    public void updateSeatStatus(SeatStatus seatStatus) {

        if (seatStatus == null) {

            throw new IllegalArgumentException("좌석상태는 비워둘 수 없습니다.");
        }
        this.seatStatus = seatStatus;
    }

    public String getSeatCode() {

        return String.valueOf((char)(row + 64)) + column;
    }
}
