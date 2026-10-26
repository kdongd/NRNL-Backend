package com.kdongdexample.norunnolifeexample.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email
        @Size(max = 255, message = "이메일은 255자까지 입력할 수 있습니다")
        String email,

        @NotBlank String password
) {}
