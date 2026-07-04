package com.example.taskmanager.exception;

import com.example.taskmanager.entity.TaskStatus;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public static InvalidStateTransitionException invalidTransition(TaskStatus from, TaskStatus to) {
        return new InvalidStateTransitionException(
                String.format("Invalid status transition from %s to %s", from, to)
        );
    }
}
