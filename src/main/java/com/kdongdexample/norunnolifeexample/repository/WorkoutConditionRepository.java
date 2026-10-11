package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.WorkoutCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkoutConditionRepository extends JpaRepository<WorkoutCondition, Long> {

    // 응답 변환이 트랜잭션 밖(컨트롤러)에서 일어나므로 통증 목록을 함께 fetch join
    @Query("select c from WorkoutCondition c left join fetch c.pains where c.workout.id = :workoutId")
    Optional<WorkoutCondition> findByWorkoutIdWithPains(@Param("workoutId") Long workoutId);
}
