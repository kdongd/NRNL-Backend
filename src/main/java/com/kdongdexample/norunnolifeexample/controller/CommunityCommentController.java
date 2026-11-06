
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.dto.CommunityCommentCreateRequest;
import com.kdongdexample.norunnolifeexample.dto.CommunityCommentResponse;
import com.kdongdexample.norunnolifeexample.dto.CommunityCommentUpdateRequest;
import com.kdongdexample.norunnolifeexample.dto.PageResponse;
import com.kdongdexample.norunnolifeexample.service.CommunityCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "CommunityComment", description = "커뮤니티 댓글 관리 API")
@RestController
@RequestMapping("/community/posts/{postId}/comments")
public class CommunityCommentController {

    private final CommunityCommentService service;

    public CommunityCommentController(CommunityCommentService service) {
        this.service = service;
    }

    // 댓글 작성
    @Operation(summary = "커뮤니티 댓글 작성")
    @PostMapping
    public ResponseEntity<CommunityCommentResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommunityCommentCreateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        CommunityCommentResponse saved =
                service.create(postId, request, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{commentId}")
                .buildAndExpand(saved.id())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    // 특정 게시글 댓글 목록 조회
    @Operation(summary = "커뮤니티 게시글 댓글 목록 조회")
    @GetMapping
    public ResponseEntity<PageResponse<CommunityCommentResponse>> getComments(
            @PathVariable Long postId,

            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                PageResponse.from(
                        service.findByPost(postId, pageable)
                )
        );
    }

    // 댓글 수정
    @Operation(summary = "커뮤니티 댓글 수정")
    @PutMapping("/{commentId}")
    public ResponseEntity<CommunityCommentResponse> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommunityCommentUpdateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                service.update(postId, commentId, request, userId)
        );
    }

    // 댓글 삭제
    @Operation(summary = "커뮤니티 댓글 삭제")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId
    ) {
        service.delete(postId, commentId, userId);

        return ResponseEntity.noContent().build();
    }
}
