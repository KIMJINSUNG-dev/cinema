/***
 * Movie.java 엔티티에 대한 데이터 접근을 담당하는 인터페이스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 * -2026-09-08: 검색 기능 구현을 위한 findByNameContainingIgnoreCaseAndDisplayStatus 메서드 작성()
 *              (2026-11-03: MovieRepositoryCustom.search() 메서드로 대체함에 따라 삭제)
 * -2026-11-03: QueryDSL 적용에 따른 MovieRepositoryCustom 인터페이스 상속,
 *              MovieRepositoryCustom.search() 메서드가 검색 역할을 대체함에 따라
 *              기존의 findByNameContainingIgnoreCaseAndDisplayStatus 메서드는 더 이상 사용되지 않으므로 삭제
 ***/
package com.cinema.backend.domain.movie;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long>, MovieRepositoryCustom {

}
