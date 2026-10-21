
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.dto.NicknameAvailabilityResponse;
import com.kdongdexample.norunnolifeexample.dto.NicknameUpdateRequest;
import com.kdongdexample.norunnolifeexample.dto.UserMeResponse;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import com.kdongdexample.norunnolifeexample.service.NicknameService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class NicknameController {

    private final NicknameService nicknameService;
    private final UserRepository userRepository;

    public NicknameController(
            NicknameService nicknameService,
            UserRepository userRepository
    ) {
        this.nicknameService = nicknameService;
        this.userRepository = userRepository;
    }

    // 로그인한 사용자 정보 조회
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<UserMeResponse> getMyInfo(
            @AuthenticationPrincipal Long userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new AuthenticatedUserNotFoundException(userId)
                );

        return ResponseEntity.ok(UserMeResponse.from(user));
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

    // 닉네임 변경
    @PutMapping("/me/nickname")
    public ResponseEntity<Void> changeNickname(
            @Valid @RequestBody NicknameUpdateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        nicknameService.changeNickname(userId, request.nickname());

        return ResponseEntity.ok().build();
    }
}
