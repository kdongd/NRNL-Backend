
package com.kdongdexample.norunnolifeexample.exception;

public class CommunityPostNotFoundException extends RuntimeException {

    public CommunityPostNotFoundException(Long id) {
        super("커뮤니티 게시글을 찾을 수 없습니다. id: " + id);
    }
}
