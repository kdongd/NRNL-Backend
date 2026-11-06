
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.dto.CommunityLikeResponse;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityPostNotFoundException;
import com.kdongdexample.norunnolifeexample.repository.CommunityPostLikeRepository;
import com.kdongdexample.norunnolifeexample.repository.CommunityPostRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommunityPostLikeService {

    private final CommunityPostLikeRepository likeRepository;
    private final CommunityPostRepository postRepository;
    private final UserRepository userRepository;

    public CommunityPostLikeService(
            CommunityPostLikeRepository likeRepository,
            CommunityPostRepository postRepository,
            UserRepository userRepository
    ) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    // 게시글 좋아요 등록
    @Transactional
    public void like(Long postId, Long userId) {

        // 로그인한 회원 존재 여부 검증
        validateUser(userId);

        // 게시글을 잠금 상태로 조회
        // 좋아요 등록 중 게시글이 삭제되는 문제 방지
        validatePostWithLock(postId);

        // 중복 좋아요는 DB에서 무시
        likeRepository.insertLike(postId, userId);
    }

    // 게시글 좋아요 취소
    @Transactional
    public void unlike(Long postId, Long userId) {

        // 게시글 존재 여부 검증 (404)
        validatePost(postId);

        // 로그인한 회원 존재 여부 검증
        validateUser(userId);

        // 좋아요가 없어도 정상 처리
        likeRepository.deleteLike(postId, userId);
    }

    // 게시글 좋아요 수 및 내 좋아요 여부 조회
    public CommunityLikeResponse getLikeStatus(
            Long postId,
            Long userId
    ) {

        // 게시글 존재 여부 검증 (404)
        validatePost(postId);

        // 게시글 좋아요 수 조회
        long likeCount = likeRepository.countByPostId(postId);

        // 비로그인 사용자는 좋아요 여부를 false로 반환
        boolean likedByMe = userId != null
                && likeRepository.existsByPostIdAndUserId(
                postId,
                userId
        );

        return new CommunityLikeResponse(
                likeCount,
                likedByMe
        );
    }

    // 일반 게시글 존재 여부 검증
    private void validatePost(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new CommunityPostNotFoundException(postId);
        }
    }

    // 좋아요 등록 시 잠금을 이용한 게시글 존재 여부 검증
    private void validatePostWithLock(Long postId) {
        postRepository.findByIdWithLock(postId)
                .orElseThrow(() ->
                        new CommunityPostNotFoundException(postId));
    }

    // 로그인한 회원 존재 여부 검증
    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new AuthenticatedUserNotFoundException(userId);
        }
    }
}
