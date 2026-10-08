/***
 * 상영관 정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-01: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 * -2026-10-08: 외부에서 기본 생성자로 빈 엔티티를 생성하지 못하도록 @NoArgsConstructor를 protected로 제한
 ***/

package com.cinema.backend.domain.screen;

import com.cinema.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "screens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Screen extends BaseEntity {

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
