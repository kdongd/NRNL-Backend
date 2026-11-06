
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.CommunityPost;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.repository.CommunityPostLikeRepository;
import com.kdongdexample.norunnolifeexample.repository.CommunityPostRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
@SpringBootTest
class CommunityPostLikeConcurrencyPostgresTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private CommunityPostLikeService likeService;

    @Autowired
    private CommunityPostLikeRepository likeRepository;

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long postId;
    private Long userId;

    @BeforeEach
    void setUp() {

        // 테스트마다 새로운 회원 생성
        User user = userRepository.save(
                User.create(
                        UUID.randomUUID() + "@test.com",
                        "encoded-password"
                )
        );

        userId = user.getId();

        // 테스트 게시글 생성
        CommunityPost post = postRepository.save(
                CommunityPost.create(
                        user,
                        "동시성 테스트 게시글",
                        "좋아요 등록과 게시글 삭제 충돌 검증"
                )
        );

        postId = post.getId();
    }

    @Test
    @DisplayName(
            "좋아요 등록 트랜잭션이 잠금을 보유하면 게시글 삭제가 대기한다"
    )
    void like_shouldBlockConcurrentPostDeletion()
            throws Exception {

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        // 좋아요 INSERT가 완료되고
        // 트랜잭션이 아직 열려 있다는 신호
        CountDownLatch likeInserted =
                new CountDownLatch(1);

        // 좋아요 트랜잭션을 커밋해도 된다는 신호
        CountDownLatch allowCommit =
                new CountDownLatch(1);

        Future<?> likeFuture = null;

        try {

            // 트랜잭션 A: 좋아요 등록
            likeFuture = executor.submit(() -> {

                TransactionTemplate transaction =
                        new TransactionTemplate(transactionManager);

                transaction.executeWithoutResult(status -> {

                    // 서비스 내부에서
                    // PESSIMISTIC_READ 잠금 획득 후 INSERT
                    likeService.like(postId, userId);

                    // INSERT 완료, 아직 커밋 전
                    likeInserted.countDown();

                    try {

                        // 테스트 스레드가 허용할 때까지
                        // 트랜잭션을 종료하지 않는다.
                        if (!allowCommit.await(
                                10, TimeUnit.SECONDS
                        )) {
                            throw new IllegalStateException(
                                    "좋아요 트랜잭션 대기 시간 초과"
                            );
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                });
            });

            // 좋아요 INSERT 및 잠금 획득 완료 대기
            assertThat(
                    likeInserted.await(
                            10, TimeUnit.SECONDS
                    )
            ).isTrue();

            // 트랜잭션 B: 게시글 삭제 시도
            TransactionTemplate deleteTransaction =
                    new TransactionTemplate(transactionManager);

            // PostgreSQL 잠금 대기 시간을 750ms로 제한
            // 잠금이 정상 동작하면 삭제는 이 시간 안에
            // 진행되지 못하고 lock_timeout 오류 발생
            Throwable deletionError = catchThrowable(() ->
                    deleteTransaction.executeWithoutResult(
                            status -> {

                                jdbcTemplate.execute(
                                        "SET LOCAL lock_timeout = '750ms'"
                                );

                                jdbcTemplate.update(
                                        """
                                        DELETE FROM community_post
                                        WHERE id = ?
                                        """,
                                        postId
                                );
                            }
                    )
            );

            // 잠금 때문에 삭제 요청이 실패해야 한다.
            assertThat(deletionError)
                    .as("게시글 삭제는 잠금 때문에 대기해야 한다")
                    .isNotNull();

            // PostgreSQL lock_not_available SQLSTATE
            assertThat(hasSqlState(
                    deletionError, "55P03"
            )).isTrue();

            // 좋아요 트랜잭션 커밋 허용
            allowCommit.countDown();

            // 좋아요 등록이 정상 완료되었는지 확인
            likeFuture.get(10, TimeUnit.SECONDS);

            // 좋아요가 실제 DB에 저장되었는지 확인
            assertThat(
                    likeRepository.existsByPostIdAndUserId(
                            postId, userId
                    )
            ).isTrue();

            // 잠금 해제 후 게시글 삭제 재시도
            int deleted = deleteTransaction.execute(
                    status -> jdbcTemplate.update(
                            """
                            DELETE FROM community_post
                            WHERE id = ?
                            """,
                            postId
                    )
            );

            assertThat(deleted).isEqualTo(1);

            // 게시글 삭제 확인
            assertThat(
                    postRepository.existsById(postId)
            ).isFalse();

            // FK ON DELETE CASCADE로 좋아요도 삭제
            assertThat(
                    likeRepository.countByPostId(postId)
            ).isZero();

        } finally {

            // 테스트 실패 시에도 트랜잭션 해제
            allowCommit.countDown();

            executor.shutdownNow();

            assertThat(
                    executor.awaitTermination(
                            15, TimeUnit.SECONDS
                    )
            ).isTrue();
        }
    }

    // 예외의 원인 체인을 따라가며
    // PostgreSQL SQLSTATE를 확인한다.
    private boolean hasSqlState(
            Throwable error,
            String expectedState
    ) {

        Throwable current = error;

        while (current != null) {

            if (current instanceof SQLException sqlException) {

                if (expectedState.equals(
                        sqlException.getSQLState()
                )) {
                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }
}
