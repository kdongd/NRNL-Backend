
package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.CommunityPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostLikeRepository
        extends JpaRepository<CommunityPostLike, Long> {

    // 게시글 좋아요 수 조회
    long countByPostId(Long postId);

    // 특정 회원의 게시글 좋아요 여부 조회
    boolean existsByPostIdAndUserId(
            Long postId,
            Long userId
    );

    // 게시글 좋아요 등록
    // 중복 요청은 무시하여 동시 요청에서도 중복 저장 방지
    @Modifying
    @Query(
            value = """
                    INSERT INTO community_post_like (post_id, user_id)
                    VALUES (:postId, :userId)
                    ON CONFLICT (post_id, user_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertLike(
            @Param("postId") Long postId,
            @Param("userId") Long userId
    );

    // 게시글 좋아요 취소
    // 좋아요가 존재하지 않아도 오류 없이 처리
    @Modifying
    @Query(
            value = """
                    DELETE FROM community_post_like
                    WHERE post_id = :postId
                      AND user_id = :userId
                    """,
            nativeQuery = true
    )
    int deleteLike(
            @Param("postId") Long postId,
            @Param("userId") Long userId
    );
}
