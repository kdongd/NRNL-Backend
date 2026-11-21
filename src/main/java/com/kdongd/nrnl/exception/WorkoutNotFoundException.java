package com.kdongd.nrnl.exception;

public class WorkoutNotFoundException extends RuntimeException {
    public WorkoutNotFoundException(Long id) {
        super("운동 기록을 찾을 수 없습니다. id: " + id);
    }
}
