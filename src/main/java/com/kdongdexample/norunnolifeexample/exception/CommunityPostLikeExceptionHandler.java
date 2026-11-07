
package com.kdongdexample.norunnolifeexample.exception;

import com.kdongdexample.norunnolifeexample.controller.CommunityPostLikeController;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.List;

@RestControllerAdvice(
        assignableTypes = CommunityPostLikeController.class
)
public class CommunityPostLikeExceptionHandler {

    // PostgreSQL 외래 키 제약조건 위반
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    // Flyway V8에 정의된 게시글 좋아요 FK 이름
    private static final String POST_LIKE_FK =
            "fk_community_post_like_post";

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException e
    ) {

        // 삭제된 게시글에 좋아요 INSERT를 시도한 경우
        if (isPostLikeForeignKeyViolation(e)) {

            ErrorResponse response = new ErrorResponse(
                    HttpStatus.NOT_FOUND.value(),
                    "좋아요 대상 게시글을 찾을 수 없습니다.",
                    List.of()
            );

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(response);
        }

        // 다른 DB 무결성 오류를 404로 잘못 처리하지 않음
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "좋아요 처리 중 데이터베이스 오류가 발생했습니다.",
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    // FK 위반인지, 어떤 FK가 위반되었는지 구분
    private boolean isPostLikeForeignKeyViolation(Throwable error) {

        boolean foreignKeyViolation = false;
        boolean postLikeConstraint = false;

        Throwable current = error;

        while (current != null) {

            // Hibernate가 추출한 제약조건 이름 확인
            if (current instanceof ConstraintViolationException exception) {

                if (POST_LIKE_FK.equals(exception.getConstraintName())) {
                    postLikeConstraint = true;
                }
            }

            // PostgreSQL SQLSTATE 확인
            if (current instanceof SQLException sqlException) {

                if (FOREIGN_KEY_VIOLATION.equals(
                        sqlException.getSQLState()
                )) {
                    foreignKeyViolation = true;

                    // Hibernate가 제약조건 이름을 추출하지 못한 경우
                    // PostgreSQL 오류 메시지에서 제약조건 이름 확인
                    if (sqlException.getMessage() != null
                            && sqlException.getMessage().contains(
                            "\"" + POST_LIKE_FK + "\""
                    )) {
                        postLikeConstraint = true;
                    }
                }
            }

            current = current.getCause();
        }

        return foreignKeyViolation && postLikeConstraint;
    }
}
