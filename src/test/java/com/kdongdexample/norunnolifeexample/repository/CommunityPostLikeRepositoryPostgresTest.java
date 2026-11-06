
package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.CommunityPost;
import com.kdongdexample.norunnolifeexample.domain.User;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class CommunityPostLikeRepositoryPostgresTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

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
        // 테스트마다 중복되지 않는 이메일 생성
        String email = UUID.randomUUID() + "@test.com";

        // 테스트 회원 생성
        User user = userRepository.save(
                User.create(email, "encoded-password")
        );

        userId = user.getId();

        // 테스트 게시글 생성
        CommunityPost post = postRepository.save(
                CommunityPost.create(
                        user,
                        "좋아요 통합 테스트",
                        "PostgreSQL Native SQL 검증"
                )
        );

        postId = post.getId();
    }

    // Native INSERT/DELETE는 트랜잭션 안에서 실행해야 한다.
    // 테스트 메서드 전체에 @Transactional을 적용하지 않고,
    // 실제 요청처럼 각 작업을 별도 트랜잭션으로 실행한다.
    private int insertLike() {
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        return transaction.execute(
                status -> likeRepository.insertLike(postId, userId)
        );
    }

    private int deleteLike() {
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        return transaction.execute(
                status -> likeRepository.deleteLike(postId, userId)
        );
    }

    @Test
    @DisplayName("최초 좋아요 등록 시 DB에 1개 저장되고 ID가 자동 생성된다")
    void insertLike_shouldInsertOneRow() {

        // when
        int inserted = insertLike();

        // then
        assertThat(inserted).isEqualTo(1);
        assertThat(likeRepository.countByPostId(postId))
                .isEqualTo(1);

        assertThat(
                likeRepository.existsByPostIdAndUserId(
                        postId, userId
                )
        ).isTrue();

        // INSERT SQL에서 id를 지정하지 않았지만
        // PostgreSQL IDENTITY에 의해 자동 생성되었는지 확인
        Long generatedId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM community_post_like
                WHERE post_id = ? AND user_id = ?
                """,
                Long.class,
                postId,
                userId
        );

        assertThat(generatedId).isNotNull();
        assertThat(generatedId).isPositive();
    }

    @Test
    @DisplayName("동일 회원의 중복 좋아요는 무시하고 1개만 유지한다")
    void insertLike_duplicate_shouldDoNothing() {

        // when
        int first = insertLike();
        int second = insertLike();

        // then
        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();

        assertThat(likeRepository.countByPostId(postId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("동시에 10번 좋아요를 요청해도 DB에는 1개만 저장된다")
    void insertLike_concurrentRequests_shouldInsertOnlyOne()
            throws Exception {

        int requestCount = 10;

        ExecutorService executor =
                Executors.newFixedThreadPool(requestCount);

        // 모든 작업이 시작 준비를 마쳤는지 확인
        CountDownLatch ready = new CountDownLatch(requestCount);

        // 모든 작업을 최대한 동시에 시작하기 위한 신호
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {

                futures.add(executor.submit(() -> {

                    ready.countDown();

                    // 시작 신호를 기다린다.
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "동시 요청 시작 대기 시간 초과"
                        );
                    }

                    // 각 스레드에서 독립적인 트랜잭션 실행
                    return insertLike();
                }));
            }

            // 모든 스레드가 준비되었는지 확인
            assertThat(
                    ready.await(10, TimeUnit.SECONDS)
            ).isTrue();

            // 10개 요청을 동시에 시작
            start.countDown();

            int successfulInserts = 0;
            int ignoredInserts = 0;

            for (Future<Integer> future : futures) {

                // SQL 오류나 예외가 발생하면 테스트 실패
                int result = future.get(30, TimeUnit.SECONDS);

                if (result == 1) {
                    successfulInserts++;
                } else if (result == 0) {
                    ignoredInserts++;
                } else {
                    throw new AssertionError(
                            "예상하지 못한 INSERT 결과: " + result
                    );
                }
            }

            // 실제 INSERT는 정확히 한 번만 성공
            assertThat(successfulInserts).isEqualTo(1);

            // 나머지 9개 요청은 ON CONFLICT로 무시
            assertThat(ignoredInserts)
                    .isEqualTo(requestCount - 1);

            // DB에 저장된 좋아요는 정확히 1개
            assertThat(likeRepository.countByPostId(postId))
                    .isEqualTo(1);

        } finally {
            // 실패하더라도 대기 중인 스레드를 해제
            start.countDown();
            executor.shutdownNow();

            // 스레드 종료 대기
            assertThat(
                    executor.awaitTermination(
                            10, TimeUnit.SECONDS
                    )
            ).isTrue();
        }
    }

    @Test
    @DisplayName("좋아요 취소 시 DB에서 삭제되고 재요청은 무시된다")
    void deleteLike_shouldRemoveOneRow() {

        // given
        insertLike();

        // when
        int firstDelete = deleteLike();
        int secondDelete = deleteLike();

        // then
        assertThat(firstDelete).isEqualTo(1);
        assertThat(secondDelete).isZero();

        assertThat(likeRepository.countByPostId(postId))
                .isZero();
    }
}
