
package com.kdongdexample.norunnolifeexample.exception;

public class CommunityCommentNotFoundException extends RuntimeException {

    public CommunityCommentNotFoundException(Long id) {
        super("커뮤니티 댓글을 찾을 수 없습니다. id: " + id);
    }
}
