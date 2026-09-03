/***
 * 영화에 할당될 장르 목록 정보를 관리하는 엔티티 클래스
 * 작성이력
 * -2026-09-02: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.movie;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "movie_genres", uniqueConstraints = @UniqueConstraint(columnNames = {"movie_id", "genre_id"}))
@NoArgsConstructor
@Getter
public class MovieGenre {

    public MovieGenre(Movie movie, Genre genre) {

        if (movie == null) {

            throw new IllegalArgumentException("연결할 영화 정보가 없습니다.");
        }
        if (genre == null) {

            throw new IllegalArgumentException("연결할 장르 정보가 없습니다.");
        }
        this.movie = movie;
        this.genre = genre;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id")
    private Genre genre;
}
