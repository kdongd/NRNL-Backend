package com.kdongd.nrnl.dto;

import com.kdongd.nrnl.domain.WorkoutType;

public record WorkoutStatByType(
        WorkoutType type,
        Long count,
        Long totalDurationMinutes
) {}
