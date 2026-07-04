package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateUserRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    List<TaskResponse> getTasksAssignedToUser(Long userId);
}
