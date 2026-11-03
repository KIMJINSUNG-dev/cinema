/***
 * MovieRepositoryCustom.java 인터페이스에서 구성한 커스텀 메서드를 구현하는 클래스
 * 작성이력
 * -2026-11-03: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.movie;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class MovieRepositoryCustomImpl implements MovieRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Movie> search(String keyword, Long genreId, Movie.MovieStatus movieStatus, Pageable pageable) {

        QMovie movie = QMovie.movie;

        List<Movie> content = queryFactory
                .selectFrom(movie)
                .where(
                        movie.displayStatus.eq(Movie.DisplayStatus.ACTIVE),
                        keywordContains(keyword),
                        genreIdEq(genreId),
                        movieStatusEq(movieStatus)
                )
                .orderBy(movie.createdAt.desc(), movie.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(movie.count())
                .from(movie)
                .where(
                        movie.displayStatus.eq(Movie.DisplayStatus.ACTIVE),
                        keywordContains(keyword),
                        genreIdEq(genreId),
                        movieStatusEq(movieStatus)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression keywordContains(String keyword) {

        return StringUtils.hasText(keyword) ? QMovie.movie.name.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression genreIdEq(Long genreId) {

        QMovieGenre movieGenre = QMovieGenre.movieGenre;

        return genreId != null
                ? QMovie.movie.id.in(
                        JPAExpressions
                                .select(movieGenre.movie.id)
                                .from(movieGenre)
                                .where(movieGenre.genre.id.eq(genreId))
                )
                : null;
    }

    private BooleanExpression movieStatusEq(Movie.MovieStatus movieStatus) {

        return movieStatus != null ? QMovie.movie.movieStatus.eq(movieStatus) : null;
    }


}
