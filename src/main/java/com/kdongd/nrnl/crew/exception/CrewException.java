package com.kdongd.nrnl.crew.exception;

import org.springframework.http.HttpStatus;

public class CrewException extends RuntimeException {

    private final HttpStatus status;

    public CrewException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static CrewException conflict(String message) {
        return new CrewException(HttpStatus.CONFLICT, message);
    }

    public static CrewException forbidden(String message) {
        return new CrewException(HttpStatus.FORBIDDEN, message);
    }

    public static CrewException notFound(String message) {
        return new CrewException(HttpStatus.NOT_FOUND, message);
    }

    public static CrewException badRequest(String message) {
        return new CrewException(HttpStatus.BAD_REQUEST, message);
    }
}
