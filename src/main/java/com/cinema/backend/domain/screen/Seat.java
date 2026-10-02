/***
 * 좌석 정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-01: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 * -2026-09-14: JPA 오류로 인한 row / column 필드의 이름 재지정
 * -2026-09-14: screen / seatGrade 필드에 nullable = false 속성 부여
 * -2026-09-15: Screen을 파라미터로 받는 생성자 추가
 * -2026-09-15: updateSeatStatus() 메서드의 파라미터를 String 기반으로 변경
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seats")
@NoArgsConstructor
@Getter
public class Seat extends BaseEntity {

    public Seat(Screen screen) {

        if (screen == null) {

            throw new IllegalArgumentException("상영관 정보는 비워둘 수 없습니다.");
        }

        this.screen = screen;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_grade_id", nullable = false)
    private SeatGrade seatGrade;

    @Column(name = "seat_row", nullable = false)
    private int row;

    @Column(name = "seat_column", nullable = false)
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

    public void updateSeatStatus(String seatStatus) {

        if (seatStatus == null || seatStatus.isBlank()) {

            throw new IllegalArgumentException("좌석상태는 비워둘 수 없습니다.");
        }

        try {

            this.seatStatus = SeatStatus.valueOf(seatStatus);
        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException("잘못된 형식의 좌석 상태코드입니다: " + seatStatus);
        }
    }

    public String getSeatCode() {

        return String.valueOf((char)(row + 64)) + column;
    }
}
