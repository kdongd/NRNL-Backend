
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CommunityComment;
import com.kdongdexample.norunnolifeexample.domain.CommunityPost;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.dto.CommunityCommentCreateRequest;
import com.kdongdexample.norunnolifeexample.dto.CommunityCommentResponse;
import com.kdongdexample.norunnolifeexample.dto.CommunityCommentUpdateRequest;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityCommentNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityPostNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.InvalidSortPropertyException;
import com.kdongdexample.norunnolifeexample.repository.CommunityCommentRepository;
import com.kdongdexample.norunnolifeexample.repository.CommunityPostRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityCommentServiceTest {

    @Mock
    private CommunityCommentRepository commentRepository;

    @Mock
    private CommunityPostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    private CommunityCommentService service;

    private User author;
    private CommunityPost post;
    private CommunityComment comment;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        service = new CommunityCommentService(
                commentRepository,
                postRepository,
                userRepository
        );

        // 테스트용 회원
        author = User.create(
                "runner@test.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(author, "id", 1L);
        ReflectionTestUtils.setField(author, "nickname", "러닝왕");

        // 테스트용 게시글
        post = CommunityPost.create(
                author,
                "오늘 러닝 완료",
                "5km 달렸습니다."
        );
        ReflectionTestUtils.setField(post, "id", 10L);

        // 테스트용 댓글
        comment = CommunityComment.create(
                post,
                author,
                "수고하셨습니다!"
        );
        ReflectionTestUtils.setField(comment, "id", 100L);
        ReflectionTestUtils.setField(
                comment,
                "createdAt",
                LocalDateTime.of(2026, 11, 3, 10, 0)
        );
        ReflectionTestUtils.setField(
                comment,
                "updatedAt",
                LocalDateTime.of(2026, 11, 3, 10, 0)
        );

        pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );
    }

    @Test
    @DisplayName("댓글 작성에 성공한다")
    void create_success() {
        CommunityCommentCreateRequest request =
                new CommunityCommentCreateRequest("운동 멋집니다!");

        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        given(userRepository.findById(1L))
                .willReturn(Optional.of(author));

        given(commentRepository.save(any(CommunityComment.class)))
                .willAnswer(invocation -> {
                    CommunityComment saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 101L);
                    return saved;
                });

        CommunityCommentResponse result =
                service.create(10L, request, 1L);

        assertThat(result.id()).isEqualTo(101L);
        assertThat(result.postId()).isEqualTo(10L);
        assertThat(result.authorId()).isEqualTo(1L);
        assertThat(result.authorNickname()).isEqualTo("러닝왕");
        assertThat(result.content()).isEqualTo("운동 멋집니다!");

        verify(commentRepository).save(any(CommunityComment.class));
    }

    @Test
    @DisplayName("존재하지 않는 게시글에는 댓글을 작성할 수 없다")
    void create_postNotFound() {
        CommunityCommentCreateRequest request =
                new CommunityCommentCreateRequest("댓글 내용");

        given(postRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.create(999L, request, 1L)
        ).isInstanceOf(CommunityPostNotFoundException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 회원은 댓글을 작성할 수 없다")
    void create_userNotFound() {
        CommunityCommentCreateRequest request =
                new CommunityCommentCreateRequest("댓글 내용");

        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        given(userRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.create(10L, request, 999L)
        ).isInstanceOf(AuthenticatedUserNotFoundException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("특정 게시글의 댓글 목록을 조회한다")
    void findByPost_success() {
        Page<CommunityComment> page =
                new PageImpl<>(List.of(comment), pageable, 1);

        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        given(commentRepository.findByPostId(10L, pageable))
                .willReturn(page);

        Page<CommunityCommentResponse> result =
                service.findByPost(10L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id())
                .isEqualTo(100L);
        assertThat(result.getContent().get(0).postId())
                .isEqualTo(10L);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(commentRepository).findByPostId(10L, pageable);
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 댓글 목록은 조회할 수 없다")
    void findByPost_postNotFound() {
        given(postRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.findByPost(999L, pageable)
        ).isInstanceOf(CommunityPostNotFoundException.class);

        verify(commentRepository, never())
                .findByPostId(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("내가 작성한 댓글 목록을 조회한다")
    void findMyComments_success() {
        Page<CommunityComment> page =
                new PageImpl<>(List.of(comment), pageable, 1);

        given(commentRepository.findByAuthorId(1L, pageable))
                .willReturn(page);

        Page<CommunityCommentResponse> result =
                service.findMyComments(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).authorId())
                .isEqualTo(1L);
        assertThat(result.getContent().get(0).content())
                .isEqualTo("수고하셨습니다!");
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(commentRepository).findByAuthorId(1L, pageable);
    }

    @Test
    @DisplayName("작성한 댓글이 없으면 빈 페이지를 반환한다")
    void findMyComments_empty() {
        given(commentRepository.findByAuthorId(1L, pageable))
                .willReturn(Page.empty(pageable));

        Page<CommunityCommentResponse> result =
                service.findMyComments(1L, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("작성자 본인은 댓글을 수정할 수 있다")
    void update_success() {
        CommunityCommentUpdateRequest request =
                new CommunityCommentUpdateRequest("수정된 댓글");

        given(commentRepository.findById(100L))
                .willReturn(Optional.of(comment));

        CommunityCommentResponse result =
                service.update(10L, 100L, request, 1L);

        assertThat(result.content()).isEqualTo("수정된 댓글");
        assertThat(comment.getContent()).isEqualTo("수정된 댓글");

        // JPA 변경 감지를 사용하므로 save()를 다시 호출하지 않는다
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 회원은 댓글을 수정할 수 없다")
    void update_otherUser() {
        CommunityCommentUpdateRequest request =
                new CommunityCommentUpdateRequest("남의 댓글 수정");

        given(commentRepository.findById(100L))
                .willReturn(Optional.of(comment));

        assertThatThrownBy(() ->
                service.update(10L, 100L, request, 2L)
        ).isInstanceOf(CommunityCommentNotFoundException.class);

        assertThat(comment.getContent())
                .isEqualTo("수고하셨습니다!");
    }

    @Test
    @DisplayName("다른 게시글의 댓글은 수정할 수 없다")
    void update_wrongPost() {
        CommunityCommentUpdateRequest request =
                new CommunityCommentUpdateRequest("수정 시도");

        given(commentRepository.findById(100L))
                .willReturn(Optional.of(comment));

        assertThatThrownBy(() ->
                service.update(20L, 100L, request, 1L)
        ).isInstanceOf(CommunityCommentNotFoundException.class);

        assertThat(comment.getContent())
                .isEqualTo("수고하셨습니다!");
    }

    @Test
    @DisplayName("존재하지 않는 댓글은 수정할 수 없다")
    void update_notFound() {
        CommunityCommentUpdateRequest request =
                new CommunityCommentUpdateRequest("수정 내용");

        given(commentRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.update(10L, 999L, request, 1L)
        ).isInstanceOf(CommunityCommentNotFoundException.class);
    }

    @Test
    @DisplayName("작성자 본인은 댓글을 삭제할 수 있다")
    void delete_success() {
        given(commentRepository.findById(100L))
                .willReturn(Optional.of(comment));

        service.delete(10L, 100L, 1L);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("다른 회원은 댓글을 삭제할 수 없다")
    void delete_otherUser() {
        given(commentRepository.findById(100L))
                .willReturn(Optional.of(comment));

        assertThatThrownBy(() ->
                service.delete(10L, 100L, 2L)
        ).isInstanceOf(CommunityCommentNotFoundException.class);

        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("다른 게시글의 댓글은 삭제할 수 없다")
    void delete_wrongPost() {
        given(commentRepository.findById(100L))
                .willReturn(Optional.of(comment));

        assertThatThrownBy(() ->
                service.delete(20L, 100L, 1L)
        ).isInstanceOf(CommunityCommentNotFoundException.class);

        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 댓글은 삭제할 수 없다")
    void delete_notFound() {
        given(commentRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.delete(10L, 999L, 1L)
        ).isInstanceOf(CommunityCommentNotFoundException.class);

        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("게시글 댓글 조회 시 허용되지 않은 정렬 필드는 예외가 발생한다")
    void findByPost_invalidSort() {
        Pageable invalidPageable =
                PageRequest.of(0, 10, Sort.by("content"));

        assertThatThrownBy(() ->
                service.findByPost(10L, invalidPageable)
        ).isInstanceOf(InvalidSortPropertyException.class);

        verifyNoInteractions(postRepository);
        verify(commentRepository, never())
                .findByPostId(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("내 댓글 조회 시 허용되지 않은 정렬 필드는 예외가 발생한다")
    void findMyComments_invalidSort() {
        Pageable invalidPageable =
                PageRequest.of(0, 10, Sort.by("author"));

        assertThatThrownBy(() ->
                service.findMyComments(1L, invalidPageable)
        ).isInstanceOf(InvalidSortPropertyException.class);

        verify(commentRepository, never())
                .findByAuthorId(any(), any(Pageable.class));
    }
}
