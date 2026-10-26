package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.RunningWorkout;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.dto.WorkoutMonthlyStat;
import com.kdongdexample.norunnolifeexample.dto.WorkoutStatByType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class JpaWorkoutRepositoryPostgresTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    JpaWorkoutRepository repository;

    @Autowired
    UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUpOwner() {
        owner = userRepository.save(User.create("test@test.com", "encoded-password"));
    }

    @Test
    void statsByMonth_shouldExtractYearAndMonth_withNullRange_onPostgres() {
        repository.save(RunningWorkout.create(
                owner, 30, "Postgres 테스트", LocalDateTime.of(2026, 7, 16, 10, 0),
                5.0, "한강", 300
        ));

        List<WorkoutMonthlyStat> result = repository.statsByMonth(owner.getId(), null, null);

        assertThat(result).isNotEmpty();
        assertThat(result)
                .anySatisfy(stat -> {
                    assertThat(stat.year()).isEqualTo(2026);
                    assertThat(stat.month()).isEqualTo(7);
                });
    }

    @Test
    void statsByType_withNullRange_onPostgres() {
        repository.save(RunningWorkout.create(
                owner, 30, "Postgres 테스트", LocalDateTime.of(2026, 7, 16, 10, 0),
                5.0, "한강", 300
        ));

        List<WorkoutStatByType> result = repository.statsByType(owner.getId(), null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).count()).isEqualTo(1L);
        assertThat(result.get(0).totalDurationMinutes()).isEqualTo(30L);
    }
}
