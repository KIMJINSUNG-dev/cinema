/***
 * 프론트엔드의 API 요청에 따라 MovieService.java의 서비스 메서드를 호출하는 컨트롤러 클래스
 * 작성이력
 * -2026-09-08: 파일 생성 및 작성
 * -2026-09-09: 영화에 종속되는 장르 구현 작업
 ***/
package com.cinema.backend.domain.movie;

import com.cinema.backend.domain.movie.dto.MovieCreateRequest;
import com.cinema.backend.domain.movie.dto.MovieResponse;
import com.cinema.backend.domain.movie.dto.MovieUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(
            @RequestBody MovieCreateRequest request
    ) {

        MovieResponse response = movieService.createMovie(
                request.getName(),
                request.getReleaseDate(),
                request.getMovieRating(),
                request.getGenreIds()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<MovieResponse>> searchMovies(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        if (keyword != null && !keyword.isBlank()) {

            return ResponseEntity.ok(movieService.findByName(keyword, pageable));
        }

        return ResponseEntity.ok(movieService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovie(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(movieService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Long id,
            @RequestBody MovieUpdateRequest request
    ) {

        return ResponseEntity.ok(
                movieService.updateMovie(
                        id,
                        request.getName(),
                        request.getReleaseDate(),
                        request.getMovieStatus(),
                        request.getDisplayStatus(),
                        request.getMovieRating(),
                        request.getGenreIds()
                )
        );
    }
}
