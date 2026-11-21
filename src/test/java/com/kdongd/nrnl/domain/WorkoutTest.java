package com.kdongd.nrnl.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutTest {

    private final User owner = User.create("test@test.com", "encoded-password");

    @Test
    @DisplayName("RunningWorkout.create()로 운동 기록을 생성할 수 있다")
    void create_러닝_생성() {
        LocalDateTime now = LocalDateTime.now();
        RunningWorkout workout = RunningWorkout.create(owner, 30, "테스트 메모", now, 5.0, "한강", 300);

        assertThat(workout.getType()).isEqualTo(WorkoutType.RUNNING);
        assertThat(workout.getDurationMinutes()).isEqualTo(30);
        assertThat(workout.getMemo()).isEqualTo("테스트 메모");
        assertThat(workout.getWorkoutDateTime()).isEqualTo(now);
        assertThat(workout.getDistanceKm()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("BoxingWorkout.create()로 운동 기록을 생성할 수 있다")
    void create_복싱_생성() {
        LocalDateTime now = LocalDateTime.now();
        BoxingWorkout workout = BoxingWorkout.create(owner, 60, "스파링", now, 3, "파트너", TechniqueType.SPARRING);

        assertThat(workout.getType()).isEqualTo(WorkoutType.BOXING);
        assertThat(workout.getRounds()).isEqualTo(3);
    }

    @Test
    @DisplayName("addDetail()로 WorkoutDetail을 추가할 수 있다")
    void addDetail_디테일_추가() {
        BoxingWorkout workout = BoxingWorkout.create(owner, 60, null, LocalDateTime.now(), 3, "파트너", TechniqueType.SPARRING);
        WorkoutDetail detail = WorkoutDetail.create(workout, 1, "1라운드", 180, "섀도우");

        workout.addDetail(detail);

        assertThat(workout.getDetails()).hasSize(1);
        assertThat(workout.getDetails().get(0).getLabel()).isEqualTo("1라운드");
    }
}
