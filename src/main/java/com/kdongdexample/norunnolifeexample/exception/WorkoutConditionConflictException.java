package com.kdongdexample.norunnolifeexample.exception;

public class WorkoutConditionConflictException extends RuntimeException {
    public WorkoutConditionConflictException(Long workoutId) {
        super("같은 운동 기록에 컨디션 저장 요청이 동시에 들어왔습니다. 다시 시도해 주세요. workoutId: " + workoutId);
    }
}
