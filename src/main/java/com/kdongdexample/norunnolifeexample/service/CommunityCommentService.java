
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CommunityComment;
import com.kdongdexample.norunnolifeexample.domain.CommunityPost;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.dto.CommunityCommentCreateRequest;
import com.kdongdexample.norunnolifeexample.dto.CommunityCommentResponse;
import com.kdongdexample.norunnolifeexample.dto.CommunityCommentUpdateRequest;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityCommentForbiddenException;
import com.kdongdexample.norunnolifeexample.exception.CommunityCommentNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityPostNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.InvalidSortPropertyException;
import com.kdongdexample.norunnolifeexample.repository.CommunityCommentRepository;
import com.kdongdexample.norunnolifeexample.repository.CommunityPostRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CommunityCommentService {

    // 댓글 조회 시 허용할 정렬 기준
    private static final Set<String> ALLOWED_SORT_PROPERTIES =
            Set.of("createdAt", "id");

    private final CommunityCommentRepository commentRepository;
    private final CommunityPostRepository postRepository;
    private final UserRepository userRepository;

    public CommunityCommentService(
            CommunityCommentRepository commentRepository,
            CommunityPostRepository postRepository,
            UserRepository userRepository
    ) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    // 댓글 작성
    @Transactional
    public CommunityCommentResponse create(
            Long postId,
            CommunityCommentCreateRequest request,
            Long userId
    ) {
        // 댓글을 작성할 게시글 조회
        CommunityPost post = findPost(postId);

        // 로그인한 회원 조회
        User author = userRepository.findById(userId)
                .orElseThrow(() ->
                        new AuthenticatedUserNotFoundException(userId));

        // 댓글 엔티티 생성
        CommunityComment comment = CommunityComment.create(
                post,
                author,
                request.content()
        );

        // DB 저장
        CommunityComment savedComment =
                commentRepository.save(comment);

        // 응답 DTO 변환
        return CommunityCommentResponse.from(savedComment);
    }

    // 특정 게시글의 댓글 목록 조회
    public Page<CommunityCommentResponse> findByPost(
            Long postId,
            Pageable pageable
    ) {
        // 허용된 정렬 필드인지 검증
        validateSort(pageable.getSort());

        // 존재하는 게시글인지 검증
        if (!postRepository.existsById(postId)) {
            throw new CommunityPostNotFoundException(postId);
        }

        // 게시글에 작성된 댓글만 조회
        return commentRepository.findByPostId(postId, pageable)
                .map(CommunityCommentResponse::from);
    }

    // 내가 작성한 댓글 목록 조회
    public Page<CommunityCommentResponse> findMyComments(
            Long userId,
            Pageable pageable
    ) {
        // 허용된 정렬 필드인지 검증
        validateSort(pageable.getSort());

        // 로그인한 회원이 작성한 댓글만 조회
        return commentRepository.findByAuthorId(userId, pageable)
                .map(CommunityCommentResponse::from);
    }

    // 댓글 수정
    @Transactional
    public CommunityCommentResponse update(
            Long postId,
            Long commentId,
            CommunityCommentUpdateRequest request,
            Long userId
    ) {
        // 댓글 존재 여부 및 게시글 소속 검증 (404)
        CommunityComment comment = findComment(postId, commentId);

        // 댓글 작성자 권한 검증 (403)
        validateAuthor(comment, userId);

        // 댓글 내용 수정
        comment.update(request.content());

        // JPA 변경 감지로 UPDATE 처리
        return CommunityCommentResponse.from(comment);
    }

    // 댓글 삭제
    @Transactional
    public void delete(
            Long postId,
            Long commentId,
            Long userId
    ) {
        // 댓글 존재 여부 및 게시글 소속 검증 (404)
        CommunityComment comment = findComment(postId, commentId);

        // 댓글 작성자 권한 검증 (403)
        validateAuthor(comment, userId);

        // 댓글 삭제
        commentRepository.delete(comment);
    }

    // 게시글 조회 공통 메서드
    private CommunityPost findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() ->
                        new CommunityPostNotFoundException(postId));
    }

    // 댓글 조회 및 게시글 소속 검증
    private CommunityComment findComment(
            Long postId,
            Long commentId
    ) {
        CommunityComment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new CommunityCommentNotFoundException(commentId));

        // URL의 게시글 ID와 실제 댓글의 게시글 ID 비교
        if (!comment.getPost().getId().equals(postId)) {
            throw new CommunityCommentNotFoundException(commentId);
        }

        return comment;
    }

    // 댓글 작성자 권한 검증
    private void validateAuthor(
            CommunityComment comment,
            Long userId
    ) {
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new CommunityCommentForbiddenException();
        }
    }

    // 허용되지 않은 정렬 필드 차단
    private void validateSort(Sort sort) {
        sort.forEach(order -> {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new InvalidSortPropertyException(
                        order.getProperty(),
                        ALLOWED_SORT_PROPERTIES
                );
            }
        });
    }
}
