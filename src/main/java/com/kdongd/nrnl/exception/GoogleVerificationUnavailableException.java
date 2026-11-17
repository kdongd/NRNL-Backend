package com.kdongd.nrnl.exception;

public class GoogleVerificationUnavailableException extends RuntimeException {
    public GoogleVerificationUnavailableException() {
        super("구글 로그인 검증 서비스에 일시적으로 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
}
