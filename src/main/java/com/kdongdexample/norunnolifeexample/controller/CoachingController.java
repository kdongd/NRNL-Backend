package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.dto.CoachingAssessmentResponse;
import com.kdongdexample.norunnolifeexample.dto.CoachingResponse;
import com.kdongdexample.norunnolifeexample.service.CoachingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Coaching", description = "운동 후 컨디션 기반 코칭 API")
@RestController
public class CoachingController {

    private final CoachingService coachingService;

    public CoachingController(CoachingService coachingService) {
        this.coachingService = coachingService;
    }

    @Operation(
            summary = "룰 기반 컨디션 판정",
            description = "최근 14일 운동 후 컨디션 기록으로 위험 단계를 판정한다. LLM을 호출하지 않는다"
    )
    @ApiResponse(responseCode = "200", description = "판정 성공")
    @GetMapping("/coaching/assessment")
    public ResponseEntity<CoachingAssessmentResponse> getAssessment(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                CoachingAssessmentResponse.from(coachingService.assess(userId))
        );
    }

    // AI 코칭 생성 또는 기존 캐시 재사용
    @Operation(
            summary = "AI 코칭 받기",
            description = "기존 캐시가 유효하면 재사용하고, 없으면 AI 코칭을 생성한다"
    )
    @ApiResponse(responseCode = "200", description = "코칭 결과 반환")
    @PostMapping("/coaching")
    public ResponseEntity<CoachingResponse> coach(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                CoachingResponse.from(coachingService.coach(userId))
        );
    }

    // 새로고침 또는 페이지 재진입 시 기존 코칭 조회
    // 이 API에서는 LLM을 절대 호출하지 않는다.
    @Operation(
            summary = "최근 AI 코칭 조회",
            description = "DB에 저장된 유효한 AI 코칭을 조회한다. 새 코칭을 생성하지 않는다"
    )
    @ApiResponse(responseCode = "200", description = "저장된 코칭 조회 성공")
    @ApiResponse(responseCode = "204", description = "저장된 유효한 코칭 없음")
    @GetMapping("/coaching/latest")
    public ResponseEntity<CoachingResponse> getLatestCoaching(
            @AuthenticationPrincipal Long userId
    ) {
        return coachingService.getLatestCoaching(userId)
                .map(result -> ResponseEntity.ok(CoachingResponse.from(result)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
