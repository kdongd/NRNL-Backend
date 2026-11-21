package com.kdongd.nrnl.controller;

import com.kdongd.nrnl.domain.Workout;
import com.kdongd.nrnl.domain.WorkoutType;
import com.kdongd.nrnl.dto.PageResponse;
import com.kdongd.nrnl.dto.WorkoutForm;
import com.kdongd.nrnl.dto.WorkoutMonthlyStat;
import com.kdongd.nrnl.dto.WorkoutResponse;
import com.kdongd.nrnl.dto.WorkoutStatByType;
import com.kdongd.nrnl.dto.WorkoutSummaryResponse;
import com.kdongd.nrnl.exception.ErrorResponse;
import com.kdongd.nrnl.service.WorkoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Workout", description = "운동 기록 관리 API")
@RestController
public class WorkoutController {
    private final WorkoutService service;

    public WorkoutController(WorkoutService service) {
        this.service = service;
    }

    @Operation(summary = "운동 기록 검색", description = "타입/기간으로 필터링하고 페이지네이션·정렬하여 운동 기록 목록을 조회한다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "정렬 필드 또는 파라미터 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/workouts")
    public ResponseEntity<PageResponse<WorkoutSummaryResponse>> searchWorkouts(
            @Parameter(description = "운동 타입 필터") @RequestParam(required = false) WorkoutType type,
            @Parameter(description = "조회 시작 일시") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "조회 종료 일시") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "페이지, 사이즈, 정렬 파라미터") @PageableDefault(size = 10, sort = "workoutDateTime", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Long userId) {

        Page<WorkoutSummaryResponse> page = service.search(type, from, to, pageable, userId)
                .map(WorkoutSummaryResponse::from);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Operation(summary = "운동 기록 등록", description = "러닝 또는 복싱 운동 기록을 새로 등록한다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/workouts")
    public ResponseEntity<WorkoutResponse> createWorkout(@Valid @RequestBody WorkoutForm form,
                                                         @AuthenticationPrincipal Long userId) {
        Workout saved = service.save(form, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(WorkoutResponse.from(saved));
    }

    @Operation(summary = "운동 기록 단건 조회", description = "id로 운동 기록 상세를 조회한다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 id의 운동 기록 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/workouts/{id}")
    public ResponseEntity<WorkoutResponse> getWorkout(@Parameter(description = "운동 기록 id") @PathVariable Long id,
                                                      @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(WorkoutResponse.from(service.findById(id, userId)));
    }

    @Operation(summary = "운동 기록 수정", description = "id로 조회된 운동 기록을 수정한다. 타입은 변경할 수 없다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 타입 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 id의 운동 기록 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/workouts/{id}")
    public ResponseEntity<WorkoutResponse> updateWorkout(@Parameter(description = "운동 기록 id") @PathVariable Long id,
                                                         @Valid @RequestBody WorkoutForm form,
                                                         @AuthenticationPrincipal Long userId) {
        Workout updated = service.update(id, form, userId);
        return ResponseEntity.ok(WorkoutResponse.from(updated));
    }

    @Operation(summary = "운동 기록 삭제", description = "id로 운동 기록을 삭제한다")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "해당 id의 운동 기록 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/workouts/{id}")
    public ResponseEntity<Void> deleteWorkout(@Parameter(description = "운동 기록 id") @PathVariable Long id,
                                              @AuthenticationPrincipal Long userId) {
        service.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "타입별 통계 조회", description = "기간 내 운동 타입별 건수와 총 시간을 조회한다. from/to 미지정 시 전체 기간 집계")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/workouts/stats/by-type")
    public ResponseEntity<List<WorkoutStatByType>> getStatsByType(
            @Parameter(description = "집계 시작 일시") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "집계 종료 일시") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(service.statsByType(from, to, userId));
    }

    @Operation(summary = "월별 통계 조회", description = "기간 내 연/월별 운동 건수를 조회한다. from/to 미지정 시 전체 기간 집계")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/workouts/stats/monthly")
    public ResponseEntity<List<WorkoutMonthlyStat>> getStatsByMonth(
            @Parameter(description = "집계 시작 일시") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "집계 종료 일시") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(service.statsByMonth(from, to, userId));
    }
}
