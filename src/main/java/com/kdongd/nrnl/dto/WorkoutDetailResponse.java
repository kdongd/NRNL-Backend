package com.kdongd.nrnl.dto;

import com.kdongd.nrnl.domain.WorkoutDetail;
import io.swagger.v3.oas.annotations.media.Schema;

public record WorkoutDetailResponse(
        @Schema(description = "상세 id", example = "1")
        Long id,
        @Schema(description = "순서", example = "1")
        Integer sequence,
        @Schema(description = "라벨", example = "워밍업")
        String label,
        @Schema(description = "지속 시간(초)", example = "300")
        Integer durationSeconds,
        @Schema(description = "비고", example = "가볍게 시작")
        String note
) {
    public static WorkoutDetailResponse from(WorkoutDetail detail) {
        return new WorkoutDetailResponse(
                detail.getId(),
                detail.getSequence(),
                detail.getLabel(),
                detail.getDurationSeconds(),
                detail.getNote()
        );
    }
}
