package com.kdongdexample.norunnolifeexample.dto;

import com.kdongdexample.norunnolifeexample.domain.ExperienceLevel;
import com.kdongdexample.norunnolifeexample.domain.Gender;
import com.kdongdexample.norunnolifeexample.domain.WorkoutType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserProfileRequest(
        @NotNull Gender gender,
        @NotNull @Min(5) @Max(100) Integer age,
        @NotNull @DecimalMin("100.0") @DecimalMax("250.0") Double heightCm,
        @NotNull @DecimalMin("30.0") @DecimalMax("200.0") Double weightKg,
        @NotNull ExperienceLevel experienceLevel,
        @NotEmpty Set<WorkoutType> primaryExercises,
        @Size(max = 255, message = "목표는 255자까지 입력할 수 있습니다")
        String goal
) {}
