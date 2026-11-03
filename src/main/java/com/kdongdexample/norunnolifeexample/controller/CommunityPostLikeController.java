
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.dto.CommunityLikeResponse;
import com.kdongdexample.norunnolifeexample.exception.ErrorResponse;
import com.kdongdexample.norunnolifeexample.service.CommunityPostLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "CommunityPostLike",
        description = "커뮤니티 게시글 좋아요 관리 API"
)
@RestController
@RequestMapping("/community/posts/{postId}/likes")
public class CommunityPostLikeController {

    private final CommunityPostLikeService service;

    public CommunityPostLikeController(
            CommunityPostLikeService service
    ) {
        this.service = service;
    }

    // 게시글 좋아요 등록
    @Operation(
            summary = "커뮤니티 게시글 좋아요 등록",
            description = "로그인한 회원이 게시글에 좋아요를 등록한다. 중복 요청은 무시한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "좋아요 등록 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping
    public ResponseEntity<Void> likePost(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,

            @AuthenticationPrincipal Long userId
    ) {
        service.like(postId, userId);

        return ResponseEntity.noContent().build();
    }

    // 게시글 좋아요 취소
    @Operation(
            summary = "커뮤니티 게시글 좋아요 취소",
            description = "로그인한 회원이 자신의 게시글 좋아요를 취소한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "좋아요 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @DeleteMapping
    public ResponseEntity<Void> unlikePost(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,

            @AuthenticationPrincipal Long userId
    ) {
        service.unlike(postId, userId);

        return ResponseEntity.noContent().build();
    }

    // 게시글 좋아요 수 및 내 좋아요 여부 조회
    @Operation(
            summary = "커뮤니티 게시글 좋아요 조회",
            description = "게시글의 좋아요 수와 현재 회원의 좋아요 여부를 조회한다. 비로그인 조회도 가능하다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<CommunityLikeResponse> getPostLikes(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,

            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                service.getLikeStatus(postId, userId)
        );
    }
}
