
package com.kdongdexample.norunnolifeexample.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DailyConditionTest {

    private final User owner =
            User.create("daily@test.com", "encoded-password");

    private WorkoutCondition condition(
            LocalDateTime at,
            int rpe,
            int body,
            Map<BodyPart, Integer> pains
    ) {
        Workout workout = RunningWorkout.create(
                owner, 30, null, at, 5.0, "한강", 300
        );

        return WorkoutCondition.create(
                workout, rpe, body, pains, null
        );
    }

    @Test
    @DisplayName("같은 날 기록은 가장 나쁜 상태를 대표값으로 합친다")
    void sameDay_mergedWithWorstValues() {
        List<DailyCondition> result = DailyCondition.aggregate(
                List.of(
                        condition(
                                LocalDateTime.of(2026, 10, 12, 7, 0),
                                3,
                                4,
                                Map.of(
                                        BodyPart.KNEE, 2,
                                        BodyPart.ANKLE, 3
                                )
                        ),
                        condition(
                                LocalDateTime.of(2026, 10, 12, 19, 0),
                                5,
                                2,
                                Map.of(BodyPart.KNEE, 4)
                        )
                )
        );

        assertThat(result).hasSize(1);

        DailyCondition day = result.get(0);

        assertThat(day.maxPainByPart())
                .containsEntry(BodyPart.KNEE, 4)
                .containsEntry(BodyPart.ANKLE, 3)
                .hasSize(2);

        assertThat(day.maxPain()).isEqualTo(4);
        assertThat(day.maxRpe()).isEqualTo(5);
        assertThat(day.minBodyCondition()).isEqualTo(2);
    }

    @Test
    @DisplayName("날짜가 다르면 최신 날짜가 먼저 나온다")
    void differentDays_sortedLatestFirst() {
        List<DailyCondition> result = DailyCondition.aggregate(
                List.of(
                        condition(
                                LocalDateTime.of(2026, 10, 10, 7, 0),
                                3, 4, Map.of()
                        ),
                        condition(
                                LocalDateTime.of(2026, 10, 12, 7, 0),
                                3, 4, Map.of()
                        )
                )
        );

        assertThat(result)
                .extracting(d -> d.date().getDayOfMonth())
                .containsExactly(12, 10);

        assertThat(result.get(0).hasPain()).isFalse();
    }
}
