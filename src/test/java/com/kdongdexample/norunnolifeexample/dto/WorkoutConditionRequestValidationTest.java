
package com.kdongdexample.norunnolifeexample.dto;

import com.kdongdexample.norunnolifeexample.domain.BodyPart;
import com.kdongdexample.norunnolifeexample.dto.WorkoutConditionRequest.PainRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutConditionRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();
    }

    private Set<ConstraintViolation<WorkoutConditionRequest>> validate(
            WorkoutConditionRequest request
    ) {
        return validator.validate(request);
    }

    @Test
    @DisplayName("통증 없이 RPE와 몸 상태만 보내도 유효하다")
    void valid_withoutPains() {
        assertThat(validate(
                new WorkoutConditionRequest(4, 3, null, null)
        )).isEmpty();

        assertThat(validate(
                new WorkoutConditionRequest(4, 3, List.of(), null)
        )).isEmpty();
    }

    @Test
    @DisplayName("RPE 1~5점은 모두 허용한다")
    void valid_rpeRange() {
        for (int rpe = 1; rpe <= 5; rpe++) {
            assertThat(validate(
                    new WorkoutConditionRequest(rpe, 3, null, null)
            )).isEmpty();
        }
    }

    @Test
    @DisplayName("RPE가 1~5점 범위를 벗어나면 실패한다")
    void invalid_rpeOutOfRange() {
        assertThat(validate(
                new WorkoutConditionRequest(0, 3, null, null)
        )).isNotEmpty();

        assertThat(validate(
                new WorkoutConditionRequest(6, 3, null, null)
        )).isNotEmpty();
    }

    @Test
    @DisplayName("몸 상태가 누락되거나 1~5점 범위를 벗어나면 실패한다")
    void invalid_bodyCondition() {
        assertThat(validate(
                new WorkoutConditionRequest(4, null, null, null)
        )).isNotEmpty();

        assertThat(validate(
                new WorkoutConditionRequest(4, 0, null, null)
        )).isNotEmpty();

        assertThat(validate(
                new WorkoutConditionRequest(4, 6, null, null)
        )).isNotEmpty();
    }

    @Test
    @DisplayName("통증 강도 0은 허용하지 않는다")
    void invalid_zeroIntensity() {
        WorkoutConditionRequest request =
                new WorkoutConditionRequest(
                        4,
                        3,
                        List.of(new PainRequest(BodyPart.KNEE, 0)),
                        null
                );

        assertThat(validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("같은 통증 부위를 중복으로 보낼 수 없다")
    void invalid_duplicateBodyPart() {
        WorkoutConditionRequest request =
                new WorkoutConditionRequest(
                        4,
                        3,
                        List.of(
                                new PainRequest(BodyPart.KNEE, 3),
                                new PainRequest(BodyPart.KNEE, 5)
                        ),
                        null
                );

        assertThat(validate(request))
                .extracting(ConstraintViolation::getMessage)
                .contains("같은 통증 부위를 중복으로 보낼 수 없습니다");
    }
}
