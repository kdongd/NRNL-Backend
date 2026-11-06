
package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.CommunityCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityCommentLikeRepository
        extends JpaRepository<CommunityCommentLike, Long> {

    // 댓글 좋아요 수 조회
    long countByCommentId(Long commentId);

    // 특정 회원의 댓글 좋아요 여부 조회
    boolean existsByCommentIdAndUserId(
            Long commentId,
            Long userId
    );

    // 댓글 좋아요 등록
    // 동일 회원의 중복 좋아요 요청 무시
    @Modifying
    @Query(
            value = """
                    INSERT INTO community_comment_like (comment_id, user_id)
                    VALUES (:commentId, :userId)
                    ON CONFLICT (comment_id, user_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertLike(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId
    );

    // 댓글 좋아요 취소
    // 좋아요가 없는 경우에도 오류 없이 처리
    @Modifying
    @Query(
            value = """
                    DELETE FROM community_comment_like
                    WHERE comment_id = :commentId
                      AND user_id = :userId
                    """,
            nativeQuery = true
    )
    int deleteLike(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId
    );
}
