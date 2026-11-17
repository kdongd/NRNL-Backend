package com.kdongd.nrnl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(
        @NotBlank(message = "닉네임을 입력해주세요")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다")
        String nickname
) {
}
