package com.skillteam.taskprogress.exception;

public class InvalidTaskStatusTransitionException extends RuntimeException {

    public InvalidTaskStatusTransitionException(String message) {
        super(message);
    }
}
