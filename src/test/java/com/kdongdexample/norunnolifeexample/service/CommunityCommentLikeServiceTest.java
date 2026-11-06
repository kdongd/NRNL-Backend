
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CommunityComment;
import com.kdongdexample.norunnolifeexample.domain.CommunityPost;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.dto.CommunityLikeResponse;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityCommentNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityPostNotFoundException;
import com.kdongdexample.norunnolifeexample.repository.CommunityCommentLikeRepository;
import com.kdongdexample.norunnolifeexample.repository.CommunityCommentRepository;
import com.kdongdexample.norunnolifeexample.repository.CommunityPostRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityCommentLikeServiceTest {

    @Mock
    private CommunityCommentLikeRepository likeRepository;

    @Mock
    private CommunityCommentRepository commentRepository;

    @Mock
    private CommunityPostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    private CommunityCommentLikeService service;
    private CommunityComment comment;

    @BeforeEach
    void setUp() {
        service = new CommunityCommentLikeService(
                likeRepository,
                commentRepository,
                postRepository,
                userRepository
        );

        User author = User.create(
                "runner@test.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(author, "id", 1L);

        CommunityPost post = CommunityPost.create(
                author,
                "오늘 러닝 완료",
                "5km 달렸습니다."
        );
        ReflectionTestUtils.setField(post, "id", 10L);

        comment = CommunityComment.create(
                post,
                author,
                "수고하셨습니다!"
        );
        ReflectionTestUtils.setField(comment, "id", 100L);
    }

    private void givenExistingComment() {
        given(postRepository.existsById(10L)).willReturn(true);
        given(commentRepository.findById(100L))
                .willReturn(Optional.of(comment));
    }

    @Test
    @DisplayName("댓글 좋아요 등록에 성공한다")
    void like_success() {
        givenExistingComment();
        given(userRepository.existsById(1L)).willReturn(true);
        given(likeRepository.insertLike(100L, 1L)).willReturn(1);

        service.like(10L, 100L, 1L);

        verify(likeRepository).insertLike(100L, 1L);
    }

    @Test
    @DisplayName("동일 댓글에 중복 좋아요 요청 시 정상 처리한다")
    void like_duplicate() {
        givenExistingComment();
        given(userRepository.existsById(1L)).willReturn(true);
        given(likeRepository.insertLike(100L, 1L)).willReturn(0);

        assertThatCode(() -> service.like(10L, 100L, 1L))
                .doesNotThrowAnyException();

        verify(likeRepository).insertLike(100L, 1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 댓글에는 좋아요할 수 없다")
    void like_postNotFound() {
        given(postRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> service.like(999L, 100L, 1L))
                .isInstanceOf(CommunityPostNotFoundException.class);

        verifyNoInteractions(commentRepository, likeRepository);
    }

    @Test
    @DisplayName("존재하지 않는 댓글에는 좋아요할 수 없다")
    void like_commentNotFound() {
        given(postRepository.existsById(10L)).willReturn(true);
        given(commentRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.like(10L, 999L, 1L))
                .isInstanceOf(CommunityCommentNotFoundException.class);

        verifyNoInteractions(likeRepository);
    }

    @Test
    @DisplayName("다른 게시글에 속한 댓글에는 좋아요할 수 없다")
    void like_wrongPost() {
        given(postRepository.existsById(20L)).willReturn(true);
        given(commentRepository.findById(100L))
                .willReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.like(20L, 100L, 1L))
                .isInstanceOf(CommunityCommentNotFoundException.class);

        verifyNoInteractions(likeRepository);
    }

    @Test
    @DisplayName("존재하지 않는 회원은 댓글에 좋아요할 수 없다")
    void like_userNotFound() {
        givenExistingComment();
        given(userRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> service.like(10L, 100L, 999L))
                .isInstanceOf(AuthenticatedUserNotFoundException.class);

        verifyNoInteractions(likeRepository);
    }

    @Test
    @DisplayName("댓글 좋아요 취소에 성공한다")
    void unlike_success() {
        givenExistingComment();
        given(userRepository.existsById(1L)).willReturn(true);
        given(likeRepository.deleteLike(100L, 1L)).willReturn(1);

        service.unlike(10L, 100L, 1L);

        verify(likeRepository).deleteLike(100L, 1L);
    }

    @Test
    @DisplayName("좋아요하지 않은 댓글을 취소해도 정상 처리한다")
    void unlike_notLiked() {
        givenExistingComment();
        given(userRepository.existsById(1L)).willReturn(true);
        given(likeRepository.deleteLike(100L, 1L)).willReturn(0);

        assertThatCode(() -> service.unlike(10L, 100L, 1L))
                .doesNotThrowAnyException();

        verify(likeRepository).deleteLike(100L, 1L);
    }

    @Test
    @DisplayName("다른 게시글에 속한 댓글의 좋아요는 취소할 수 없다")
    void unlike_wrongPost() {
        given(postRepository.existsById(20L)).willReturn(true);
        given(commentRepository.findById(100L))
                .willReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.unlike(20L, 100L, 1L))
                .isInstanceOf(CommunityCommentNotFoundException.class);

        verifyNoInteractions(likeRepository);
    }

    @Test
    @DisplayName("존재하지 않는 회원은 댓글 좋아요를 취소할 수 없다")
    void unlike_userNotFound() {
        givenExistingComment();
        given(userRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> service.unlike(10L, 100L, 999L))
                .isInstanceOf(AuthenticatedUserNotFoundException.class);

        verifyNoInteractions(likeRepository);
    }

    @Test
    @DisplayName("댓글 좋아요 수와 내 좋아요 여부를 조회한다")
    void getLikeStatus_liked() {
        givenExistingComment();
        given(likeRepository.countByCommentId(100L)).willReturn(7L);
        given(likeRepository.existsByCommentIdAndUserId(100L, 1L))
                .willReturn(true);

        CommunityLikeResponse result =
                service.getLikeStatus(10L, 100L, 1L);

        assertThat(result.likeCount()).isEqualTo(7);
        assertThat(result.likedByMe()).isTrue();
    }

    @Test
    @DisplayName("좋아요하지 않은 댓글은 내 좋아요 여부가 false이다")
    void getLikeStatus_notLiked() {
        givenExistingComment();
        given(likeRepository.countByCommentId(100L)).willReturn(3L);
        given(likeRepository.existsByCommentIdAndUserId(100L, 1L))
                .willReturn(false);

        CommunityLikeResponse result =
                service.getLikeStatus(10L, 100L, 1L);

        assertThat(result.likeCount()).isEqualTo(3);
        assertThat(result.likedByMe()).isFalse();
    }

    @Test
    @DisplayName("비로그인 사용자는 댓글 좋아요 수만 조회한다")
    void getLikeStatus_anonymous() {
        givenExistingComment();
        given(likeRepository.countByCommentId(100L)).willReturn(4L);

        CommunityLikeResponse result =
                service.getLikeStatus(10L, 100L, null);

        assertThat(result.likeCount()).isEqualTo(4);
        assertThat(result.likedByMe()).isFalse();

        verify(likeRepository, never())
                .existsByCommentIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 댓글의 좋아요는 조회할 수 없다")
    void getLikeStatus_commentNotFound() {
        given(postRepository.existsById(10L)).willReturn(true);
        given(commentRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getLikeStatus(10L, 999L, 1L)
        ).isInstanceOf(CommunityCommentNotFoundException.class);

        verifyNoInteractions(likeRepository);
    }

    @Test
    @DisplayName("다른 게시글에 속한 댓글의 좋아요는 조회할 수 없다")
    void getLikeStatus_wrongPost() {
        given(postRepository.existsById(20L)).willReturn(true);
        given(commentRepository.findById(100L))
                .willReturn(Optional.of(comment));

        assertThatThrownBy(() ->
                service.getLikeStatus(20L, 100L, 1L)
        ).isInstanceOf(CommunityCommentNotFoundException.class);

        verifyNoInteractions(likeRepository);
    }
}
