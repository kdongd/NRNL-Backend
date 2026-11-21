package com.kdongd.nrnl.exception;

public class AuthenticatedUserNotFoundException extends RuntimeException {
    public AuthenticatedUserNotFoundException(Long userId) {
        super("인증된 사용자를 찾을 수 없습니다. id: " + userId);
    }
}
