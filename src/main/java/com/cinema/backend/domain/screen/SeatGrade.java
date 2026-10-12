/***
 * 좌석등급 정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-01: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 * -2026-10-08: 외부에서 기본 생성자로 빈 엔티티를 생성하지 못하도록 @NoArgsConstructor를 protected로 제한
 * -2026-10-12: public SeatGrade(String name, int basePrice) 생성자 추가
 * -2026-10-12: SeatGrade의 활성화/비활성화를 담당하는 enum/필드 및 update 메서드 추가
 * -2026-10-12: name 필드의 최대 길이 설정을 감안하여 updateName() 메서드에 방어 코드 추가
 * -2026-10-12: SeatGrade의 활성화 여부를 반환하는 isActive() 메서드 추가
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seat_grades")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SeatGrade extends BaseEntity {

    public SeatGrade(String name, int basePrice) {

        updateName(name);
        updateBasePrice(basePrice);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false)
    private int basePrice = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatGradeStatus seatGradeStatus = SeatGradeStatus.ACTIVE;

    public enum SeatGradeStatus {

        ACTIVE, DEACTIVATED
    }

    public void updateName(String name) {

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException("등급명은 비워둘 수 없습니다.");
        }

        if (name.trim().length() > 50) {

            throw new IllegalArgumentException("등급명은 최대 50자까지만 입력 가능합니다.");
        }

        this.name = name.trim();
    }

    public void updateBasePrice(int basePrice) {

        if (basePrice < 0) {

            throw new IllegalArgumentException("올바른 가격을 입력해주세요.");
        }
        this.basePrice = basePrice;
    }

    public void updateSeatGradeStatus(String seatGradeStatus) {

        if (seatGradeStatus == null || seatGradeStatus.isBlank()) {

            throw new IllegalArgumentException("좌석등급상태는 비워둘 수 없습니다.");
        }

        try {

            this.seatGradeStatus = SeatGradeStatus.valueOf(seatGradeStatus);
        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException("잘못된 형식의 좌석등급 상태코드입니다: " + seatGradeStatus);
        }
    }

    public boolean isActive() {

        if (getSeatGradeStatus() == SeatGrade.SeatGradeStatus.ACTIVE) {

            return true;
        }
        return false;
    }
}
