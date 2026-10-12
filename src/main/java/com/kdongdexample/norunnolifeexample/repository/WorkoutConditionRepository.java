package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.WorkoutCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkoutConditionRepository extends JpaRepository<WorkoutCondition, Long> {

    // 응답 변환이 트랜잭션 밖(컨트롤러)에서 일어나므로 통증 목록을 함께 fetch join
    @Query("select c from WorkoutCondition c left join fetch c.pains where c.workout.id = :workoutId")
    Optional<WorkoutCondition> findByWorkoutIdWithPains(@Param("workoutId") Long workoutId);

    // 코칭 판정용: 회원의 기간 내 컨디션 전체. 날짜별로 묶어야 해서 운동 기록(운동 일시)과 통증을 함께 가져옵니다.
    @Query("select distinct c from WorkoutCondition c " +
            "join fetch c.workout w " +
            "left join fetch c.pains " +
            "where w.owner.id = :userId " +
            "and w.workoutDateTime >= :from " +
            "and w.workoutDateTime < :to")
    List<WorkoutCondition> findAllByOwnerAndWorkoutDateRange(@Param("userId") Long userId,
                                                             @Param("from") LocalDateTime from,
                                                             @Param("to") LocalDateTime to);
}
