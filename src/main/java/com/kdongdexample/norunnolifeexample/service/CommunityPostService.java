
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CommunityPost;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.dto.CommunityPostCreateRequest;
import com.kdongdexample.norunnolifeexample.dto.CommunityPostResponse;
import com.kdongdexample.norunnolifeexample.dto.CommunityPostUpdateRequest;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityPostNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.InvalidSortPropertyException;
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
public class CommunityPostService {

    // 게시글 조회 시 허용할 정렬 기준
    private static final Set<String> ALLOWED_SORT_PROPERTIES =
            Set.of("createdAt", "id");

    private final CommunityPostRepository postRepository;
    private final UserRepository userRepository;


    public CommunityPostService(
            CommunityPostRepository postRepository,
            UserRepository userRepository
    ) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    // 게시글 작성
    @Transactional
    public CommunityPostResponse create(
            CommunityPostCreateRequest request,
            Long userId
    ) {
        // 로그인한 회원 조회
        User author = userRepository.findById(userId)
                .orElseThrow(() ->
                        new AuthenticatedUserNotFoundException(userId));

        // 게시글 엔티티 생성
        CommunityPost post = CommunityPost.create(
                author,
                request.title(),
                request.content()
        );

        // DB 저장 후 응답 DTO로 변환
        CommunityPost savedPost = postRepository.save(post);

        return CommunityPostResponse.from(savedPost);
    }

    // 전체 게시글 조회 (정렬 및 페이징)
    public Page<CommunityPostResponse> findAll(Pageable pageable) {
        validateSort(pageable.getSort());

        return postRepository.findAll(pageable)
                .map(CommunityPostResponse::from);
    }

    // 게시글 상세 조회
    public CommunityPostResponse findById(Long postId) {
        CommunityPost post = findPost(postId);

        return CommunityPostResponse.from(post);
    }

    // 제목으로 게시글 검색
    public Page<CommunityPostResponse> searchByTitle(
            String keyword,
            Pageable pageable
    ) {
        validateSort(pageable.getSort());

        return postRepository
                .findByTitleContainingIgnoreCase(keyword, pageable)
                .map(CommunityPostResponse::from);
    }

    // 작성자 닉네임으로 게시글 검색
    public Page<CommunityPostResponse> searchByNickname(
            String nickname,
            Pageable pageable
    ) {
        validateSort(pageable.getSort());

        return postRepository
                .findByAuthorNicknameContainingIgnoreCase(nickname, pageable)
                .map(CommunityPostResponse::from);
    }

    // 특정 회원이 작성한 게시글 조회
    // 내 게시글 조회에도 사용 가능
    public Page<CommunityPostResponse> findByAuthor(
            Long userId,
            Pageable pageable
    ) {
        validateSort(pageable.getSort());

        return postRepository.findByAuthorId(userId, pageable)
                .map(CommunityPostResponse::from);
    }

    // 게시글 수정
    @Transactional
    public CommunityPostResponse update(
            Long postId,
            CommunityPostUpdateRequest request,
            Long userId
    ) {
        // 게시글 조회
        CommunityPost post = findPost(postId);

        // 작성자 본인인지 검증
        validateAuthor(post, userId);

        // 제목 및 본문 수정
        post.update(request.title(), request.content());

        // JPA 변경 감지로 트랜잭션 종료 시 UPDATE 실행
        return CommunityPostResponse.from(post);
    }

    // 게시글 삭제
    @Transactional
    public void delete(Long postId, Long userId) {
        // 게시글 조회
        CommunityPost post = findPost(postId);

        // 작성자 본인인지 검증
        validateAuthor(post, userId);

        // DB에서 게시글 삭제
        postRepository.delete(post);
    }

    // 게시글 조회 공통 메서드
    private CommunityPost findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() ->
                        new CommunityPostNotFoundException(postId));
    }

    // 작성자 권한 검증
    // 다른 회원의 게시글이면 존재하지 않는 것처럼 처리
    private void validateAuthor(CommunityPost post, Long userId) {
        if (!post.getAuthor().getId().equals(userId)) {
            throw new CommunityPostNotFoundException(post.getId());
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
