/***
 * Movie.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-09-08: 검색 기능 구현을 위한 findByNameContainingIgnoreCaseAndDisplayStatus 메서드 작성
 ***/
package com.cinema.backend.domain.movie;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    Page<Movie> findByNameContainingIgnoreCaseAndDisplayStatus(String name, Movie.DisplayStatus displayStatus, Pageable pageable);
}
