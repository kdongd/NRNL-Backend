
package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    // 특정 회원이 작성한 게시글 조회
    // 정렬 방식과 페이지 크기는 Pageable에서 결정
    Page<CommunityPost> findByAuthorId(Long userId, Pageable pageable);

    // 게시글 제목에 특정 검색어가 포함된 게시글 조회
    Page<CommunityPost> findByTitleContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    // 작성자 닉네임에 특정 검색어가 포함된 게시글 조회
    Page<CommunityPost> findByAuthorNicknameContainingIgnoreCase(
            String nickname,
            Pageable pageable
    );
}
