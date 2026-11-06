
package com.kdongdexample.norunnolifeexample.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityPostUpdateRequest(

        @Schema(description = "수정할 게시글 제목 (최대 50자)", example = "오늘 러닝 10km 완료!")
        @NotBlank(message = "게시글 제목은 필수입니다")
        @Size(max = 50, message = "게시글 제목은 50자까지 입력할 수 있습니다")
        String title,

        @Schema(description = "수정할 게시글 본문", example = "오늘 한강에서 10km 러닝을 완료했습니다.")
        @NotBlank(message = "게시글 내용은 필수입니다")
        String content

) {
}
