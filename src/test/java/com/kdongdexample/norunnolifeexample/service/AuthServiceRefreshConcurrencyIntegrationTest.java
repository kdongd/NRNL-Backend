package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.AuthProvider;
import com.kdongdexample.norunnolifeexample.domain.RefreshToken;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.dto.AuthTokens;
import com.kdongdexample.norunnolifeexample.exception.InvalidRefreshTokenException;
import com.kdongdexample.norunnolifeexample.repository.RefreshTokenRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import com.kdongdexample.norunnolifeexample.security.RefreshTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// AuthService.refresh()에 건 PESSIMISTIC_WRITE 락(findByTokenHashForUpdate)이 실제로 동시 요청을 막아주는지 검증합니다.
@Testcontainers
@SpringBootTest
class AuthServiceRefreshConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenProvider refreshTokenProvider;

    @Test
    @DisplayName("같은 리프레시 토큰으로 동시에 refresh()가 호출되면 하나만 성공하고, " +
            "성공한 쪽이 새로 발급한 토큰까지 포함해 해당 family 전체가 폐기된다")
    void refresh_concurrentRequestsWithSameToken_onlyOneSucceedsAndWholeFamilyIsRevoked() throws Exception {
        User user = userRepository.save(
                User.createOAuth("concurrency-test@test.com", AuthProvider.GOOGLE, "google-sub-concurrency"));

        String rawToken = refreshTokenProvider.generate();
        String tokenHash = refreshTokenProvider.hash(rawToken);
        String tokenFamily = UUID.randomUUID().toString();
        refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), tokenHash, tokenFamily, LocalDateTime.now().plusDays(1)));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        // 두 스레드가 각자 커넥션을 잡아둔 채로 진짜 같은 순간에 refresh()를 호출하도록
        // 강제로 맞춰준다. 이게 없으면 스레드 스케줄링에 따라 그냥 순차 실행처럼 돼버려서
        // 락이 실제로 막아주는 상황 자체가 재현이 안 될 수 있다.
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        Callable<AuthTokens> task = () -> {
            barrier.await();
            return authService.refresh(rawToken);
        };

        List<Future<AuthTokens>> futures = executor.invokeAll(List.of(task, task));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        int successCount = 0;
        int failureCount = 0;
        for (Future<AuthTokens> future : futures) {
            try {
                future.get();
                successCount++;
            } catch (Exception e) {
                assertThat(e.getCause()).isInstanceOf(InvalidRefreshTokenException.class);
                failureCount++;
            }
        }

        // 락이 없었다면 둘 다 revoked=false 스냅샷을 읽어서 successCount == 2가 나왔을 것.
        // 락이 제대로 걸려 있으면 나중에 락을 획득한 쪽은 반드시 변경된 revoked=true를
        // 보게 되어 재사용 탐지로 실패한다.
        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);

        // 재사용 탐지는 공격/우연한 동시 요청을 구분하지 않으므로, 성공한 쪽이 새로 발급한
        // 토큰까지 포함해 해당 family 전체가 폐기되어야 한다 (RTR 설계상 정상 동작).
        List<RefreshToken> allTokensInFamily = refreshTokenRepository.findAll().stream()
                .filter(token -> token.getTokenFamily().equals(tokenFamily))
                .toList();
        assertThat(allTokensInFamily).isNotEmpty();
        assertThat(allTokensInFamily).allSatisfy(token -> assertThat(token.isRevoked()).isTrue());
    }
}
