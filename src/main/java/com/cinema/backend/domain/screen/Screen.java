/***
 * 상영관 정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-01: 파일 생성 및 작성
 ***/

package com.cinema.backend.domain.screen;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "screens")
@NoArgsConstructor
@Getter
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActiveStatus activeStatus = ActiveStatus.CLOSED;

    public enum ActiveStatus {

        ACTIVE, MAINTENANCE, CLOSED;
    }

    public void updateName(String name) {

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException("상영관명은 비워둘 수 없습니다.");
        }
        this.name = name;
    }

    public void updateActiveStatus(ActiveStatus activeStatus) {

        if (activeStatus == null) {

            throw new IllegalArgumentException("상영관 상태코드는 비워둘 수 없습니다.");
        }
        this.activeStatus = activeStatus;
    }
}
