/***
 * 예약 동시성 테스트를 담당하는 클래스
 * 작성이력
 * -2026-10-28: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.reservation;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.member.MemberRepository;
import com.cinema.backend.domain.member.MemberService;
import com.cinema.backend.domain.movie.MovieRepository;
import com.cinema.backend.domain.movie.MovieService;
import com.cinema.backend.domain.screen.*;
import com.cinema.backend.domain.showtime.ShowtimeRepository;
import com.cinema.backend.domain.showtime.ShowtimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// 동시성 테스트: 각 스레드가 실제로 커밋해야 하므로 @Transactional을 쓰지 않음
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReservationConcurrencyTest {

    private static final int THREAD_COUNT = 10;

    @Autowired private ReservationService reservationService;
    @Autowired private MovieService movieService;
    @Autowired private ScreenService screenService;
    @Autowired private SeatGradeService seatGradeService;
    @Autowired private SeatService seatService;
    @Autowired private ShowtimeService showtimeService;
    @Autowired private MemberService memberService;

    @Autowired private ReservationSeatRepository reservationSeatRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private SeatGradeRepository seatGradeRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private MemberRepository memberRepository;


    private Long showtimeId;
    private Long seatId;
    private final List<String> emails = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();

    @BeforeEach
    void setUp() {

        // 영화: 공개(ACTIVE) · 상영 중 (이후 예약 조건 검사가 들어가도 통과하도록 실제 운영 상태로 준비)
        Long movieId = movieService.createMovie("동시성 테스트 영화", null, null, null, 120).getId();
        movieService.updateMovie(movieId, "동시성 테스트 영화", null, "SHOWING", "ACTIVE", null, null, 120);

        // 상영관: 생성 시 CLOSED이므로 ACTIVE로 변경
        Long screenId = screenService.createScreen("동시성 테스트관").getId();
        screenService.updateScreen(screenId, "동시성 테스트관", "ACTIVE");

        // 좌석 등급과 좌석 A1
        Long gradeId = seatGradeService.createSeatGrade("동시성 테스트 등급", 10000, null).getId();
        seatId = seatService.createSeat(screenId, gradeId, "A", 1, null).getId();

        // 상영 일정: 2099년(D-37), 활성화
        showtimeId = showtimeService.createShowtime(movieId, screenId, LocalDateTime.of(2099, 1, 1, 13, 0)).getId();
        showtimeService.updateShowtimeStatus(showtimeId, "ACTIVE");

        // 회원 10명
        for (int i = 0; i < THREAD_COUNT; i++) {

            String email = "concurrency" + i + "@test.com";
            memberIds.add(memberService.createMember(email, "password1234", "동시성회원" + i).getId());
            emails.add(email);
        }
    }

    @AfterEach
    void tearDown() {

        // 외래 키 순서대로 삭제 (자식 → 부모)
        reservationSeatRepository.deleteAllInBatch();
        reservationRepository.deleteAllInBatch();
        showtimeRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        seatGradeRepository.deleteAllInBatch();
        screenRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();
        // 회원은 이 테스트가 만든 회원만 삭제 (현장 판매 시스템 계정 등 다른 데이터 보호)
        memberRepository.deleteAllByIdInBatch(memberIds);
    }

    @Test
    @DisplayName("같은 좌석에 10명이 동시에 예약하면 1명만 성공하고 나머지는 모두 충돌(409)로 거부")
    void concurrentReservation_onlyOneSucceeds() throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT); // 모든 스레드가 준비될 때까지
        CountDownLatch start = new CountDownLatch(1);            // 출발 신호
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);  // 모든 스레드가 끝날 때까지

        AtomicInteger successCount = new AtomicInteger();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        for (String email : emails) {

            executor.submit(() -> {

                ready.countDown();
                try {

                    start.await(); // 출발 신호를 기다렸다가 동시에 요청
                    reservationService.createOnlineReservation(email, showtimeId, List.of(seatId));
                    successCount.incrementAndGet();
                } catch (Throwable e) {

                    failures.add(e);
                } finally {

                    done.countDown();
                }
            });
        }

        ready.await();     // 10개 스레드가 모두 출발선에 설 때까지 대기
        start.countDown(); // 출발
        boolean finished = done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failures).hasSize(THREAD_COUNT - 1);
        assertThat(failures)
                .extracting(e -> e.getClass().getSimpleName())
                .containsOnly("ConflictException");
        assertThat(reservationSeatRepository.countByShowtimeIdAndSeatIdAndActiveTrue(showtimeId, seatId))
                .isEqualTo(1);
    }
}
