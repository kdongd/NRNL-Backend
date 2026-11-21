package com.kdongd.nrnl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkoutDetailForm(
        @Schema(description = "순서", example = "1")
        @NotNull @Min(1)
        Integer sequence,

        @Schema(description = "라벨", example = "워밍업")
        @NotNull @Size(max = 100)
        String label,

        @Schema(description = "지속 시간(초)", example = "300")
        @NotNull @Min(1) @Max(36000)
        Integer durationSeconds,

        @Schema(description = "비고", example = "가볍게 시작")
        @Size(max = 255)
        String note
) {}
