package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.Priority;
import com.example.taskmanager.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TaskService {
    TaskResponse createTask(Long projectId, CreateTaskRequest request);
    Page<TaskResponse> searchTasks(
            Long projectId,
            TaskStatus status,
            Priority priority,
            Long assigneeId,
            Long reporterId,
            Long labelId,
            LocalDate dueBefore,
            LocalDate dueAfter,
            String keyword,
            Pageable pageable
    );
    TaskResponse getTaskById(Long id);
    TaskResponse updateTask(Long id, UpdateTaskRequest request);
    TaskResponse changeStatus(Long id, TaskStatus newStatus);
    TaskResponse assignTask(Long id, Long assigneeId);
    TaskResponse unassignTask(Long id);
    TaskResponse moveTask(Long id, Long targetProjectId);
    void deleteTask(Long id);
    TaskResponse attachLabel(Long taskId, Long labelId);
    TaskResponse detachLabel(Long taskId, Long labelId);
}
