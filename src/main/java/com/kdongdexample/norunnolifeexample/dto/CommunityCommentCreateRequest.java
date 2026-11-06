
package com.kdongdexample.norunnolifeexample.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityCommentCreateRequest(

        @Schema(description = "댓글 내용", example = "오늘 운동 정말 수고하셨습니다!")
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        @Size(max = 200, message = "댓글은 200자 이하로 입력해주세요.")
        String content

) {
}
