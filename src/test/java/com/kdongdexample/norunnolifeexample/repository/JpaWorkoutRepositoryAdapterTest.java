package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.BoxingWorkout;
import com.kdongdexample.norunnolifeexample.domain.RunningWorkout;
import com.kdongdexample.norunnolifeexample.domain.TechniqueType;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.domain.Workout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(JpaWorkoutRepositoryAdapter.class)
class JpaWorkoutRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private JpaWorkoutRepositoryAdapter adapter;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User otherOwner;

    @BeforeEach
    void setUpOwner() {
        owner = userRepository.save(User.create("test@test.com", "encoded-password"));
        otherOwner = userRepository.save(User.create("other@test.com", "encoded-password"));
    }

    @Test
    @DisplayName("distanceKm으로 정렬하면 복싱 기록도 포함되고 null은 마지막으로 온다")
    void sortByDistanceKmIncludesBoxingWorkoutsWithNullsLast() {

        RunningWorkout running10km = RunningWorkout.create(
                owner, 60, "메모", LocalDateTime.now().minusDays(1), 10.0, "한강", 500);
        RunningWorkout running5km = RunningWorkout.create(
                owner, 30, "메모", LocalDateTime.now().minusDays(2), 5.0, "한강", 300);
        BoxingWorkout boxing = BoxingWorkout.create(
                owner, 45, "메모", LocalDateTime.now().minusDays(3), 3, "파트너", TechniqueType.SPARRING);

        adapter.save(running10km);
        adapter.save(running5km);
        adapter.save(boxing);


        Page<Workout> result = adapter.search(
                owner.getId(), null, null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "distanceKm")));


        List<Workout> content = result.getContent();
        assertThat(content).hasSize(3);
        assertThat(content.get(0)).isEqualTo(running5km);
        assertThat(content.get(1)).isEqualTo(running10km);
        assertThat(content.get(2)).isEqualTo(boxing); // distanceKm null -> 마지막
    }

    @Test
    @DisplayName("distanceKm 정렬 시 count 쿼리도 정상적으로 전체 개수를 반환한다")
    void countQueryReturnsCorrectTotalWhenSortingByDistanceKm() {

        adapter.save(RunningWorkout.create(
                owner, 60, "메모", LocalDateTime.now(), 10.0, "한강", 500));
        adapter.save(RunningWorkout.create(
                owner, 30, "메모", LocalDateTime.now(), 5.0, "한강", 300));
        adapter.save(BoxingWorkout.create(
                owner, 45, "메모", LocalDateTime.now(), 3, "파트너", TechniqueType.SPARRING));


        Page<Workout> result = adapter.search(
                owner.getId(), null, null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "distanceKm")));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("search는 다른 회원이 소유한 운동 기록을 결과에 포함하지 않는다 (유저 1명으로는 증명 불가능하던 격리를 검증)")
    void search_excludesOtherOwnersWorkouts() {
        adapter.save(RunningWorkout.create(owner, 30, "내 기록1", LocalDateTime.now(), 5.0, "한강", 300));
        adapter.save(RunningWorkout.create(owner, 30, "내 기록2", LocalDateTime.now(), 5.0, "한강", 300));
        adapter.save(BoxingWorkout.create(otherOwner, 60, "남의 기록", LocalDateTime.now(), 3, "파트너", TechniqueType.SPARRING));

        Page<Workout> result = adapter.search(owner.getId(), null, null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(w -> w.getOwner().equals(owner));
    }
}
