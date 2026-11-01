/***
 * 상영 일정 등록 경합과 상영 일정 변경 제한(D-44)의 동시성 테스트를 담당하는 클래스
 * 작성이력
 * -2026-11-01: 파일 생성 및 작성 (ReservationConcurrencyTest.java 구조 참고)
 ***/
package com.cinema.backend.domain.showtime;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.member.MemberRepository;
import com.cinema.backend.domain.member.MemberService;
import com.cinema.backend.domain.movie.MovieRepository;
import com.cinema.backend.domain.movie.MovieService;
import com.cinema.backend.domain.reservation.ReservationRepository;
import com.cinema.backend.domain.reservation.ReservationSeatRepository;
import com.cinema.backend.domain.reservation.ReservationService;
import com.cinema.backend.domain.screen.*;
import com.cinema.backend.global.exception.ConflictException;
import org.junit.jupiter.api.AfterEach;
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

// 동시성 테스트: 각 스레드 · 요청이 실제로 커밋해야 하므로 @Transactional을 쓰지 않음 (ReservationConcurrencyTest와 같은 이유)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ShowtimeConcurrencyTest {

    private static final int THREAD_COUNT = 5;

    @Autowired private ShowtimeService showtimeService;
    @Autowired private ReservationService reservationService;
    @Autowired private MovieService movieService;
    @Autowired private ScreenService screenService;
    @Autowired private SeatGradeService seatGradeService;
    @Autowired private SeatService seatService;
    @Autowired private MemberService memberService;

    @Autowired private ReservationSeatRepository reservationSeatRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private SeatGradeRepository seatGradeRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private MemberRepository memberRepository;

    // 이 테스트가 만든 회원만 정리 대상으로 추적 (현장 판매 시스템 계정 등 다른 데이터 보호)
    private final List<Long> memberIds = new ArrayList<>();

    @AfterEach
    void tearDown() {

        // 외래 키 순서대로 삭제 (자식 → 부모), ReservationConcurrencyTest와 같은 방식
        reservationSeatRepository.deleteAllInBatch();
        reservationRepository.deleteAllInBatch();
        showtimeRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        seatGradeRepository.deleteAllInBatch();
        screenRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();
        memberRepository.deleteAllByIdInBatch(memberIds);
    }

    @Test
    @DisplayName("같은 상영관 · 겹치는 시간의 상영 일정을 5개 스레드가 동시에 등록하면 1건만 성공하고 나머지는 겹침(400)으로 거부된다")
    void concurrentCreateShowtime_onlyOneSucceeds() throws InterruptedException {

        Long movieId = movieService.createMovie("등록경합 테스트 영화", null, null, null, 120).getId();
        movieService.updateMovie(movieId, "등록경합 테스트 영화", null, "SHOWING", "ACTIVE", null, null, 120);

        Long screenId = screenService.createScreen("등록경합 테스트관").getId();
        screenService.updateScreen(screenId, "등록경합 테스트관", "ACTIVE");

        LocalDateTime startTime = LocalDateTime.of(2099, 1, 1, 13, 0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT); // 모든 스레드가 준비될 때까지
        CountDownLatch start = new CountDownLatch(1);            // 출발 신호
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);  // 모든 스레드가 끝날 때까지

        AtomicInteger successCount = new AtomicInteger();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < THREAD_COUNT; i++) {

            executor.submit(() -> {

                ready.countDown();
                try {

                    start.await(); // 출발 신호를 기다렸다가 동시에 등록 요청
                    showtimeService.createShowtime(movieId, screenId, startTime);
                    successCount.incrementAndGet();
                } catch (Throwable e) {

                    failures.add(e);
                } finally {

                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        boolean finished = done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failures).hasSize(THREAD_COUNT - 1);
        assertThat(failures)
                .extracting(e -> e.getClass().getSimpleName())
                .containsOnly("IllegalArgumentException");
        assertThat(failures)
                .allSatisfy(e -> assertThat(e).hasMessageContaining("겹칩니다"));
        assertThat(showtimeService.findByScreenId(screenId)).hasSize(1);
    }

    @Test
    @DisplayName("같은 상영 일정에 예약 1건과 비활성화를 동시에 요청하면 한쪽만 성공한다")
    void concurrentReservationAndDeactivate_onlyOneSucceeds() throws InterruptedException {

        Long movieId = movieService.createMovie("변경제한 동시성 테스트 영화", null, null, null, 120).getId();
        movieService.updateMovie(movieId, "변경제한 동시성 테스트 영화", null, "SHOWING", "ACTIVE", null, null, 120);

        Long screenId = screenService.createScreen("변경제한 동시성 테스트관").getId();
        screenService.updateScreen(screenId, "변경제한 동시성 테스트관", "ACTIVE");

        Long gradeId = seatGradeService.createSeatGrade("변경제한 동시성 테스트 등급", 10000, null).getId();
        Long seatId = seatService.createSeat(screenId, gradeId, "A", 1, null).getId();

        Long showtimeId = showtimeService.createShowtime(movieId, screenId, LocalDateTime.of(2099, 1, 1, 13, 0)).getId();
        showtimeService.updateShowtimeStatus(showtimeId, "ACTIVE");

        String email = "showtime-concurrency@test.com";
        memberIds.add(memberService.createMember(email, "password1234", "변경제한동시성회원").getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        AtomicInteger reservationSuccess = new AtomicInteger();
        AtomicInteger deactivateSuccess = new AtomicInteger();
        List<Throwable> reservationFailures = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> deactivateFailures = Collections.synchronizedList(new ArrayList<>());

        executor.submit(() -> {

            ready.countDown();
            try {

                start.await();
                reservationService.createOnlineReservation(email, showtimeId, List.of(seatId));
                reservationSuccess.incrementAndGet();
            } catch (Throwable e) {

                reservationFailures.add(e);
            } finally {

                done.countDown();
            }
        });

        executor.submit(() -> {

            ready.countDown();
            try {

                start.await();
                showtimeService.updateShowtimeStatus(showtimeId, "DEACTIVATED");
                deactivateSuccess.incrementAndGet();
            } catch (Throwable e) {

                deactivateFailures.add(e);
            } finally {

                done.countDown();
            }
        });

        ready.await();
        start.countDown();
        boolean finished = done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();

        // 둘 다 성공하는 경우는 없어야 한다: 상영 일정 락(D-44)이 두 요청을 한 줄로 세워, 먼저 커밋한 쪽만 성공한다
        assertThat(reservationSuccess.get() + deactivateSuccess.get()).isEqualTo(1);

        if (reservationSuccess.get() == 1) {

            // 예약이 먼저 커밋된 경우: 비활성화는 활성 예약을 발견해 거부되고, 상영 일정은 ACTIVE로 남는다
            assertThat(deactivateFailures).hasSize(1);
            assertThat(deactivateFailures.get(0))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("예매된 좌석이 있는 상영정보는 비활성화할 수 없습니다.");
            assertThat(showtimeService.findById(showtimeId).getShowtimeStatus())
                    .isEqualTo(Showtime.ShowtimeStatus.ACTIVE);
        } else {

            // 비활성화가 먼저 커밋된 경우: 예약은 비활성 상영정보라 거부되고, 활성 예약 좌석은 0개로 남는다
            assertThat(reservationFailures).hasSize(1);
            assertThat(reservationFailures.get(0))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("예매할 수 없는 상영정보입니다.");
            assertThat(reservationSeatRepository.existsByShowtimeIdAndActiveTrue(showtimeId)).isFalse();
        }
    }
}
