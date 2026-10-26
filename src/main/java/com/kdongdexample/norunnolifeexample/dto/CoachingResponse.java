package com.kdongdexample.norunnolifeexample.dto;

import com.kdongdexample.norunnolifeexample.domain.CoachingAdvice;
import com.kdongdexample.norunnolifeexample.domain.CoachingLevel;
import com.kdongdexample.norunnolifeexample.domain.CoachingResult;
import com.kdongdexample.norunnolifeexample.domain.CoachingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record CoachingResponse(
        @Schema(description = "룰 엔진 판정 단계", example = "REST")
        CoachingLevel level,
        @Schema(description = "판정 근거")
        List<CoachingAssessmentResponse.ReasonResponse> reasons,
        @Schema(description = "판정 기간 시작일", example = "2026-09-29")
        LocalDate evaluatedFrom,
        @Schema(description = "판정 기간 종료일", example = "2026-10-12")
        LocalDate evaluatedTo,
        @Schema(description = "AI 코칭 생성 상태 (GENERATED / SKIPPED_INSUFFICIENT_DATA / UNAVAILABLE)", example = "GENERATED")
        CoachingStatus status,
        @Schema(description = "AI 코칭 내용. status가 GENERATED일 때만 존재")
        AdviceResponse coaching
) {
    public record AdviceResponse(
            @Schema(description = "오늘의 결론", example = "최근 무릎 통증이 이어지고 있어 오늘은 쉬는 게 좋아요.")
            String summary,
            @Schema(description = "판정 근거와 최근 기록 설명")
            String explanation,
            @Schema(description = "오늘 할 수 있는 행동")
            List<String> recommendations,
            @Schema(description = "주의사항 (없으면 빈 문자열)")
            String caution
    ) {
        static AdviceResponse from(CoachingAdvice advice) {
            return advice == null ? null
                    : new AdviceResponse(advice.summary(), advice.explanation(), advice.recommendations(), advice.caution());
        }
    }

    public static CoachingResponse from(CoachingResult result) {
        CoachingAssessmentResponse assessment = CoachingAssessmentResponse.from(result.assessment());
        return new CoachingResponse(
                assessment.level(),
                assessment.reasons(),
                assessment.evaluatedFrom(),
                assessment.evaluatedTo(),
                result.status(),
                AdviceResponse.from(result.advice())
        );
    }
}
