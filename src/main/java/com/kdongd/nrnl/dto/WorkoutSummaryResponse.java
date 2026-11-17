package com.kdongd.nrnl.dto;

import com.kdongd.nrnl.domain.TechniqueType;
import com.kdongd.nrnl.domain.Workout;
import com.kdongd.nrnl.domain.WorkoutType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record WorkoutSummaryResponse(
        @Schema(description = "운동 기록 id", example = "1")
        Long id,
        @Schema(description = "운동 타입", example = "RUNNING")
        WorkoutType type,
        @Schema(description = "운동 시간(분)", example = "30")
        Integer durationMinutes,
        @Schema(description = "메모", example = "가볍게 조깅함")
        String memo,
        @Schema(description = "운동 일시", example = "2026-08-10T07:00:00")
        LocalDateTime workoutDateTime,
        @Schema(description = "생성 일시", example = "2026-08-10T07:05:00")
        LocalDateTime createdAt,
        @Schema(description = "수정 일시", example = "2026-08-10T07:05:00")
        LocalDateTime updatedAt,

        // 러닝 전용
        @Schema(description = "러닝 거리(km), 러닝 타입 전용", example = "5.2")
        Double distanceKm,
        @Schema(description = "러닝 장소, 러닝 타입 전용", example = "한강공원")
        String place,
        @Schema(description = "소모 칼로리, 러닝 타입 전용", example = "350")
        Integer caloriesBurned,

        // 복싱 전용
        @Schema(description = "라운드 수, 복싱 타입 전용", example = "5")
        Integer rounds,
        @Schema(description = "스파링 상대, 복싱 타입 전용", example = "김철수")
        String sparringPartner,
        @Schema(description = "기술 타입, 복싱 타입 전용", example = "SPARRING")
        TechniqueType techniqueType
) {
    public static WorkoutSummaryResponse from(Workout workout) {
        WorkoutTypeFields typeFields = WorkoutTypeFields.from(workout);

        return new WorkoutSummaryResponse(
                workout.getId(),
                workout.getType(),
                workout.getDurationMinutes(),
                workout.getMemo(),
                workout.getWorkoutDateTime(),
                workout.getCreatedAt(),
                workout.getUpdatedAt(),
                typeFields.distanceKm(),
                typeFields.place(),
                typeFields.caloriesBurned(),
                typeFields.rounds(),
                typeFields.sparringPartner(),
                typeFields.techniqueType()
        );
    }
}
