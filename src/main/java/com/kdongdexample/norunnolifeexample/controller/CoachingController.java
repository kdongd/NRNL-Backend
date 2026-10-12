package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.dto.CoachingAssessmentResponse;
import com.kdongdexample.norunnolifeexample.service.CoachingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Coaching", description = "운동 후 컨디션 기반 코칭 API")
@RestController
public class CoachingController {

    private final CoachingService coachingService;

    public CoachingController(CoachingService coachingService) {
        this.coachingService = coachingService;
    }

    @Operation(summary = "룰 기반 컨디션 판정",
            description = "최근 14일 운동 후 컨디션 기록으로 위험 단계(중단/휴식/강도 감소/정상)를 판정한다")
    @ApiResponse(responseCode = "200", description = "판정 성공")
    @GetMapping("/coaching/assessment")
    public ResponseEntity<CoachingAssessmentResponse> getAssessment(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(CoachingAssessmentResponse.from(coachingService.assess(userId)));
    }
}
