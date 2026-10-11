package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.BodyPart;
import com.kdongdexample.norunnolifeexample.domain.RunningWorkout;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.domain.Workout;
import com.kdongdexample.norunnolifeexample.domain.WorkoutCondition;
import com.kdongdexample.norunnolifeexample.dto.WorkoutConditionRequest;
import com.kdongdexample.norunnolifeexample.dto.WorkoutConditionRequest.PainRequest;
import com.kdongdexample.norunnolifeexample.exception.WorkoutConditionNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.WorkoutNotFoundException;
import com.kdongdexample.norunnolifeexample.repository.JpaWorkoutRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import com.kdongdexample.norunnolifeexample.repository.WorkoutConditionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class WorkoutConditionServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private WorkoutConditionService conditionService;

    @Autowired
    private WorkoutService workoutService;

    @Autowired
    private WorkoutConditionRepository conditionRepository;

    @Autowired
    private JpaWorkoutRepository jpaWorkoutRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    private User owner;
    private User otherUser;
    private Workout workout;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.create("condition-owner@test.com", "encoded-password"));
        otherUser = userRepository.save(User.create("condition-other@test.com", "encoded-password"));
        workout = jpaWorkoutRepository.save(
                RunningWorkout.create(owner, 40, "러닝", LocalDateTime.now().minusHours(1), 5.0, "한강", 300));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("통증 부위와 함께 컨디션을 저장하면 다시 조회할 수 있다")
    void upsert_createsConditionWithPains() {
        WorkoutConditionRequest request = new WorkoutConditionRequest(7, 6,
                List.of(new PainRequest(BodyPart.KNEE, 4), new PainRequest(BodyPart.ANKLE, 2)), "무릎 시큰");

        conditionService.upsert(workout.getId(), request, owner.getId());
        flushAndClear();

        WorkoutCondition found = conditionService.findByWorkout(workout.getId(), owner.getId());
        assertThat(found.getRpe()).isEqualTo(7);
        assertThat(found.getBodyCondition()).isEqualTo(6);
        assertThat(found.hasPain()).isTrue();
        assertThat(found.getPains())
                .containsEntry(BodyPart.KNEE, 4)
                .containsEntry(BodyPart.ANKLE, 2)
                .hasSize(2);
        assertThat(found.getMemo()).isEqualTo("무릎 시큰");
    }

    @Test
    @DisplayName("통증 목록 없이 저장하면 통증 없음으로 판정된다")
    void upsert_withoutPains_hasPainIsFalse() {
        conditionService.upsert(workout.getId(), new WorkoutConditionRequest(5, 8, null, null), owner.getId());
        flushAndClear();

        WorkoutCondition found = conditionService.findByWorkout(workout.getId(), owner.getId());
        assertThat(found.hasPain()).isFalse();
        assertThat(found.getPains()).isEmpty();
    }

    @Test
    @DisplayName("같은 운동에 다시 저장하면 새 행이 생기지 않고 통증 목록까지 전부 교체된다")
    void upsert_twice_replacesExistingCondition() {
        conditionService.upsert(workout.getId(), new WorkoutConditionRequest(7, 6,
                List.of(new PainRequest(BodyPart.KNEE, 4), new PainRequest(BodyPart.ANKLE, 2)), "첫 입력"), owner.getId());
        flushAndClear();

        conditionService.upsert(workout.getId(), new WorkoutConditionRequest(5, 8,
                List.of(new PainRequest(BodyPart.SHOULDER, 3)), "수정"), owner.getId());
        flushAndClear();

        assertThat(conditionRepository.count()).isEqualTo(1);
        WorkoutCondition found = conditionService.findByWorkout(workout.getId(), owner.getId());
        assertThat(found.getRpe()).isEqualTo(5);
        assertThat(found.getBodyCondition()).isEqualTo(8);
        assertThat(found.getPains())
                .containsOnlyKeys(BodyPart.SHOULDER)
                .containsEntry(BodyPart.SHOULDER, 3);
        assertThat(found.getMemo()).isEqualTo("수정");
    }

    @Test
    @DisplayName("다른 회원의 운동 기록에는 컨디션을 저장할 수 없다")
    void upsert_otherUsersWorkout_throwsWorkoutNotFound() {
        WorkoutConditionRequest request = new WorkoutConditionRequest(7, 6, null, null);

        assertThatThrownBy(() -> conditionService.upsert(workout.getId(), request, otherUser.getId()))
                .isInstanceOf(WorkoutNotFoundException.class);
        assertThat(conditionRepository.count()).isZero();
    }

    @Test
    @DisplayName("다른 회원의 운동 기록 컨디션은 조회할 수 없다")
    void findByWorkout_otherUsersWorkout_throwsWorkoutNotFound() {
        conditionService.upsert(workout.getId(), new WorkoutConditionRequest(7, 6, null, null), owner.getId());
        flushAndClear();

        assertThatThrownBy(() -> conditionService.findByWorkout(workout.getId(), otherUser.getId()))
                .isInstanceOf(WorkoutNotFoundException.class);
    }

    @Test
    @DisplayName("컨디션을 입력하지 않은 운동 기록을 조회하면 WorkoutConditionNotFoundException이 발생한다")
    void findByWorkout_withoutCondition_throwsNotFound() {
        assertThatThrownBy(() -> conditionService.findByWorkout(workout.getId(), owner.getId()))
                .isInstanceOf(WorkoutConditionNotFoundException.class);
    }

    @Test
    @DisplayName("운동 기록을 삭제하면 컨디션과 통증 기록도 함께 삭제된다 (FK on delete cascade)")
    void deleteWorkout_cascadesConditionAndPains() {
        conditionService.upsert(workout.getId(), new WorkoutConditionRequest(7, 6,
                List.of(new PainRequest(BodyPart.KNEE, 4)), null), owner.getId());
        flushAndClear();

        workoutService.delete(workout.getId(), owner.getId());
        flushAndClear();

        assertThat(conditionRepository.count()).isZero();
        Number painRows = (Number) em.createNativeQuery("select count(*) from workout_condition_pain")
                .getSingleResult();
        assertThat(painRows.longValue()).isZero();
    }
}
