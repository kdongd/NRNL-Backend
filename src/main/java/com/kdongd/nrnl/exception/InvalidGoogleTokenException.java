package com.kdongd.nrnl.exception;

public class InvalidGoogleTokenException extends RuntimeException {
    public InvalidGoogleTokenException() {
        super("유효하지 않은 구글 로그인 토큰입니다.");
    }
}
