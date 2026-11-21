package com.kdongd.nrnl.dto;

import com.kdongd.nrnl.domain.ExperienceLevel;
import com.kdongd.nrnl.domain.Gender;
import com.kdongd.nrnl.domain.UserProfile;
import com.kdongd.nrnl.domain.WorkoutType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

public record UserProfileResponse(
        @Schema(description = "프로필 id", example = "1")
        Long id,
        @Schema(description = "성별", example = "MAN")
        Gender gender,
        @Schema(description = "나이", example = "28")
        Integer age,
        @Schema(description = "키(cm)", example = "175.5")
        Double heightCm,
        @Schema(description = "체중(kg)", example = "70.2")
        Double weightKg,
        @Schema(description = "피트니스 레벨", example = "INTERMEDIATE")
        ExperienceLevel experienceLevel,
        @Schema(description = "주요 운동(다중 선택)")
        Set<WorkoutType> primaryExercises,
        @Schema(description = "운동 목표", example = "10km 50분 완주")
        String goal,
        @Schema(description = "생성 일시", example = "2026-09-08T10:00:00")
        LocalDateTime createdAt,
        @Schema(description = "수정 일시", example = "2026-09-08T10:00:00")
        LocalDateTime updatedAt
) {
    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getGender(),
                profile.getAge(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getExperienceLevel(),
                profile.getPrimaryExercises(),
                profile.getGoal(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
