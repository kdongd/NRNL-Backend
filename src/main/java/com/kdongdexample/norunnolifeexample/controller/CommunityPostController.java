
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.dto.CommunityPostCreateRequest;
import com.kdongdexample.norunnolifeexample.dto.CommunityPostResponse;
import com.kdongdexample.norunnolifeexample.dto.CommunityPostUpdateRequest;
import com.kdongdexample.norunnolifeexample.dto.PageResponse;
import com.kdongdexample.norunnolifeexample.exception.ErrorResponse;
import com.kdongdexample.norunnolifeexample.service.CommunityPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@Tag(
        name = "CommunityPost",
        description = "커뮤니티 게시글 관리 API"
)
@RestController
@RequestMapping("/community/posts")
public class CommunityPostController {

    private final CommunityPostService service;

    public CommunityPostController(CommunityPostService service) {
        this.service = service;
    }

    // 게시글 작성
    @Operation(
            summary = "커뮤니티 게시글 작성",
            description = "로그인한 회원이 새로운 게시글을 작성한다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "작성 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<CommunityPostResponse> createPost(
            @Valid @RequestBody CommunityPostCreateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        CommunityPostResponse saved = service.create(request, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.id())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    // 전체 게시글 조회
    @Operation(
            summary = "커뮤니티 게시글 전체 조회",
            description = "전체 게시글을 페이징 및 정렬하여 조회한다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "정렬 필드 오류",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<PageResponse<CommunityPostResponse>> getPosts(
            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                PageResponse.from(service.findAll(pageable))
        );
    }

    // 제목 및 작성자 닉네임 검색
    @Operation(
            summary = "커뮤니티 게시글 검색",
            description = "제목 또는 작성자 닉네임을 기준으로 게시글을 검색한다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색 조건 또는 정렬 필드 오류"
            )
    })
    @GetMapping("/search")
    public ResponseEntity<PageResponse<CommunityPostResponse>> searchPosts(
            @Parameter(description = "검색 기준", example = "title")
            @RequestParam String type,

            @Parameter(description = "검색어", example = "러닝")
            @RequestParam String keyword,

            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        if (keyword.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if ("title".equals(type)) {
            return ResponseEntity.ok(
                    PageResponse.from(
                            service.searchByTitle(keyword, pageable)
                    )
            );
        }

        if ("nickname".equals(type)) {
            return ResponseEntity.ok(
                    PageResponse.from(
                            service.searchByNickname(keyword, pageable)
                    )
            );
        }

        return ResponseEntity.badRequest().build();
    }

    // 내가 작성한 게시글 조회
    @Operation(
            summary = "내 게시글 조회",
            description = "현재 로그인한 회원이 작성한 게시글을 조회한다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/me")
    public ResponseEntity<PageResponse<CommunityPostResponse>> getMyPosts(
            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,

            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                PageResponse.from(
                        service.findByAuthor(userId, pageable)
                )
        );
    }

    // 특정 회원이 작성한 게시글 조회
    @Operation(
            summary = "특정 회원 게시글 조회",
            description = "회원 ID를 기준으로 해당 회원이 작성한 게시글을 조회한다"
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/users/{userId}")
    public ResponseEntity<PageResponse<CommunityPostResponse>> getUserPosts(
            @Parameter(description = "회원 ID", example = "1")
            @PathVariable Long userId,

            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                PageResponse.from(
                        service.findByAuthor(userId, pageable)
                )
        );
    }

    // 게시글 상세 조회
    @Operation(
            summary = "커뮤니티 게시글 상세 조회",
            description = "게시글 ID를 기준으로 게시글 상세 정보를 조회한다"
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
    @GetMapping("/{id}")
    public ResponseEntity<CommunityPostResponse> getPost(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.findById(id));
    }

    // 게시글 수정
    @Operation(
            summary = "커뮤니티 게시글 수정",
            description = "게시글 작성자 본인만 제목과 본문을 수정할 수 있다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음 또는 수정 권한 없음"
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<CommunityPostResponse> updatePost(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long id,

            @Valid @RequestBody CommunityPostUpdateRequest request,

            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                service.update(id, request, userId)
        );
    }

    // 게시글 삭제
    @Operation(
            summary = "커뮤니티 게시글 삭제",
            description = "게시글 작성자 본인만 게시글을 삭제할 수 있다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음 또는 삭제 권한 없음"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long id,

            @AuthenticationPrincipal Long userId
    ) {
        service.delete(id, userId);

        return ResponseEntity.noContent().build();
    }
}
