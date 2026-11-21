package com.kdongd.nrnl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        @Schema(description = "현재 페이지 데이터 목록")
        List<T> content,
        @Schema(description = "현재 페이지 번호(0부터 시작)", example = "0")
        int number,
        @Schema(description = "페이지 크기", example = "10")
        int size,
        @Schema(description = "전체 요소 수", example = "42")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages,
        @Schema(description = "첫 페이지 여부", example = "true")
        boolean first,
        @Schema(description = "마지막 페이지 여부", example = "false")
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
