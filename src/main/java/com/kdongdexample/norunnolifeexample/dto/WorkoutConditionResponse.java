
package com.kdongdexample.norunnolifeexample.dto;

import com.kdongdexample.norunnolifeexample.domain.BodyPart;
import com.kdongdexample.norunnolifeexample.domain.WorkoutCondition;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record WorkoutConditionResponse(
        @Schema(description = "운동 기록 id", example = "1")
        Long workoutId,

        @Schema(description = "운동 체감 강도(RPE, 1~5)", example = "4")
        Integer rpe,

        @Schema(description = "운동 직후 몸 상태(1~5)", example = "3")
        Integer bodyCondition,

        @Schema(
                description = "통증 여부 (pains가 비어 있지 않으면 true)",
                example = "true"
        )
        boolean hasPain,

        @Schema(description = "통증 부위별 강도 목록")
        List<PainResponse> pains,

        @Schema(description = "메모", example = "운동 중간에 무릎 통증이 생겼다.")
        String memo,

        @Schema(description = "생성 일시", example = "2026-10-11T21:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 일시", example = "2026-10-11T21:00:00")
        LocalDateTime updatedAt
) {
    public record PainResponse(
            @Schema(description = "통증 부위", example = "KNEE")
            BodyPart bodyPart,

            @Schema(description = "통증 강도(1~5)", example = "3")
            Integer intensity
    ) {}

    public static WorkoutConditionResponse from(WorkoutCondition condition) {
        List<PainResponse> pains = condition.getPains()
                .entrySet()
                .stream()
                .map(e -> new PainResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PainResponse::bodyPart))
                .toList();

        return new WorkoutConditionResponse(
                condition.getWorkout().getId(),
                condition.getRpe(),
                condition.getBodyCondition(),
                condition.hasPain(),
                pains,
                condition.getMemo(),
                condition.getCreatedAt(),
                condition.getUpdatedAt()
        );
    }
}
