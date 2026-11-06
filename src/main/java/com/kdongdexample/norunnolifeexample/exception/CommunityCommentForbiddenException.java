
package com.kdongdexample.norunnolifeexample.exception;

public class CommunityCommentForbiddenException extends RuntimeException {

    public CommunityCommentForbiddenException() {
        super("다른 회원의 댓글은 수정하거나 삭제할 수 없습니다.");
    }
}
