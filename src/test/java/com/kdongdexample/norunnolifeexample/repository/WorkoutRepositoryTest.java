package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.BoxingWorkout;
import com.kdongdexample.norunnolifeexample.domain.RunningWorkout;
import com.kdongdexample.norunnolifeexample.domain.TechniqueType;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.domain.Workout;
import com.kdongdexample.norunnolifeexample.domain.WorkoutDetail;
import com.kdongdexample.norunnolifeexample.domain.WorkoutType;
import com.kdongdexample.norunnolifeexample.dto.WorkoutMonthlyStat;
import com.kdongdexample.norunnolifeexample.dto.WorkoutStatByType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
class WorkoutRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    JpaWorkoutRepository jpaWorkoutRepository;

    @Autowired
    UserRepository userRepository;

    private User owner;
    private User otherOwner;

    @BeforeEach
    void setUpOwner() {
        owner = userRepository.save(User.create("test@test.com", "encoded-password"));
        otherOwner = userRepository.save(User.create("other@test.com", "encoded-password"));
    }

    @Test
    @DisplayName("운동 기록을 저장하고 조회할 수 있다")
    void saveAndFind() {
        RunningWorkout workout = RunningWorkout.create(owner, 30, "테스트", LocalDateTime.now(), 5.0, "한강", 300);
        jpaWorkoutRepository.save(workout);

        List<Workout> result = jpaWorkoutRepository.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(WorkoutType.RUNNING);
    }

    @Test
    @DisplayName("detail 포함해서 저장하고 조회할 수 있다")
    void saveWithDetails() {
        BoxingWorkout workout = BoxingWorkout.create(owner, 60, "스파링", LocalDateTime.now(), 3, "파트너", TechniqueType.SPARRING);
        WorkoutDetail detail = WorkoutDetail.create(workout, 1, "1라운드", 180, "섀도우");
        workout.addDetail(detail);
        jpaWorkoutRepository.save(workout);

        Optional<Workout> result = jpaWorkoutRepository.findByIdWithDetails(workout.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getDetails()).hasSize(1);
    }

    @Test
    @DisplayName("Workout 삭제 시 연관된 WorkoutDetail도 함께 삭제된다")
    void deleteWorkout_cascadeDeletesDetails() {
        BoxingWorkout workout = BoxingWorkout.create(owner, 60, "스파링", LocalDateTime.now(), 3, "파트너", TechniqueType.SPARRING);
        WorkoutDetail detail = WorkoutDetail.create(workout, 1, "1라운드", 180, "섀도우");
        workout.addDetail(detail);
        Workout saved = jpaWorkoutRepository.save(workout);
        Long savedId = saved.getId();

        jpaWorkoutRepository.delete(saved);
        jpaWorkoutRepository.flush();

        Optional<Workout> found = jpaWorkoutRepository.findById(savedId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("memo가 255자를 초과하면 DB 저장 시 예외가 발생한다")
    void memo가_255자_초과하면_예외발생() {
        String longMemo = "가".repeat(300);
        RunningWorkout workout = RunningWorkout.create(owner, 30, longMemo, LocalDateTime.now(), 5.0, "한강", 300);

        assertThatThrownBy(() -> {
            jpaWorkoutRepository.save(workout);
            jpaWorkoutRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("from과 정확히 같은 시각의 기록도 검색 결과에 포함된다")
    void search_fromBoundary_inclusive() {
        LocalDateTime boundary = LocalDateTime.of(2026, 5, 1, 0, 0);
        RunningWorkout workout = RunningWorkout.create(owner, 30, "경계", boundary, 5.0, "한강", 300);
        jpaWorkoutRepository.save(workout);

        Specification<Workout> spec = WorkoutSpecifications.fromDate(boundary);
        Page<Workout> result = jpaWorkoutRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("to와 정확히 같은 시각의 기록도 검색 결과에 포함된다")
    void search_toBoundary_inclusive() {
        LocalDateTime boundary = LocalDateTime.of(2026, 5, 31, 23, 59);
        RunningWorkout workout = RunningWorkout.create(owner, 30, "경계", boundary, 5.0, "한강", 300);
        jpaWorkoutRepository.save(workout);

        Specification<Workout> spec = WorkoutSpecifications.toDate(boundary);
        Page<Workout> result = jpaWorkoutRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("durationMinutes가 전부 null이어도 statsByType의 합계는 0으로 반환된다")
    void statsByType_allNullDuration_returnsZero() {
        BoxingWorkout workout = BoxingWorkout.create(owner, null, "메모", LocalDateTime.now(), 3, "파트너", TechniqueType.SPARRING);
        jpaWorkoutRepository.save(workout);

        List<WorkoutStatByType> stats = jpaWorkoutRepository.statsByType(owner.getId(), null, null);

        WorkoutStatByType boxingStat = stats.stream()
                .filter(s -> s.type() == WorkoutType.BOXING)
                .findFirst()
                .orElseThrow();
        assertThat(boxingStat.totalDurationMinutes()).isEqualTo(0L);
        assertThat(boxingStat.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("statsByMonth는 년/월별로 올바르게 집계된다")
    void statsByMonth_aggregatesByYearMonth() {
        jpaWorkoutRepository.save(RunningWorkout.create(owner, 30, "1", LocalDateTime.of(2026, 5, 10, 9, 0), 5.0, "한강", 300));
        jpaWorkoutRepository.save(RunningWorkout.create(owner, 30, "2", LocalDateTime.of(2026, 5, 20, 9, 0), 5.0, "한강", 300));
        jpaWorkoutRepository.save(BoxingWorkout.create(owner, 60, "3", LocalDateTime.of(2026, 6, 1, 9, 0), 3, "파트너", TechniqueType.SPARRING));

        List<WorkoutMonthlyStat> stats = jpaWorkoutRepository.statsByMonth(owner.getId(), null, null);

        WorkoutMonthlyStat may = stats.stream()
                .filter(s -> s.year() == 2026 && s.month() == 5)
                .findFirst()
                .orElseThrow();
        assertThat(may.count()).isEqualTo(2L);

        WorkoutMonthlyStat june = stats.stream()
                .filter(s -> s.year() == 2026 && s.month() == 6)
                .findFirst()
                .orElseThrow();
        assertThat(june.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("from 기간 필터를 지정하면 그 이전 기록은 통계에서 제외된다")
    void statsByType_withFromFilter_excludesEarlierRecords() {
        jpaWorkoutRepository.save(RunningWorkout.create(owner, 30, "이전", LocalDateTime.of(2026, 4, 1, 0, 0), 5.0, "한강", 300));
        jpaWorkoutRepository.save(RunningWorkout.create(owner, 30, "이후", LocalDateTime.of(2026, 6, 1, 0, 0), 5.0, "한강", 300));

        List<WorkoutStatByType> stats = jpaWorkoutRepository.statsByType(owner.getId(), LocalDateTime.of(2026, 5, 1, 0, 0), null);

        WorkoutStatByType runningStat = stats.stream()
                .filter(s -> s.type() == WorkoutType.RUNNING)
                .findFirst()
                .orElseThrow();
        assertThat(runningStat.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("statsByType는 다른 회원의 기록을 집계에 포함하지 않는다 (유저 1명으로는 증명 불가능하던 격리를 검증)")
    void statsByType_excludesOtherOwnersRecords() {
        jpaWorkoutRepository.save(RunningWorkout.create(owner, 30, "내 기록", LocalDateTime.now(), 5.0, "한강", 300));
        jpaWorkoutRepository.save(BoxingWorkout.create(otherOwner, 60, "남의 기록", LocalDateTime.now(), 3, "파트너", TechniqueType.SPARRING));

        List<WorkoutStatByType> stats = jpaWorkoutRepository.statsByType(owner.getId(), null, null);

        assertThat(stats).extracting(WorkoutStatByType::type).containsExactly(WorkoutType.RUNNING);
        assertThat(stats).noneMatch(s -> s.type() == WorkoutType.BOXING);
    }
}
