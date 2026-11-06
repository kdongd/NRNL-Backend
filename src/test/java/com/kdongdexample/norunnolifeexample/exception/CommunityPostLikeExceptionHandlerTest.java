
package com.kdongdexample.norunnolifeexample.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

class CommunityPostLikeExceptionHandlerTest {

    private CommunityPostLikeExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CommunityPostLikeExceptionHandler();
    }

    @Test
    @DisplayName("게시글 좋아요 FK 위반은 404를 반환한다")
    void postLikeForeignKeyViolation_returns404() {

        SQLException sqlException = new SQLException(
                """
                insert or update on table "community_post_like"
                violates foreign key constraint
                "fk_community_post_like_post"
                """,
                "23503"
        );

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "게시글 좋아요 FK 위반",
                        sqlException
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode().value())
                .isEqualTo(404);

        assertThat(response.getBody())
                .isNotNull();
    }

    @Test
    @DisplayName("다른 FK 위반은 게시글 없음으로 처리하지 않는다")
    void otherForeignKeyViolation_returns500() {

        SQLException sqlException = new SQLException(
                """
                violates foreign key constraint
                "fk_community_post_like_user"
                """,
                "23503"
        );

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "회원 FK 위반",
                        sqlException
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode().value())
                .isEqualTo(500);
    }

    @Test
    @DisplayName("UNIQUE 제약조건 위반은 404로 처리하지 않는다")
    void uniqueViolation_returns500() {

        SQLException sqlException = new SQLException(
                """
                violates unique constraint
                "uk_community_post_like_post_user"
                """,
                "23505"
        );

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "UNIQUE 제약조건 위반",
                        sqlException
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode().value())
                .isEqualTo(500);
    }
}
