/***
 * 좌석등급(SeatGrade) 관련 버그 회귀 테스트를 담당하는 클래스
 * 작성이력
 * -2026-10-20: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.TestcontainersConfiguration;
import com.cinema.backend.domain.screen.dto.SeatGradeResponse;
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
class SeatGradeRegressionTest {

    @Autowired
    private SeatGradeService seatGradeService;

    @Test
    @DisplayName("앞뒤 공백을 제거한 이름으로 중복 검사를 수행한다")
    void nameTrimmedBeforeDuplicateCheck_rejected() {

        seatGradeService.createSeatGrade("VIP", 10000, null);

        assertThatThrownBy(() -> seatGradeService.createSeatGrade(" VIP ", 10000, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 좌석등급명");
    }

    // 이 시나리오는 애플리케이션 코드가 아니라 MySQL의 기본 정렬 규칙(대소문자 구분 없음) 동작을 검증한다 (decisions.md D-28 참고)
    @Test
    @DisplayName("대소문자만 다른 이름도 중복으로 처리된다")
    void nameDifferingOnlyByCase_treatedAsDuplicate() {

        seatGradeService.createSeatGrade("VIP", 10000, null);

        assertThatThrownBy(() -> seatGradeService.createSeatGrade("vip", 10000, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 좌석등급명");
    }

    @Test
    @DisplayName("이름을 그대로 유지한 수정은 자기 자신과 충돌하지 않는다")
    void updateKeepingSameName_doesNotConflictWithSelf() {

        SeatGradeResponse created = seatGradeService.createSeatGrade("VIP", 10000, null);

        SeatGradeResponse response = seatGradeService.updateSeatGrade(
                created.getId(),
                "VIP",
                15000,
                "ACTIVE"
        );

        assertThat(response.getName()).isEqualTo("VIP");
        assertThat(response.getBasePrice()).isEqualTo(15000);
    }

    @Test
    @DisplayName("다른 등급이 사용 중인 이름으로 수정하면 거부된다")
    void updateToNameUsedByAnotherGrade_rejected() {

        seatGradeService.createSeatGrade("VIP", 10000, null);
        SeatGradeResponse normal = seatGradeService.createSeatGrade("일반", 8000, null);

        assertThatThrownBy(() -> seatGradeService.updateSeatGrade(
                normal.getId(),
                "VIP",
                8000,
                "ACTIVE"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용중인 좌석등급명");
    }
}
