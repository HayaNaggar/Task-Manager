package com.example.taskmanager.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException user(Long id) {
        return new ResourceNotFoundException("User not found with id: " + id);
    }

    public static ResourceNotFoundException project(Long id) {
        return new ResourceNotFoundException("Project not found with id: " + id);
    }

    public static ResourceNotFoundException task(Long id) {
        return new ResourceNotFoundException("Task not found with id: " + id);
    }

    public static ResourceNotFoundException label(Long id) {
        return new ResourceNotFoundException("Label not found with id: " + id);
    }

    public static ResourceNotFoundException comment(Long id) {
        return new ResourceNotFoundException("Comment not found with id: " + id);
    }

    public static ResourceNotFoundException userByEmail(String email) {
        return new ResourceNotFoundException("User not found with email: " + email);
    }
}
