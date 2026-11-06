
package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.CommunityPost;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityPostRepository
        extends JpaRepository<CommunityPost, Long> {

    // 전체 게시글 조회 시 작성자 정보도 함께 조회하여 N+1 문제 방지
    @Override
    @EntityGraph(attributePaths = "author")
    Page<CommunityPost> findAll(Pageable pageable);

    // 특정 회원이 작성한 게시글 조회
    // 작성자 정보도 함께 조회하여 N+1 문제 방지
    @EntityGraph(attributePaths = "author")
    Page<CommunityPost> findByAuthorId(
            Long userId,
            Pageable pageable
    );

    // 게시글 제목 검색
    // 작성자 정보도 함께 조회하여 N+1 문제 방지
    @EntityGraph(attributePaths = "author")
    Page<CommunityPost> findByTitleContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    // 작성자 닉네임 검색
    // 작성자 정보도 함께 조회하여 N+1 문제 방지
    @EntityGraph(attributePaths = "author")
    Page<CommunityPost> findByAuthorNicknameContainingIgnoreCase(
            String nickname,
            Pageable pageable
    );

    // 좋아요 등록 시 게시글을 잠금 상태로 조회
    // 다른 트랜잭션이 게시글을 동시에 삭제하지 못하도록 방지
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            SELECT p
            FROM CommunityPost p
            WHERE p.id = :postId
            """)
    Optional<CommunityPost> findByIdWithLock(
            @Param("postId") Long postId
    );
}
