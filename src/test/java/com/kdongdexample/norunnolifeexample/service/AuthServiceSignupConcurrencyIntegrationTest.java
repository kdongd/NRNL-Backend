
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.ExperienceLevel;
import com.kdongdexample.norunnolifeexample.domain.Gender;
import com.kdongdexample.norunnolifeexample.domain.WorkoutType;
import com.kdongdexample.norunnolifeexample.dto.SignupRequest;
import com.kdongdexample.norunnolifeexample.dto.UserProfileRequest;
import com.kdongdexample.norunnolifeexample.exception.EmailAlreadyExistsException;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class AuthServiceSignupConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("같은 이메일로 동시에 signup()이 호출되면 하나만 성공하고 나머지는 EmailAlreadyExistsException이 발생한다")
    void signup_concurrentRequestsWithSameEmail_onlyOneSucceeds() throws Exception {
        String email = "race-signup@test.com";

        UserProfileRequest profile = new UserProfileRequest(
                Gender.MAN,
                28,
                175.0,
                70.0,
                ExperienceLevel.INTERMEDIATE,
                Set.of(WorkoutType.RUNNING),
                "10km 완주"
        );

        SignupRequest request = new SignupRequest(
                email,
                "password1234",
                "동시가입테스트",
                profile
        );

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        Callable<Void> task = () -> {
            barrier.await();
            authService.signup(request);
            return null;
        };

        List<Future<Void>> futures = executor.invokeAll(List.of(task, task));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        int successCount = 0;
        int failureCount = 0;
        for (Future<Void> future : futures) {
            try {
                future.get();
                successCount++;
            } catch (Exception e) {
                assertThat(e.getCause()).isInstanceOf(EmailAlreadyExistsException.class);
                failureCount++;
            }
        }

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);
        assertThat(userRepository.existsByEmail(email)).isTrue();
    }
}
