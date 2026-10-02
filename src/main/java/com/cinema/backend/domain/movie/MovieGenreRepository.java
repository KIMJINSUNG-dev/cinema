/***
 * MovieGenre.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-09-09: 영화에 종속되는 장르 구현을 위한 findByMovieId 메서드 작성
 ***/
package com.cinema.backend.domain.movie;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieGenreRepository extends JpaRepository<MovieGenre, Long> {

    List<MovieGenre> findByMovieId(Long id);
}
