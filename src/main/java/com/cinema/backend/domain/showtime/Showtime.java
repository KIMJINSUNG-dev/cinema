/***
 * 상영정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-03: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.domain.movie.Movie;
import com.cinema.backend.domain.screen.Screen;
import com.cinema.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "showtimes")
@NoArgsConstructor
@Getter
public class Showtime extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;
    
    @Column(nullable = false)
    private Boolean isActive = false;

    public void updateScreen(Screen screen) {

        if (screen == null) {

            throw new IllegalArgumentException("상영관은 비워둘 수 없습니다.");
        }
        this.screen = screen;
    }

    public void updateMovie(Movie movie) {

        if (movie == null) {

            throw new IllegalArgumentException("영화는 비워둘 수 없습니다.");
        }
        this.movie = movie;
    }

    public void updateStartTime(LocalDateTime startTime) {

        if (startTime == null) {

            throw new IllegalArgumentException("상영시작시각은 비워둘 수 없습니다.");
        }

        if (this.endTime != null && startTime.isAfter(this.endTime)) {

            throw new IllegalArgumentException("상영시작시각은 상영종료시각 이전이어야 합니다.");
        }
        this.startTime = startTime;
    }

    public void updateEndTime(LocalDateTime endTime) {

        if (endTime == null) {

            throw new IllegalArgumentException("상영종료시각은 비워둘 수 없습니다.");
        }

        if (this.startTime != null && endTime.isBefore(this.startTime)) {

            throw new IllegalArgumentException("상영종료시각은 상영시작시각 이후여야 합니다.");
        }
        this.endTime = endTime;
    }
    
    public void updateIsActive(Boolean isActive) {
        
        if (isActive == null) {
            
            throw new IllegalArgumentException("상영정보 활성화 여부는 비워둘 수 없습니다.");
        }
        this.isActive = isActive;
    }
}
