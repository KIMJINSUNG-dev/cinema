/***
 * 좌석(Seat) 관련 버그 회귀 테스트를 담당하는 클래스
 * 작성이력
 * -2026-10-20: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.screen.dto.SeatGradeResponse;
import com.cinema.backend.domain.screen.dto.SeatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class SeatRegressionTest {

    @Autowired
    private SeatService seatService;

    @Autowired
    private ScreenService screenService;

    @Autowired
    private SeatGradeService seatGradeService;

    private Long screen1Id;
    private Long screen2Id;
    private Long activeGradeId;

    @BeforeEach
    void setup() {

        screen1Id = screenService.createScreen("테스트 1관").getId();
        screen2Id = screenService.createScreen("테스트 2관").getId();
        activeGradeId = seatGradeService.createSeatGrade("일반", 10000, null).getId();
    }

    @Test
    @DisplayName("같은 상영관에 같은 좌석코드를 중복 등록하면 거부된다")
    void duplicateSeatCodeInSameScreen_rejected() {

        seatService.createSeat(screen1Id, activeGradeId, "A", 1, null);

        assertThatThrownBy(() -> seatService.createSeat(screen1Id, activeGradeId, "A", 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 좌석코드");
    }

    @Test
    @DisplayName("다른 상영관에는 같은 좌석코드를 등록할 수 있다")
    void sameSeatCodeInDifferentScreen_allowed() {

        seatService.createSeat(screen1Id, activeGradeId, "A", 1, null);

        SeatResponse response = seatService.createSeat(screen2Id, activeGradeId, "A", 1, null);

        assertThat(response.getSeatCode()).isEqualTo("A1");
        assertThat(response.getScreenId()).isEqualTo(screen2Id);
    }

    @Test
    @DisplayName("위치를 바꾸지 않는 수정은 자기 자신과 충돌하지 않는다 (과거 자동 flush 버그 회귀 방지)")
    void updateWithoutChangingPosition_doesNotConflictWithSelf() {

        SeatResponse created = seatService.createSeat(screen1Id, activeGradeId, "A", 1, null);

        SeatResponse response = seatService.updateSeat(
                screen1Id,
                created.getId(),
                "A",
                1,
                activeGradeId,
                "DEACTIVATED"
        );

        assertThat(response.getSeatCode()).isEqualTo("A1");
        assertThat(response.getSeatStatus()).isEqualTo(Seat.SeatStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("이미 사용 중인 위치로 수정하면 거부된다")
    void updateToPositionAlreadyInUse_rejected() {

        SeatResponse seatA1 = seatService.createSeat(screen1Id, activeGradeId, "A", 1, null);
        seatService.createSeat(screen1Id, activeGradeId, "A", 2, null);

        assertThatThrownBy(() -> seatService.updateSeat(
                screen1Id,
                seatA1.getId(),
                "A",
                2,
                activeGradeId,
                "ACTIVE"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 좌석코드");
    }

    @Test
    @DisplayName("소문자 행 코드도 대문자로 변환한 뒤 중복 검사를 수행한다 (과거 변환 전 값으로 검사하던 버그 회귀 방지)")
    void lowercaseRowCode_isConvertedBeforeDuplicateCheck() {

        seatService.createSeat(screen1Id, activeGradeId, "A", 1, null);

        assertThatThrownBy(() -> seatService.createSeat(screen1Id, activeGradeId, "a", 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 좌석코드");
    }

    @Test
    @DisplayName("비활성화된 좌석등급으로는 새 좌석을 등록할 수 없다")
    void createSeatWithDeactivatedGrade_rejected() {

        Long deactivatedGradeId = seatGradeService.createSeatGrade("임시등급", 5000, null).getId();
        seatGradeService.updateSeatGrade(deactivatedGradeId, "임시등급", 5000, "DEACTIVATED");

        assertThatThrownBy(() -> seatService.createSeat(screen1Id, deactivatedGradeId, "A", 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용할 수 없는 좌석등급");
    }

    @Test
    @DisplayName("좌석등급이 비활성화되어도 그 등급을 유지한 수정은 허용된다")
    void updateKeepingSameNowDeactivatedGrade_allowed() {

        SeatGradeResponse grade = seatGradeService.createSeatGrade("G등급", 8000, null);
        SeatResponse seat = seatService.createSeat(screen1Id, grade.getId(), "A", 1, null);

        seatGradeService.updateSeatGrade(grade.getId(), "G등급", 8000, "DEACTIVATED");

        SeatResponse response = seatService.updateSeat(
                screen1Id,
                seat.getId(),
                "A",
                1,
                grade.getId(),
                "ACTIVE"
        );

        assertThat(response.getSeatGradeId()).isEqualTo(grade.getId());
    }

    @Test
    @DisplayName("다른 비활성화된 좌석등급으로 변경하는 수정은 거부된다")
    void updateToAnotherDeactivatedGrade_rejected() {

        Long activeGrade1Id = seatGradeService.createSeatGrade("G1등급", 9000, null).getId();
        SeatResponse seat = seatService.createSeat(screen1Id, activeGrade1Id, "A", 1, null);

        Long grade2Id = seatGradeService.createSeatGrade("G2등급", 7000, null).getId();
        seatGradeService.updateSeatGrade(grade2Id, "G2등급", 7000, "DEACTIVATED");

        assertThatThrownBy(() -> seatService.updateSeat(
                screen1Id,
                seat.getId(),
                "A",
                1,
                grade2Id,
                "ACTIVE"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용할 수 없는 좌석등급");
    }
}
