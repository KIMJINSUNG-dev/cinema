/***
 * 좌석등급 정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-01: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seat_grades")
@NoArgsConstructor
@Getter
public class SeatGrade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false)
    private int basePrice = 0;

    public void updateName(String name) {

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException("등급명은 비워둘 수 없습니다.");
        }
        this.name = name;
    }

    public void updateBasePrice(int basePrice) {

        if (basePrice < 0) {

            throw new IllegalArgumentException("올바른 가격을 입력해주세요.");
        }
        this.basePrice = basePrice;
    }
}
