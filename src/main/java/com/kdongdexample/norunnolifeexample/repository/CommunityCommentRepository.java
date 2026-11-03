
package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.CommunityComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCommentRepository
        extends JpaRepository<CommunityComment, Long> {

    // 특정 게시글에 작성된 댓글 목록 조회
    Page<CommunityComment> findByPostId(
            Long postId,
            Pageable pageable
    );

    // 특정 회원이 작성한 댓글 목록 조회
    Page<CommunityComment> findByAuthorId(
            Long authorId,
            Pageable pageable
    );
}
