
package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    // 전체 게시글 조회 시 작성자 정보도 함께 조회하여 N+1 문제 방지
    @Override
    @EntityGraph(attributePaths = "author")
    Page<CommunityPost> findAll(Pageable pageable);

    // 특정 회원이 작성한 게시글 조회
    // 작성자 정보도 함께 조회하여 N+1 문제 방지
    @EntityGraph(attributePaths = "author")
    Page<CommunityPost> findByAuthorId(Long userId, Pageable pageable);

    // 게시글 제목에 특정 검색어가 포함된 게시글 조회
    // 작성자 정보도 함께 조회하여 N+1 문제 방지
    @EntityGraph(attributePaths = "author")
    Page<CommunityPost> findByTitleContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    // 작성자 닉네임에 특정 검색어가 포함된 게시글 조회
    // 작성자 정보도 함께 조회하여 N+1 문제 방지
    @EntityGraph(attributePaths = "author")
    Page<CommunityPost> findByAuthorNicknameContainingIgnoreCase(
            String nickname,
            Pageable pageable
    );
}
