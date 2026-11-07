
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.dto.CommunityCommentResponse;
import com.kdongdexample.norunnolifeexample.dto.PageResponse;
import com.kdongdexample.norunnolifeexample.service.CommunityCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CommunityComment", description = "커뮤니티 댓글 관리 API")
@RestController
@RequestMapping("/community/comments")
public class CommunityMyCommentController {

    private final CommunityCommentService service;

    public CommunityMyCommentController(CommunityCommentService service) {
        this.service = service;
    }

    // 내가 작성한 댓글 목록 조회
    @Operation(summary = "내 댓글 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<PageResponse<CommunityCommentResponse>> getMyComments(
            @AuthenticationPrincipal Long userId,

            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                PageResponse.from(
                        service.findMyComments(userId, pageable)
                )
        );
    }
}
