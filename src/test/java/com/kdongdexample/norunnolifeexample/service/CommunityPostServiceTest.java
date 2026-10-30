
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
class CommunityPostServiceTest {

    @Mock
    private CommunityPostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    private CommunityPostService service;

    private User author;

    private CommunityPost post;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        service = new CommunityPostService(postRepository, userRepository);

        // 테스트용 회원 생성
        author = User.create("runner@test.com", "encoded-password");
        ReflectionTestUtils.setField(author, "id", 1L);
        ReflectionTestUtils.setField(author, "nickname", "러닝왕");

        // 테스트용 게시글 생성
        post = CommunityPost.create(author, "오늘 러닝 완료", "5km 달렸습니다.");
        ReflectionTestUtils.setField(post, "id", 10L);
        ReflectionTestUtils.setField(
                post, "createdAt",
                LocalDateTime.of(2026, 10, 30, 10, 0)
        );
        ReflectionTestUtils.setField(
                post, "updatedAt",
                LocalDateTime.of(2026, 10, 30, 10, 0)
        );

        // 첫 페이지, 10개, 최신순
        pageable = PageRequest.of(
                0, 10, Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    @Test
    @DisplayName("게시글 작성에 성공한다")
    void create_success() {
        CommunityPostCreateRequest request =
                new CommunityPostCreateRequest("새 게시글", "새로운 내용");

        given(userRepository.findById(1L))
                .willReturn(Optional.of(author));

        given(postRepository.save(any(CommunityPost.class)))
                .willAnswer(invocation -> {
                    CommunityPost saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 20L);
                    return saved;
                });

        CommunityPostResponse result = service.create(request, 1L);

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.authorId()).isEqualTo(1L);
        assertThat(result.authorNickname()).isEqualTo("러닝왕");
        assertThat(result.title()).isEqualTo("새 게시글");
        assertThat(result.content()).isEqualTo("새로운 내용");

        verify(postRepository).save(any(CommunityPost.class));
    }

    @Test
    @DisplayName("존재하지 않는 회원은 게시글을 작성할 수 없다")
    void create_userNotFound() {
        CommunityPostCreateRequest request =
                new CommunityPostCreateRequest("제목", "내용");

        given(userRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request, 999L))
                .isInstanceOf(AuthenticatedUserNotFoundException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("전체 게시글을 페이징하여 조회한다")
    void findAll_success() {
        Page<CommunityPost> page =
                new PageImpl<>(List.of(post), pageable, 1);

        given(postRepository.findAll(pageable)).willReturn(page);

        Page<CommunityPostResponse> result = service.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(10L);
        assertThat(result.getContent().get(0).title())
                .isEqualTo("오늘 러닝 완료");
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(postRepository).findAll(pageable);
    }

    @Test
    @DisplayName("존재하는 게시글을 상세 조회한다")
    void findById_success() {
        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        CommunityPostResponse result = service.findById(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.authorNickname()).isEqualTo("러닝왕");
        assertThat(result.content()).isEqualTo("5km 달렸습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 예외가 발생한다")
    void findById_notFound() {
        given(postRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(CommunityPostNotFoundException.class);
    }

    @Test
    @DisplayName("게시글 제목으로 검색한다")
    void searchByTitle_success() {
        Page<CommunityPost> page =
                new PageImpl<>(List.of(post), pageable, 1);

        given(postRepository.findByTitleContainingIgnoreCase("러닝", pageable))
                .willReturn(page);

        Page<CommunityPostResponse> result =
                service.searchByTitle("러닝", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title())
                .contains("러닝");

        verify(postRepository)
                .findByTitleContainingIgnoreCase("러닝", pageable);
    }

    @Test
    @DisplayName("작성자 닉네임으로 게시글을 검색한다")
    void searchByNickname_success() {
        Page<CommunityPost> page =
                new PageImpl<>(List.of(post), pageable, 1);

        given(postRepository.findByAuthorNicknameContainingIgnoreCase(
                "러닝왕", pageable
        )).willReturn(page);

        Page<CommunityPostResponse> result =
                service.searchByNickname("러닝왕", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).authorNickname())
                .isEqualTo("러닝왕");
    }

    @Test
    @DisplayName("특정 회원이 작성한 게시글을 조회한다")
    void findByAuthor_success() {
        Page<CommunityPost> page =
                new PageImpl<>(List.of(post), pageable, 1);

        given(postRepository.findByAuthorId(1L, pageable))
                .willReturn(page);

        Page<CommunityPostResponse> result =
                service.findByAuthor(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).authorId())
                .isEqualTo(1L);

        verify(postRepository).findByAuthorId(1L, pageable);
    }

    @Test
    @DisplayName("작성자 본인은 게시글을 수정할 수 있다")
    void update_success() {
        CommunityPostUpdateRequest request =
                new CommunityPostUpdateRequest(
                        "수정된 제목", "수정된 내용"
                );

        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        CommunityPostResponse result =
                service.update(10L, request, 1L);

        assertThat(result.title()).isEqualTo("수정된 제목");
        assertThat(result.content()).isEqualTo("수정된 내용");
        assertThat(post.getTitle()).isEqualTo("수정된 제목");
        assertThat(post.getContent()).isEqualTo("수정된 내용");

        // JPA 변경 감지를 사용하므로 save()를 다시 호출하지 않는다
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 회원은 게시글을 수정할 수 없다")
    void update_otherUser() {
        CommunityPostUpdateRequest request =
                new CommunityPostUpdateRequest(
                        "남의 글 수정", "수정 시도"
                );

        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        assertThatThrownBy(() ->
                service.update(10L, request, 2L)
        ).isInstanceOf(CommunityPostNotFoundException.class);

        assertThat(post.getTitle()).isEqualTo("오늘 러닝 완료");
        assertThat(post.getContent()).isEqualTo("5km 달렸습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 게시글은 수정할 수 없다")
    void update_notFound() {
        CommunityPostUpdateRequest request =
                new CommunityPostUpdateRequest("수정 제목", "수정 내용");

        given(postRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.update(999L, request, 1L)
        ).isInstanceOf(CommunityPostNotFoundException.class);
    }

    @Test
    @DisplayName("작성자 본인은 게시글을 삭제할 수 있다")
    void delete_success() {
        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        service.delete(10L, 1L);

        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("다른 회원은 게시글을 삭제할 수 없다")
    void delete_otherUser() {
        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        assertThatThrownBy(() ->
                service.delete(10L, 2L)
        ).isInstanceOf(CommunityPostNotFoundException.class);

        verify(postRepository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 게시글은 삭제할 수 없다")
    void delete_notFound() {
        given(postRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.delete(999L, 1L)
        ).isInstanceOf(CommunityPostNotFoundException.class);

        verify(postRepository, never()).delete(any());
    }

    @Test
    @DisplayName("허용되지 않은 정렬 필드는 예외가 발생한다")
    void findAll_invalidSort() {
        Pageable invalidPageable =
                PageRequest.of(0, 10, Sort.by("title"));

        assertThatThrownBy(() ->
                service.findAll(invalidPageable)
        ).isInstanceOf(InvalidSortPropertyException.class);

        verify(postRepository, never()).findAll(any(Pageable.class));
    }
}
