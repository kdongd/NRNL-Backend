
package com.kdongdexample.norunnolifeexample.dto;

import com.kdongdexample.norunnolifeexample.domain.CommunityComment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CommunityCommentResponse(

        @Schema(description = "댓글 ID")
        Long id,

        @Schema(description = "게시글 ID")
        Long postId,

        @Schema(description = "작성자 ID")
        Long authorId,

        @Schema(description = "작성자 닉네임")
        String authorNickname,

        @Schema(description = "댓글 내용")
        String content,

        @Schema(description = "댓글 작성 시간")
        LocalDateTime createdAt,

        @Schema(description = "댓글 수정 시간")
        LocalDateTime updatedAt

) {

    public static CommunityCommentResponse from(CommunityComment comment) {
        return new CommunityCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getNickname(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
