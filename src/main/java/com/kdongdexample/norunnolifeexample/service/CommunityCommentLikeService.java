
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CommunityComment;
import com.kdongdexample.norunnolifeexample.dto.CommunityLikeResponse;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityCommentNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityPostNotFoundException;
import com.kdongdexample.norunnolifeexample.repository.CommunityCommentLikeRepository;
import com.kdongdexample.norunnolifeexample.repository.CommunityCommentRepository;
import com.kdongdexample.norunnolifeexample.repository.CommunityPostRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommunityCommentLikeService {

    private final CommunityCommentLikeRepository likeRepository;
    private final CommunityCommentRepository commentRepository;
    private final CommunityPostRepository postRepository;
    private final UserRepository userRepository;

    public CommunityCommentLikeService(
            CommunityCommentLikeRepository likeRepository,
            CommunityCommentRepository commentRepository,
            CommunityPostRepository postRepository,
            UserRepository userRepository
    ) {
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    // 댓글 좋아요 등록
    @Transactional
    public void like(
            Long postId,
            Long commentId,
            Long userId
    ) {
        // 댓글 존재 여부 및 게시글 소속 검증 (404)
        validateComment(postId, commentId);

        // 로그인한 회원 존재 여부 검증
        validateUser(userId);

        // 중복 좋아요는 DB에서 무시
        likeRepository.insertLike(commentId, userId);
    }

    // 댓글 좋아요 취소
    @Transactional
    public void unlike(
            Long postId,
            Long commentId,
            Long userId
    ) {
        // 댓글 존재 여부 및 게시글 소속 검증 (404)
        validateComment(postId, commentId);

        // 로그인한 회원 존재 여부 검증
        validateUser(userId);

        // 좋아요가 없어도 정상 처리
        likeRepository.deleteLike(commentId, userId);
    }

    // 댓글 좋아요 수 및 내 좋아요 여부 조회
    public CommunityLikeResponse getLikeStatus(
            Long postId,
            Long commentId,
            Long userId
    ) {
        // 댓글 존재 여부 및 게시글 소속 검증 (404)
        validateComment(postId, commentId);

        // 댓글 좋아요 수 조회
        long likeCount = likeRepository.countByCommentId(commentId);

        // 비로그인 사용자는 좋아요 여부를 false로 반환
        boolean likedByMe = userId != null
                && likeRepository.existsByCommentIdAndUserId(
                commentId,
                userId
        );

        return new CommunityLikeResponse(
                likeCount,
                likedByMe
        );
    }

    // 게시글 존재 여부 및 댓글 소속 검증
    private void validateComment(
            Long postId,
            Long commentId
    ) {
        // 게시글 존재 여부 검증 (404)
        if (!postRepository.existsById(postId)) {
            throw new CommunityPostNotFoundException(postId);
        }

        // 댓글 존재 여부 검증 (404)
        CommunityComment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new CommunityCommentNotFoundException(commentId));

        // URL의 게시글 ID와 실제 댓글의 게시글 ID 비교
        if (!comment.getPost().getId().equals(postId)) {
            throw new CommunityCommentNotFoundException(commentId);
        }
    }

    // 로그인한 회원 존재 여부 검증
    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new AuthenticatedUserNotFoundException(userId);
        }
    }
}
