package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.AuthProvider;
import com.kdongdexample.norunnolifeexample.domain.ExperienceLevel;
import com.kdongdexample.norunnolifeexample.domain.Gender;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.domain.UserProfile;
import com.kdongdexample.norunnolifeexample.domain.WorkoutType;
import com.kdongdexample.norunnolifeexample.dto.UserProfileRequest;
import com.kdongdexample.norunnolifeexample.exception.UserProfileAlreadyExistsException;
import com.kdongdexample.norunnolifeexample.repository.UserProfileRepository;
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
class UserProfileServiceConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    @DisplayName("같은 유저가 동시에 프로필 작성을 시도하면 하나만 성공하고 나머지는 UserProfileAlreadyExistsException이 발생한다")
    void createProfile_concurrentRequestsForSameUser_onlyOneSucceeds() throws Exception {
        User user = userRepository.save(
                User.createOAuth("profile-race@test.com", AuthProvider.GOOGLE, "google-sub-profile-race"));
        Long userId = user.getId();

        UserProfileRequest request = new UserProfileRequest(Gender.MAN, 28, 175.0, 70.0,
                ExperienceLevel.INTERMEDIATE, Set.of(WorkoutType.RUNNING), "10km 완주");

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        Callable<UserProfile> task = () -> {
            barrier.await();
            return userProfileService.createProfile(userId, request);
        };

        List<Future<UserProfile>> futures = executor.invokeAll(List.of(task, task));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        int successCount = 0;
        int failureCount = 0;
        for (Future<UserProfile> future : futures) {
            try {
                future.get();
                successCount++;
            } catch (Exception e) {
                assertThat(e.getCause()).isInstanceOf(UserProfileAlreadyExistsException.class);
                failureCount++;
            }
        }

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);
        assertThat(userProfileRepository.existsByUserId(userId)).isTrue();
    }
}
