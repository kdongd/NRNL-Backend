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

    @Operation(summary = "룰 기반 컨디션 판정",
            description = "최근 14일 운동 후 컨디션 기록으로 위험 단계(중단/휴식/강도 감소/정상)를 판정한다. LLM을 호출하지 않는다")
    @ApiResponse(responseCode = "200", description = "판정 성공")
    @GetMapping("/coaching/assessment")
    public ResponseEntity<CoachingAssessmentResponse> getAssessment(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(CoachingAssessmentResponse.from(coachingService.assess(userId)));
    }

    // 호출할 때마다 LLM 비용이 발생하는 생성 작업이라 GET이 아닌 POST ("코칭 받기" 버튼)
    @Operation(summary = "AI 코칭 받기",
            description = "룰 판정 결과와 최근 기록을 바탕으로 AI 코칭을 생성한다. " +
                    "데이터가 부족하면 LLM을 호출하지 않고, LLM 호출에 실패하면 룰 판정만 반환한다")
    @ApiResponse(responseCode = "200", description = "코칭 결과 (status로 AI 생성 여부 구분)")
    @PostMapping("/coaching")
    public ResponseEntity<CoachingResponse> coach(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(CoachingResponse.from(coachingService.coach(userId)));
    }
}
