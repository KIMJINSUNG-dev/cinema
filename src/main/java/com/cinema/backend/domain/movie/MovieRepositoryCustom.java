/***
 * MovieRepository.java 리포지토리에서 사용할 커스텀 메서드 구성을 담당하는 인터페이스
 * 작성이력
 * -2026-11-03: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.movie;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovieRepositoryCustom {

    Page<Movie> search(String keyword, Long genreId, Movie.MovieStatus movieStatus, Pageable pageable);
}
