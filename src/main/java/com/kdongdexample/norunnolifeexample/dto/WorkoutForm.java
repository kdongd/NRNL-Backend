package com.kdongdexample.norunnolifeexample.dto;

import com.kdongdexample.norunnolifeexample.domain.TechniqueType;
import com.kdongdexample.norunnolifeexample.domain.WorkoutType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public record WorkoutForm(
        @Schema(description = "운동 타입", example = "RUNNING")
        @NotNull
        WorkoutType type,

        @Schema(description = "운동 시간(분)", example = "30")
        @NotNull @Min(1) @Max(600)
        Integer durationMinutes,

        @Schema(description = "메모", example = "가볍게 조깅함")
        @Size(max = 255, message = "메모는 255자까지 입력할 수 있습니다")
        String memo,

        @Schema(description = "운동 일시", example = "2026-08-10T07:00:00")
        @NotNull @PastOrPresent
        LocalDateTime workoutDateTime,

        @Schema(description = "운동 상세 목록")
        @Valid
        List<WorkoutDetailForm> details,

        // 러닝 전용
        @Schema(description = "러닝 거리(km), 러닝 타입 전용", example = "5.2")
        @Positive
        Double distanceKm,
        @Schema(description = "러닝 장소, 러닝 타입 전용", example = "한강공원")
        @Size(max = 255, message = "장소는 255자까지 입력할 수 있습니다")
        String place,
        @Schema(description = "소모 칼로리, 러닝 타입 전용", example = "350")
        @Min(1)
        Integer caloriesBurned,

        // 복싱 전용
        @Schema(description = "라운드 수, 복싱 타입 전용", example = "5")
        @Min(1)
        Integer rounds,
        @Schema(description = "스파링 상대, 복싱 타입 전용", example = "김철수")
        @Size(max = 255, message = "스파링 상대는 255자까지 입력할 수 있습니다")
        String sparringPartner,
        @Schema(description = "기술 타입, 복싱 타입 전용", example = "SPARRING")
        TechniqueType techniqueType
) {
        @AssertTrue(message = "러닝 기록에는 distanceKm이 필수입니다")
        public boolean isDistanceKmValidForRunning() {
                return type != WorkoutType.RUNNING || distanceKm != null;
        }

        @AssertTrue(message = "복싱 기록에는 rounds가 필수입니다")
        public boolean isRoundsValidForBoxing() {
                return type != WorkoutType.BOXING || rounds != null;
        }

        @AssertTrue(message = "복싱 기록에는 distanceKm/place/caloriesBurned를 보낼 수 없습니다")
        public boolean isNoRunningFieldsForBoxing() {
                return type != WorkoutType.BOXING
                        || (distanceKm == null && place == null && caloriesBurned == null);
        }

        @AssertTrue(message = "러닝 기록에는 rounds/sparringPartner/techniqueType을 보낼 수 없습니다")
        public boolean isNoBoxingFieldsForRunning() {
                return type != WorkoutType.RUNNING
                        || (rounds == null && sparringPartner == null && techniqueType == null);
        }
}
