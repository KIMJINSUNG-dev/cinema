/***
 * 상영정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-03: 파일 생성 및 작성
 * -2026-09-03: BaseEntity 상속 추가
 * -2026-10-08: 외부에서 기본 생성자로 빈 엔티티를 생성하지 못하도록 @NoArgsConstructor를 protected로 제한
 * -2026-10-14: updateStartTime(), updateEndTime() 메서드를 updateSchedule()로 통합 리워크,
 *              리워크 과정에서 스태틱 메서드 calcEndTime() 메서드 추가
 * -2026-10-14: updateScreen(), updateMovie() 메서드 논리를 생성자 내부로 이관 및 기존 메서드 삭제
 * -2026-10-14: 활성화 여부를 boolean에서 enum으로 변경
 * -2026-10-15: updateShowtimeStatus()에 ACTIVE로 변환 시 검사 조건 추가
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.domain.movie.Movie;
import com.cinema.backend.domain.screen.Screen;
import com.cinema.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "showtimes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Showtime extends BaseEntity {

    public Showtime (Movie movie, Screen screen, LocalDateTime startTime) {

        if (screen == null) {

            throw new IllegalArgumentException("상영관은 비워둘 수 없습니다.");
        }

        if (screen.getActiveStatus() != Screen.ActiveStatus.ACTIVE) {

            throw new IllegalArgumentException("상영관이 비활성화 상태입니다.");
        }

        if (movie == null) {

            throw new IllegalArgumentException("영화는 비워둘 수 없습니다.");
        }

        if (movie.getMovieStatus() == Movie.MovieStatus.ENDED) {

            throw new IllegalArgumentException("상영종료된 영화는 등록할 수 없습니다.");
        }

        this.screen = screen;
        this.movie = movie;
        updateSchedule(startTime);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShowtimeStatus showtimeStatus = ShowtimeStatus.DEACTIVATED;

    public enum ShowtimeStatus {

        ACTIVE, DEACTIVATED
    }

    public void updateSchedule(LocalDateTime startTime) {

        if (startTime == null) {

            throw new IllegalArgumentException("상영시작시각은 비워둘 수 없습니다.");
        }

        if (startTime.isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException("상영시작시각은 과거로 설정할 수 없습니다.");
        }

        LocalDateTime endTime = calcEndTime(startTime, this.movie);

        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void updateShowtimeStatus(String showtimeStatus) {

        if (showtimeStatus == null || showtimeStatus.isBlank()) {

            throw new IllegalArgumentException("상영정보상태는 비워둘 수 없습니다.");
        }

        ShowtimeStatus status;

        try {

            status = ShowtimeStatus.valueOf(showtimeStatus);
        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException("잘못된 형식의 상영정보 상태코드입니다: " + showtimeStatus);
        }

        if (status == ShowtimeStatus.ACTIVE) {

            if (movie.getDisplayStatus() != Movie.DisplayStatus.ACTIVE) {

                throw new IllegalArgumentException("비공개상태인 영화의 상영정보는 활성화할 수 없습니다.");
            }

            if (screen.getActiveStatus() != Screen.ActiveStatus.ACTIVE) {

                throw new IllegalArgumentException("비활성화상태인 상영관의 상영정보는 활성화할 수 없습니다.");
            }

            if (movie.getMovieStatus() == Movie.MovieStatus.ENDED) {

                throw new IllegalArgumentException("상영종료된 영화의 상영정보는 활성화할 수 없습니다.");
            }

            if (startTime.isBefore(LocalDateTime.now())) {

                throw new IllegalArgumentException("시작시각이 지난 상영정보는 활성화할 수 없습니다.");
            }
        }

        this.showtimeStatus = status;
    }

    public static LocalDateTime calcEndTime(LocalDateTime startTime, Movie movie) {

        if (startTime == null) {

            throw new IllegalArgumentException("상영시작시각은 비워둘 수 없습니다.");
        }

        if (movie == null) {

            throw new IllegalArgumentException("영화는 비워둘 수 없습니다.");
        }

        if (movie.getRunningTime() == null) {

            throw new IllegalArgumentException("러닝타임이 없는 영화는 등록할 수 없습니다.");
        }
        return startTime.plusMinutes(movie.getRunningTime());
    }
}
