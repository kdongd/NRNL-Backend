
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CommunityPost;
import com.kdongdexample.norunnolifeexample.dto.CommunityLikeResponse;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.CommunityPostNotFoundException;
import com.kdongdexample.norunnolifeexample.repository.CommunityPostLikeRepository;
import com.kdongdexample.norunnolifeexample.repository.CommunityPostRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityPostLikeServiceTest {

    @Mock
    private CommunityPostLikeRepository likeRepository;

    @Mock
    private CommunityPostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityPost post;

    private CommunityPostLikeService service;

    @BeforeEach
    void setUp() {
        service = new CommunityPostLikeService(
                likeRepository,
                postRepository,
                userRepository
        );
    }

    @Test
    @DisplayName("게시글 좋아요 등록에 성공한다")
    void like_success() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(postRepository.findByIdWithLock(10L))
                .willReturn(Optional.of(post));
        given(likeRepository.insertLike(10L, 1L)).willReturn(1);

        service.like(10L, 1L);

        verify(postRepository).findByIdWithLock(10L);
        verify(likeRepository).insertLike(10L, 1L);
    }

    @Test
    @DisplayName("동일 게시글에 중복 좋아요 요청 시 정상 처리한다")
    void like_duplicate() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(postRepository.findByIdWithLock(10L))
                .willReturn(Optional.of(post));
        given(likeRepository.insertLike(10L, 1L)).willReturn(0);

        assertThatCode(() -> service.like(10L, 1L))
                .doesNotThrowAnyException();

        verify(postRepository).findByIdWithLock(10L);
        verify(likeRepository).insertLike(10L, 1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 좋아요를 등록할 수 없다")
    void like_postNotFound() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(postRepository.findByIdWithLock(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.like(999L, 1L))
                .isInstanceOf(CommunityPostNotFoundException.class);

        verify(postRepository).findByIdWithLock(999L);
        verifyNoInteractions(likeRepository);
    }

    @Test
    @DisplayName("존재하지 않는 회원은 좋아요를 등록할 수 없다")
    void like_userNotFound() {
        given(userRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> service.like(10L, 999L))
                .isInstanceOf(AuthenticatedUserNotFoundException.class);

        verifyNoInteractions(postRepository, likeRepository);
    }

    @Test
    @DisplayName("게시글 좋아요 취소에 성공한다")
    void unlike_success() {
        given(postRepository.existsById(10L)).willReturn(true);
        given(userRepository.existsById(1L)).willReturn(true);
        given(likeRepository.deleteLike(10L, 1L)).willReturn(1);

        service.unlike(10L, 1L);

        verify(likeRepository).deleteLike(10L, 1L);
    }

    @Test
    @DisplayName("좋아요가 없는 게시글을 취소해도 정상 처리한다")
    void unlike_notLiked() {
        given(postRepository.existsById(10L)).willReturn(true);
        given(userRepository.existsById(1L)).willReturn(true);
        given(likeRepository.deleteLike(10L, 1L)).willReturn(0);

        assertThatCode(() -> service.unlike(10L, 1L))
                .doesNotThrowAnyException();

        verify(likeRepository).deleteLike(10L, 1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 좋아요는 취소할 수 없다")
    void unlike_postNotFound() {
        given(postRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> service.unlike(999L, 1L))
                .isInstanceOf(CommunityPostNotFoundException.class);

        verifyNoInteractions(userRepository, likeRepository);
    }

    @Test
    @DisplayName("존재하지 않는 회원은 좋아요를 취소할 수 없다")
    void unlike_userNotFound() {
        given(postRepository.existsById(10L)).willReturn(true);
        given(userRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> service.unlike(10L, 999L))
                .isInstanceOf(AuthenticatedUserNotFoundException.class);

        verifyNoInteractions(likeRepository);
    }

    @Test
    @DisplayName("게시글 좋아요 수와 내 좋아요 여부를 조회한다")
    void getLikeStatus_liked() {
        given(postRepository.existsById(10L)).willReturn(true);
        given(likeRepository.countByPostId(10L)).willReturn(5L);
        given(likeRepository.existsByPostIdAndUserId(10L, 1L))
                .willReturn(true);

        CommunityLikeResponse result =
                service.getLikeStatus(10L, 1L);

        assertThat(result.likeCount()).isEqualTo(5);
        assertThat(result.likedByMe()).isTrue();
    }

    @Test
    @DisplayName("좋아요하지 않은 회원의 좋아요 여부는 false이다")
    void getLikeStatus_notLiked() {
        given(postRepository.existsById(10L)).willReturn(true);
        given(likeRepository.countByPostId(10L)).willReturn(3L);
        given(likeRepository.existsByPostIdAndUserId(10L, 1L))
                .willReturn(false);

        CommunityLikeResponse result =
                service.getLikeStatus(10L, 1L);

        assertThat(result.likeCount()).isEqualTo(3);
        assertThat(result.likedByMe()).isFalse();
    }

    @Test
    @DisplayName("비로그인 사용자는 좋아요 수만 조회한다")
    void getLikeStatus_anonymous() {
        given(postRepository.existsById(10L)).willReturn(true);
        given(likeRepository.countByPostId(10L)).willReturn(5L);

        CommunityLikeResponse result =
                service.getLikeStatus(10L, null);

        assertThat(result.likeCount()).isEqualTo(5);
        assertThat(result.likedByMe()).isFalse();

        verify(likeRepository, never())
                .existsByPostIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 좋아요는 조회할 수 없다")
    void getLikeStatus_postNotFound() {
        given(postRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> service.getLikeStatus(999L, 1L))
                .isInstanceOf(CommunityPostNotFoundException.class);

        verifyNoInteractions(likeRepository);
    }
}
