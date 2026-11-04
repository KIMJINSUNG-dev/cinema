/***
 * MovieGenre.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-09-09: 영화에 종속되는 장르 구현을 위한 findByMovieId 메서드 작성
 * -2026-11-04: N+1 문제를 해결하기 위해 id IN 일괄 조회 + 장르 fetch join 메서드 findAllWithGenreByMovieIdIn 추가
 ***/
package com.cinema.backend.domain.movie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieGenreRepository extends JpaRepository<MovieGenre, Long> {

    List<MovieGenre> findByMovieId(Long id);

    @Query("select mg from MovieGenre mg join fetch mg.genre where mg.movie.id in :movieIds")
    List<MovieGenre> findAllWithGenreByMovieIdIn(@Param("movieIds") List<Long> movieIds);
}
