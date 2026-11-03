
package com.kdongdexample.norunnolifeexample.exception;

public class CommunityPostForbiddenException extends RuntimeException {

    public CommunityPostForbiddenException() {
        super("다른 회원의 게시글은 수정하거나 삭제할 수 없습니다.");
    }
}
