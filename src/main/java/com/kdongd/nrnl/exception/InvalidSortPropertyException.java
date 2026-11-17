package com.kdongd.nrnl.exception;

import java.util.Set;

public class InvalidSortPropertyException extends RuntimeException {
    public InvalidSortPropertyException(String property, Set<String> allowed) {
        super("정렬 필드가 올바르지 않습니다: " + property + " (허용값: " + allowed + ")");
    }
}
