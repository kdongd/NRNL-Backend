package com.kdongd.nrnl.exception;

import com.kdongd.nrnl.domain.WorkoutType;

public class WorkoutTypeMismatchException extends RuntimeException {
    public WorkoutTypeMismatchException(WorkoutType existingType, WorkoutType requestedType) {
        super("운동 타입은 변경할 수 없습니다. 기존 타입: " + existingType + ", 요청 타입: " + requestedType);
    }
}
