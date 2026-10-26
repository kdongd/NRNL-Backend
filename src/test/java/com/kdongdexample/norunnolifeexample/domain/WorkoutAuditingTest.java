package com.kdongdexample.norunnolifeexample.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.kdongdexample.norunnolifeexample.config.AuditingConfig;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(AuditingConfig.class)
class WorkoutAuditingTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private EntityManager em;

    private User persistOwner() {
        User owner = User.create("test@test.com", "encoded-password");
        em.persist(owner);
        return owner;
    }

    @Test
    @DisplayName("엔티티 저장 시 createdAt과 updatedAt이 자동으로 채워진다")
    void createdAtAndUpdatedAtAreSetOnPersist() {

        User owner = persistOwner();
        RunningWorkout workout = RunningWorkout.create(
                owner, 30, "메모", LocalDateTime.now(), 5.0, "한강", 300);


        em.persist(workout);
        em.flush();
        em.clear();


        RunningWorkout found = em.find(RunningWorkout.class, workout.getId());
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("엔티티 수정 시 updatedAt만 갱신되고 createdAt은 유지된다")
    void updatedAtChangesButCreatedAtStaysOnModify() {

        User owner = persistOwner();
        RunningWorkout workout = RunningWorkout.create(
                owner, 30, "메모", LocalDateTime.now(), 5.0, "한강", 300);
        em.persist(workout);
        em.flush();
        em.clear();

        RunningWorkout saved = em.find(RunningWorkout.class, workout.getId());
        LocalDateTime originalCreatedAt = saved.getCreatedAt();
        LocalDateTime originalUpdatedAt = saved.getUpdatedAt();


        saved.update(60, "수정된 메모", LocalDateTime.now(), 10.0, "남산", 500);
        em.flush();
        em.clear();


        RunningWorkout updated = em.find(RunningWorkout.class, workout.getId());
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isNotEqualTo(originalUpdatedAt);
    }
}
