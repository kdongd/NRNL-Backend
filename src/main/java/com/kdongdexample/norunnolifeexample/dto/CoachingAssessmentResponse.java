package com.kdongdexample.norunnolifeexample.dto;

import com.kdongdexample.norunnolifeexample.domain.CoachingAssessment;
import com.kdongdexample.norunnolifeexample.domain.CoachingLevel;
import com.kdongdexample.norunnolifeexample.domain.CoachingRule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record CoachingAssessmentResponse(
        @Schema(description = "판정 단계 (STOP > REST > REDUCE > NORMAL, 데이터 부족 시 INSUFFICIENT_DATA)", example = "REST")
        CoachingLevel level,
        @Schema(description = "판정 근거가 된 룰 목록")
        List<ReasonResponse> reasons,
        @Schema(description = "판정에 사용한 기간 시작일", example = "2026-09-29")
        LocalDate evaluatedFrom,
        @Schema(description = "판정에 사용한 기간 종료일", example = "2026-10-12")
        LocalDate evaluatedTo
) {
    public record ReasonResponse(
            @Schema(description = "걸린 룰", example = "PAIN_PERSISTENT")
            CoachingRule rule,
            @Schema(description = "상세 근거", example = "최근 입력일 3번 모두 통증 기록 (2026-10-12, 2026-10-10, 2026-10-08)")
            String detail
    ) {}

    public static CoachingAssessmentResponse from(CoachingAssessment assessment) {
        List<ReasonResponse> reasons = assessment.reasons().stream()
                .map(r -> new ReasonResponse(r.rule(), r.detail()))
                .toList();
        return new CoachingAssessmentResponse(
                assessment.level(), reasons, assessment.evaluatedFrom(), assessment.evaluatedTo());
    }
}
