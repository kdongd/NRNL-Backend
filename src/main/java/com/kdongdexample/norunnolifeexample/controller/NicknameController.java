
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.dto.NicknameAvailabilityResponse;
import com.kdongdexample.norunnolifeexample.dto.NicknameUpdateRequest;
import com.kdongdexample.norunnolifeexample.service.NicknameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class NicknameController {

    private final NicknameService nicknameService;

    public NicknameController(NicknameService nicknameService) {
        this.nicknameService = nicknameService;
    }

    // 현재 로그인한 사용자의 닉네임 조회
    @GetMapping("/me/nickname")
    public ResponseEntity<NicknameResponse> getMyNickname(
            @AuthenticationPrincipal Long userId
    ) {
        String nickname = nicknameService.getNickname(userId);

        return ResponseEntity.ok(new NicknameResponse(nickname));
    }

    // 닉네임 중복 확인
    @GetMapping("/nicknames/availability")
    public ResponseEntity<NicknameAvailabilityResponse> checkNicknameAvailability(
            @RequestParam String nickname
    ) {
        return ResponseEntity.ok(
                nicknameService.checkAvailability(nickname)
        );
    }

    // 닉네임 등록 및 변경
    @PutMapping("/me/nickname")
    public ResponseEntity<Void> changeNickname(
            @Valid @RequestBody NicknameUpdateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        nicknameService.changeNickname(userId, request.nickname());

        return ResponseEntity.ok().build();
    }

    public record NicknameResponse(String nickname) {
    }
}
