/***
 * 영화 정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-02: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 ***/
package com.cinema.backend.domain.movie;

import com.cinema.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "movies")
@NoArgsConstructor
@Getter
public class Movie extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column
    private LocalDate releaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovieStatus movieStatus = MovieStatus.UPCOMING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisplayStatus displayStatus = DisplayStatus.DEACTIVATED;

    @Enumerated(EnumType.STRING)
    @Column
    private MovieRating movieRating;

    public enum MovieStatus {

        UPCOMING, SHOWING, ENDED
    }

    public enum DisplayStatus {

        ACTIVE, DEACTIVATED
    }

    public enum MovieRating {

        ALL, OVER_12, OVER_15, OVER_19, RESTRICTED
    }

    public void updateName(String name) {

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException("영화명은 비워둘 수 없습니다.");
        }
        this.name = name;
    }

    public void updateReleaseDate(LocalDate releaseDate) {

        this.releaseDate = releaseDate;
    }

    public void updateMovieStatus(MovieStatus movieStatus) {

        if (movieStatus == null) {

            throw new IllegalArgumentException("영화 상태코드는 비워둘 수 없습니다.");
        }
        this.movieStatus = movieStatus;
    }

    public void updateDisplayStatus(DisplayStatus displayStatus) {

        if (displayStatus == null) {

            throw new IllegalArgumentException("노출 상태코드는 비워둘 수 없습니다.");
        }
        this.displayStatus = displayStatus;
    }

    public void updateMovieRating(MovieRating movieRating) {

        this.movieRating = movieRating;
    }
}
