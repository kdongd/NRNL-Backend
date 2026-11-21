
package com.kdongd.nrnl.dto;

import com.kdongd.nrnl.domain.User;

public record UserMeResponse(
        Long id,
        String email,
        String nickname
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }
}
