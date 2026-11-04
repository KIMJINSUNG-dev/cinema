/***
 * ShowtimeRepositoryCustom.java 인터페이스에서 구성한 커스텀 메서드를 구현하는 클래스
 * 작성이력
 * -2026-11-04: 파일 생성 및 작성 (MovieRepositoryCustomImpl.java 구조 참고)
 *
 * findTimetable()은 고객용 상영시간표 조회에 쓰인다. 영화 · 상영관을 join + fetchJoin으로
 * 한 번에 조회해 목록을 순회할 때 지연 로딩으로 인한 N+1이 생기지 않는다.
 * 현재 시각은 호출부(ShowtimeService)에서 구해 파라미터로 받는다 (리포지토리에서
 * LocalDateTime.now()를 직접 호출하지 않음 — 테스트에서 now를 주입해 검증할 수 있도록 함).
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.domain.movie.Movie;
import com.cinema.backend.domain.movie.QMovie;
import com.cinema.backend.domain.screen.QScreen;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ShowtimeRepositoryCustomImpl implements ShowtimeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Showtime> findTimetable(LocalDate date, Long movieId, Long screenId, LocalDateTime now) {

        QShowtime showtime = QShowtime.showtime;
        QMovie movie = QMovie.movie;
        QScreen screen = QScreen.screen;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);

        return queryFactory
                .selectFrom(showtime)
                .join(showtime.movie, movie).fetchJoin()
                .join(showtime.screen, screen).fetchJoin()
                .where(
                        showtime.showtimeStatus.eq(Showtime.ShowtimeStatus.ACTIVE),
                        movie.displayStatus.eq(Movie.DisplayStatus.ACTIVE),
                        showtime.startTime.gt(now),
                        showtime.startTime.goe(startOfDay),
                        showtime.startTime.lt(startOfNextDay),
                        movieIdEq(movieId),
                        screenIdEq(screenId)
                )
                .orderBy(movie.name.asc(), showtime.startTime.asc(), showtime.id.asc())
                .fetch();
    }

    private BooleanExpression movieIdEq(Long movieId) {

        return movieId != null ? QShowtime.showtime.movie.id.eq(movieId) : null;
    }

    private BooleanExpression screenIdEq(Long screenId) {

        return screenId != null ? QShowtime.showtime.screen.id.eq(screenId) : null;
    }
}
