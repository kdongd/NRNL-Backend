package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.BodyPart;
import com.kdongdexample.norunnolifeexample.domain.CoachingAssessment;
import com.kdongdexample.norunnolifeexample.domain.CoachingLevel;
import com.kdongdexample.norunnolifeexample.domain.CoachingReason;
import com.kdongdexample.norunnolifeexample.domain.CoachingRule;
import com.kdongdexample.norunnolifeexample.domain.DailyCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CoachingRuleEngineTest {

    private final CoachingRuleEngine engine = new CoachingRuleEngine();
    private final LocalDate today = LocalDate.of(2026, 10, 12);

    private DailyCondition day(int daysAgo, int rpe, int body, Map<BodyPart, Integer> pains) {
        return new DailyCondition(today.minusDays(daysAgo), pains, rpe, body);
    }

    private DailyCondition normalDay(int daysAgo) {
        return day(daysAgo, 5, 7, Map.of());
    }

    private List<CoachingRule> rulesOf(CoachingAssessment assessment) {
        return assessment.reasons().stream().map(CoachingReason::rule).toList();
    }

    @Test
    @DisplayName("기록이 없으면 데이터 부족")
    void noData_insufficient() {
        CoachingAssessment result = engine.evaluate(List.of(), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.INSUFFICIENT_DATA);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    @DisplayName("입력일이 3개 미만이면 데이터 부족")
    void lessThanThreeDays_insufficient() {
        CoachingAssessment result = engine.evaluate(List.of(normalDay(0), normalDay(1)), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.INSUFFICIENT_DATA);
    }

    @Test
    @DisplayName("통증 5는 입력일이 하나뿐이어도 STOP")
    void severePain_withSingleDay_stop() {
        CoachingAssessment result = engine.evaluate(
                List.of(day(0, 5, 7, Map.of(BodyPart.KNEE, 5))), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.STOP);
        assertThat(rulesOf(result)).containsExactly(CoachingRule.SEVERE_PAIN);
    }

    @Test
    @DisplayName("통증 5라도 마지막 입력일이 3일보다 오래됐으면 통증 강도 룰을 적용하지 않는다")
    void severePain_notRecent_ignored() {
        CoachingAssessment result = engine.evaluate(
                List.of(day(3, 5, 7, Map.of(BodyPart.KNEE, 5))), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.INSUFFICIENT_DATA);
    }

    @Test
    @DisplayName("데이터가 충분하고 마지막 입력일 통증이 4면 REDUCE")
    void moderatePain_reduce() {
        CoachingAssessment result = engine.evaluate(List.of(
                day(0, 5, 7, Map.of(BodyPart.ANKLE, 4)),
                normalDay(1),
                normalDay(2)), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.REDUCE);
        assertThat(rulesOf(result)).containsExactly(CoachingRule.MODERATE_PAIN);
    }

    @Test
    @DisplayName("통증 4는 데이터가 부족하면 판정하지 않는다 (통증 5만 예외)")
    void moderatePain_withInsufficientData_insufficient() {
        CoachingAssessment result = engine.evaluate(
                List.of(day(0, 5, 7, Map.of(BodyPart.ANKLE, 4))), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.INSUFFICIENT_DATA);
    }

    @Test
    @DisplayName("최근 입력일 3개 모두 통증이 있으면 REST (달력상 연속이 아니어도 된다)")
    void painPersistent_rest() {
        CoachingAssessment result = engine.evaluate(List.of(
                day(0, 5, 7, Map.of(BodyPart.KNEE, 2)),
                day(2, 5, 7, Map.of(BodyPart.KNEE, 1)),
                day(5, 5, 7, Map.of(BodyPart.HIP, 1))), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.REST);
        assertThat(rulesOf(result)).containsExactly(CoachingRule.PAIN_PERSISTENT);
    }

    @Test
    @DisplayName("중간에 통증 없는 입력일이 있으면 통증 지속이 아니다")
    void painStreakBroken_normal() {
        CoachingAssessment result = engine.evaluate(List.of(
                day(0, 5, 7, Map.of(BodyPart.KNEE, 2)),
                normalDay(1),
                day(2, 5, 7, Map.of(BodyPart.KNEE, 2))), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.NORMAL);
    }

    @Test
    @DisplayName("최근 입력일 5개 중 3개 이상 RPE 9 이상이면 REDUCE")
    void highRpeRepeated_reduce() {
        CoachingAssessment result = engine.evaluate(List.of(
                day(0, 9, 7, Map.of()),
                day(1, 6, 7, Map.of()),
                day(2, 10, 7, Map.of()),
                day(3, 5, 7, Map.of()),
                day(4, 9, 7, Map.of())), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.REDUCE);
        assertThat(rulesOf(result)).containsExactly(CoachingRule.HIGH_RPE_REPEATED);
    }


    @Test
    @DisplayName("최근 입력일 3개 모두 몸 상태 3 이하면 REST")
    void lowBodyCondition_rest() {
        CoachingAssessment result = engine.evaluate(List.of(
                day(0, 5, 3, Map.of()),
                day(1, 5, 2, Map.of()),
                day(2, 5, 1, Map.of())), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.REST);
        assertThat(rulesOf(result)).containsExactly(CoachingRule.LOW_BODY_CONDITION);
    }

    @Test
    @DisplayName("여러 룰이 걸리면 가장 심한 단계가 결과가 되고 걸린 룰은 모두 이유로 남는다")
    void multipleRules_highestLevelWins() {
        CoachingAssessment result = engine.evaluate(List.of(
                day(0, 10, 7, Map.of(BodyPart.KNEE, 5)),
                day(1, 9, 7, Map.of(BodyPart.KNEE, 3)),
                day(2, 9, 7, Map.of(BodyPart.KNEE, 2))), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.STOP);
        assertThat(rulesOf(result)).containsExactlyInAnyOrder(
                CoachingRule.SEVERE_PAIN, CoachingRule.PAIN_PERSISTENT, CoachingRule.HIGH_RPE_REPEATED);
    }


    @Test
    @DisplayName("걸리는 룰이 없으면 NORMAL")
    void noRuleTriggered_normal() {
        CoachingAssessment result = engine.evaluate(List.of(normalDay(0), normalDay(1), normalDay(2)), today);

        assertThat(result.level()).isEqualTo(CoachingLevel.NORMAL);
        assertThat(result.reasons()).isEmpty();
    }
}
