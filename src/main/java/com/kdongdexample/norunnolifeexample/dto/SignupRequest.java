package com.kdongdexample.norunnolifeexample.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        // @Email은 형식만 검사하고 전체 길이는 보지 않으므로 DB 컬럼 users.email varchar(255)에 맞춰 길이를 따로 제한한다
        @NotBlank @Email
        @Size(max = 255, message = "이메일은 255자까지 입력할 수 있습니다")
        String email,

        @NotBlank @Size(min = 8, max = 64) String password,

        @NotBlank
        @Size(min = 2, max = 10)
        String nickname,

        @Valid
        @NotNull
        UserProfileRequest profile
) {}
