package com.kdongd.nrnl.exception;

public class UserProfileAlreadyExistsException extends RuntimeException {
    public UserProfileAlreadyExistsException(Long userId) {
        super("이미 프로필이 존재하는 회원입니다: userId=" + userId);
    }
}
