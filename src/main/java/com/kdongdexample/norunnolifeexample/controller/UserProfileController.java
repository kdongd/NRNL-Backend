
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.domain.UserProfile;
import com.kdongdexample.norunnolifeexample.dto.UserProfileRequest;
import com.kdongdexample.norunnolifeexample.dto.UserProfileResponse;
import com.kdongdexample.norunnolifeexample.exception.ErrorResponse;
import com.kdongdexample.norunnolifeexample.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(
        name = "UserProfile",
        description = "회원 운동 프로필 관리 API"
)
@RestController
@RequestMapping("/users/me/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    // 기존 API: 최초 운동 프로필 등록
    @Operation(
            summary = "운동 프로필 등록",
            description = "운동 프로필이 없는 회원이 최초 등록한다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 프로필이 존재함"
            )
    })
    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(
            @Valid @RequestBody UserProfileRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        UserProfile profile =
                userProfileService.createProfile(userId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();

        return ResponseEntity.created(location)
                .body(UserProfileResponse.from(profile));
    }

    // [추가] 내 운동 프로필 조회
    @Operation(
            summary = "내 운동 프로필 조회",
            description = "현재 로그인한 회원의 운동 프로필을 조회한다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "프로필 없음")
    })
    @GetMapping
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                userProfileService.getMyProfile(userId)
        );
    }

    // [추가] 내 운동 프로필 수정
    @Operation(
            summary = "내 운동 프로필 수정",
            description = "현재 로그인한 회원의 운동 프로필 전체를 수정한다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @ApiResponse(responseCode = "404", description = "프로필 없음")
    })
    @PutMapping
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UserProfileRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                userProfileService.updateProfile(userId, request)
        );
    }
}
