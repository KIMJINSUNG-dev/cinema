/***
 * 영화 정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-02: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 * -2026-09-08: 외부 접근 허용 메서드의 enum 파라미터를 String 기반으로 교체
 * -2026-10-08: 외부에서 기본 생성자로 빈 엔티티를 생성하지 못하도록 @NoArgsConstructor를 protected로 제한
 * -2026-10-14: runningTime 필드 및 updateRunningTime() 메서드 추가
 ***/
package com.cinema.backend.domain.movie;

import com.cinema.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "movies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column
    private Integer runningTime;

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

    public void updateMovieStatus(String movieStatus) {

        if (movieStatus == null || movieStatus.isBlank()) {

            throw new IllegalArgumentException("영화 상태코드는 비워둘 수 없습니다.");
        }

        try {

            this.movieStatus = MovieStatus.valueOf(movieStatus);
        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException("잘못된 형식의 영화 상태코드입니다: " + movieStatus);
        }
    }

    public void updateDisplayStatus(String displayStatus) {

        if (displayStatus == null || displayStatus.isBlank()) {

            throw new IllegalArgumentException("노출 상태코드는 비워둘 수 없습니다.");
        }

        try {

            this.displayStatus = DisplayStatus.valueOf(displayStatus);
        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException("잘못된 형식의 노출 상태코드입니다: " + displayStatus);
        }
    }

    public void updateMovieRating(String movieRating) {

        if (movieRating == null || movieRating.isBlank()) {

            this.movieRating = null;
            return;
        }

        try {

            this.movieRating = MovieRating.valueOf(movieRating);
        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException("잘못된 형식의 영화 등급코드입니다: " + movieRating);
        }
    }

    public void updateRunningTime(Integer runningTime) {

        if (runningTime == null) {

            this.runningTime = null;
            return;
        }

        if (runningTime <= 0) {

            throw new IllegalArgumentException("러닝타임은 0분 이하일 수 없습니다.");
        }
        this.runningTime = runningTime;
    }
}
