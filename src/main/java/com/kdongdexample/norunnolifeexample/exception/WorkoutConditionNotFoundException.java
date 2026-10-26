package com.kdongdexample.norunnolifeexample.exception;

public class WorkoutConditionNotFoundException extends RuntimeException {
    public WorkoutConditionNotFoundException(Long workoutId) {
        super("운동 후 컨디션 기록이 없습니다. workoutId: " + workoutId);
    }
}
