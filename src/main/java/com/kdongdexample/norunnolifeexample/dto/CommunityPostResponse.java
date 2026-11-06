
package com.kdongdexample.norunnolifeexample.dto;

import com.kdongdexample.norunnolifeexample.domain.CommunityPost;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CommunityPostResponse(

        @Schema(description = "게시글 ID", example = "1")
        Long id,

        @Schema(description = "작성자 회원 ID", example = "3")
        Long authorId,

        @Schema(description = "작성자 닉네임", example = "러닝왕")
        String authorNickname,

        @Schema(description = "게시글 제목", example = "오늘 러닝 5km 완료!")
        String title,

        @Schema(description = "게시글 본문", example = "오늘 한강에서 5km 러닝을 완료했습니다.")
        String content,

        @Schema(description = "게시글 생성 일시", example = "2026-10-29T20:30:00")
        LocalDateTime createdAt,

        @Schema(description = "게시글 수정 일시", example = "2026-10-29T20:30:00")
        LocalDateTime updatedAt

) {

    // CommunityPost 엔티티를 응답 DTO로 변환
    public static CommunityPostResponse from(CommunityPost post) {
        return new CommunityPostResponse(
                post.getId(),
                post.getAuthor().getId(),
                post.getAuthor().getNickname(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
