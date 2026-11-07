
package com.kdongdexample.norunnolifeexample.controller;

import com.kdongdexample.norunnolifeexample.config.SecurityConfig;
import com.kdongdexample.norunnolifeexample.dto.CommunityLikeResponse;
import com.kdongdexample.norunnolifeexample.security.CustomAuthenticationEntryPoint;
import com.kdongdexample.norunnolifeexample.security.JwtTokenProvider;
import com.kdongdexample.norunnolifeexample.security.TokenStatus;
import com.kdongdexample.norunnolifeexample.service.CommunityPostLikeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommunityPostLikeController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class
})
@TestPropertySource(properties = {
        "cors.allowed-origins=http://localhost:5173"
})
class CommunityPostLikeSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommunityPostLikeService service;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final String VALID_TOKEN = "valid-test-token";

    // 유효한 JWT를 가진 회원 ID를 1L로 설정
    private void mockValidToken() {
        when(jwtTokenProvider.validate(VALID_TOKEN))
                .thenReturn(TokenStatus.VALID);

        when(jwtTokenProvider.getUserId(VALID_TOKEN))
                .thenReturn(1L);
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글 좋아요를 조회할 수 있다")
    void publicGetLikes() throws Exception {
        when(service.getLikeStatus(10L, null))
                .thenReturn(new CommunityLikeResponse(5L, false));

        mockMvc.perform(get("/community/posts/10/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(5))
                .andExpect(jsonPath("$.likedByMe").value(false));

        verify(service).getLikeStatus(10L, null);
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글 좋아요를 등록할 수 없다")
    void anonymousLikeDenied() throws Exception {
        mockMvc.perform(post("/community/posts/10/likes"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글 좋아요를 취소할 수 없다")
    void anonymousUnlikeDenied() throws Exception {
        mockMvc.perform(delete("/community/posts/10/likes"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 게시글 좋아요를 등록할 수 있다")
    void authenticatedLike() throws Exception {
        mockValidToken();

        mockMvc.perform(post("/community/posts/10/likes")
                        .header(
                                "Authorization",
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isNoContent());

        verify(service).like(10L, 1L);
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 게시글 좋아요를 취소할 수 있다")
    void authenticatedUnlike() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/community/posts/10/likes")
                        .header(
                                "Authorization",
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isNoContent());

        verify(service).unlike(10L, 1L);
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 내 게시글 좋아요 여부를 조회한다")
    void authenticatedGetLikes() throws Exception {
        mockValidToken();

        when(service.getLikeStatus(10L, 1L))
                .thenReturn(new CommunityLikeResponse(5L, true));

        mockMvc.perform(get("/community/posts/10/likes")
                        .header(
                                "Authorization",
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(5))
                .andExpect(jsonPath("$.likedByMe").value(true));

        verify(service).getLikeStatus(10L, 1L);
    }

    @Test
    @DisplayName("유효하지 않은 JWT로 게시글 좋아요를 등록할 수 없다")
    void invalidTokenLikeDenied() throws Exception {
        mockMvc.perform(post("/community/posts/10/likes")
                        .header(
                                "Authorization",
                                "Bearer invalid-token"
                        ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }
}
