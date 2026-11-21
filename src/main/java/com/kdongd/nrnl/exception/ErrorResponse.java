package com.kdongd.nrnl.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ErrorResponse(
        @Schema(description = "HTTP 상태 코드", example = "400")
        int status,
        @Schema(description = "에러 메시지", example = "입력값이 올바르지 않습니다")
        String message,
        @Schema(description = "필드별 에러 목록")
        List<FieldError> errors,
        @Schema(description = "머신이 구분할 수 있는 에러 코드 (해당하는 경우에만 포함)", example = "TOKEN_EXPIRED")
        String errorCode
) {
    // 기존 호출부(GlobalExceptionHandler 등) 전부 이 3-인자 생성자를 쓰고 있어서
    // errorCode 없이 만들면 자동으로 null이 들어가도록 호환 생성자를 유지한다.
    public ErrorResponse(int status, String message, List<FieldError> errors) {
        this(status, message, errors, null);
    }

    public record FieldError(
            @Schema(description = "필드명", example = "durationMinutes")
            String field,
            @Schema(description = "필드 에러 메시지", example = "1 이상이어야 합니다")
            String message
    ) {}
}
