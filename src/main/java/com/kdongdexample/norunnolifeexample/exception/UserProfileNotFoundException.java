
package com.kdongdexample.norunnolifeexample.exception;

public class UserProfileNotFoundException extends RuntimeException {

    public UserProfileNotFoundException(Long userId) {
        super("운동 프로필을 찾을 수 없습니다. userId=" + userId);
    }
}
