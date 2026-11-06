
package com.kdongdexample.norunnolifeexample.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityLikeResponse(

        @Schema(description = "좋아요 수", example = "15")
        long likeCount,

        @Schema(description = "현재 로그인한 회원의 좋아요 여부", example = "true")
        boolean likedByMe

) {
}
