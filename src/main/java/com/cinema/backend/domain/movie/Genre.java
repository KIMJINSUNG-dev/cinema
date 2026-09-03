/***
 * 영화장르 정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-02: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.movie;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "genres")
@NoArgsConstructor
@Getter
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public void updateName(String name) {

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException("장르명은 비워둘 수 없습니다.");
        }
        this.name = name;
    }
}
