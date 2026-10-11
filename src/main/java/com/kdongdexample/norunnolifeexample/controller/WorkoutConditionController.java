package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.domain.WorkoutCondition;
import com.kdongdexample.norunnolifeexample.dto.WorkoutConditionRequest;
import com.kdongdexample.norunnolifeexample.dto.WorkoutConditionResponse;
import com.kdongdexample.norunnolifeexample.exception.ErrorResponse;
import com.kdongdexample.norunnolifeexample.service.WorkoutConditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "WorkoutCondition", description = "운동 종료 후 컨디션(RPE/몸 상태/통증) 기록 API")
@RestController
public class WorkoutConditionController {

    private final WorkoutConditionService conditionService;

    public WorkoutConditionController(WorkoutConditionService conditionService) {
        this.conditionService = conditionService;
    }

    @Operation(summary = "운동 후 컨디션 저장",
            description = "운동 기록에 RPE, 몸 상태, 통증 정보를 저장한다. 이미 있으면 전체를 덮어쓴다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 id의 운동 기록 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "동시 저장 충돌",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/workouts/{workoutId}/condition")
    public ResponseEntity<WorkoutConditionResponse> saveCondition(
            @Parameter(description = "운동 기록 id") @PathVariable Long workoutId,
            @Valid @RequestBody WorkoutConditionRequest request,
            @AuthenticationPrincipal Long userId) {
        WorkoutCondition saved = conditionService.upsert(workoutId, request, userId);
        return ResponseEntity.ok(WorkoutConditionResponse.from(saved));
    }

    @Operation(summary = "운동 후 컨디션 조회", description = "운동 기록에 저장된 컨디션을 조회한다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "운동 기록 또는 컨디션 기록 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/workouts/{workoutId}/condition")
    public ResponseEntity<WorkoutConditionResponse> getCondition(
            @Parameter(description = "운동 기록 id") @PathVariable Long workoutId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(WorkoutConditionResponse.from(conditionService.findByWorkout(workoutId, userId)));
    }
}
