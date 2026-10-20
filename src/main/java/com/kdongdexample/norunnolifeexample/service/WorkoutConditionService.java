package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.BodyPart;
import com.kdongdexample.norunnolifeexample.domain.Workout;
import com.kdongdexample.norunnolifeexample.domain.WorkoutCondition;
import com.kdongdexample.norunnolifeexample.dto.WorkoutConditionRequest;
import com.kdongdexample.norunnolifeexample.exception.WorkoutConditionConflictException;
import com.kdongdexample.norunnolifeexample.exception.WorkoutConditionNotFoundException;
import com.kdongdexample.norunnolifeexample.repository.CoachingCacheRepository;
import com.kdongdexample.norunnolifeexample.repository.WorkoutConditionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class WorkoutConditionService {

    private final WorkoutService workoutService;
    private final WorkoutConditionRepository conditionRepository;
    private final CoachingCacheRepository coachingCacheRepository;

    public WorkoutConditionService(WorkoutService workoutService,
                                   WorkoutConditionRepository conditionRepository,
                                   CoachingCacheRepository coachingCacheRepository) {
        this.workoutService = workoutService;
        this.conditionRepository = conditionRepository;
        this.coachingCacheRepository = coachingCacheRepository;
    }

    // 없으면 생성, 있으면 전체 덮어쓰기
    @Transactional
    public WorkoutCondition upsert(Long workoutId, WorkoutConditionRequest request, Long userId) {
        // 컨디션이 바뀌면 코칭 입력이 바뀌므로 저장된 코칭을 지운다.
        // 수정(PUT)은 컨디션 id가 그대로라 캐시 키만으로는 변경을 알 수 없다.
        coachingCacheRepository.deleteByUserId(userId);

        // 운동 기록 존재 여부 + 본인 소유 검증 (남의 기록이면 WorkoutNotFoundException)
        Workout workout = workoutService.findById(workoutId, userId);
        Map<BodyPart, Integer> pains = request.toPainMap();

        Optional<WorkoutCondition> existing = conditionRepository.findByWorkoutIdWithPains(workoutId);
        if (existing.isPresent()) {
            WorkoutCondition condition = existing.get();
            condition.update(request.rpe(), request.bodyCondition(), pains, request.memo());
            return condition;
        }

        WorkoutCondition condition = WorkoutCondition.create(
                workout, request.rpe(), request.bodyCondition(), pains, request.memo());

        // 같은 운동에 대한 저장 요청이 동시에 들어오면 둘 다 "없음"을 보고 INSERT를 시도한다.
        try {
            return conditionRepository.saveAndFlush(condition);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                throw new WorkoutConditionConflictException(workoutId);
            }
            throw e;   // 길이 초과 같은 건 그대로 올려보낸다
        }
    }

    public WorkoutCondition findByWorkout(Long workoutId, Long userId) {
        workoutService.findById(workoutId, userId);
        return conditionRepository.findByWorkoutIdWithPains(workoutId)
                .orElseThrow(() -> new WorkoutConditionNotFoundException(workoutId));
    }
}
