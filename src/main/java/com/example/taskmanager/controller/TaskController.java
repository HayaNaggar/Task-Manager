package com.example.taskmanager.controller;

import com.example.taskmanager.dto.request.AssignTaskRequest;
import com.example.taskmanager.dto.request.ChangeStatusRequest;
import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.MoveTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.Priority;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/tasks")
    public ResponseEntity<Page<TaskResponse>> searchTasks(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueAfter,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        Page<TaskResponse> tasks = taskService.searchTasks(
                projectId, status, priority, assigneeId, reporterId,
                labelId, dueBefore, dueAfter, keyword, pageable
        );
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        TaskResponse task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @RequestBody UpdateTaskRequest request) {
        TaskResponse response = taskService.updateTask(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/tasks/{id}/status")
    public ResponseEntity<TaskResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {
        TaskResponse response = taskService.changeStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/tasks/{id}/assignee")
    public ResponseEntity<TaskResponse> assignOrUnassignTask(
            @PathVariable Long id,
            @RequestBody AssignTaskRequest request) {

        TaskResponse response;
        if (request.getAssigneeId() == null) {
            response = taskService.unassignTask(id);
        } else {
            response = taskService.assignTask(id, request.getAssigneeId());
        }
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/tasks/{id}/project")
    public ResponseEntity<TaskResponse> moveTask(
            @PathVariable Long id,
            @Valid @RequestBody MoveTaskRequest request) {
        TaskResponse response = taskService.moveTask(id, request.getProjectId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tasks/{taskId}/labels/{labelId}")
    public ResponseEntity<TaskResponse> attachLabel(
            @PathVariable Long taskId,
            @PathVariable Long labelId) {
        TaskResponse response = taskService.attachLabel(taskId, labelId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/tasks/{taskId}/labels/{labelId}")
    public ResponseEntity<TaskResponse> detachLabel(
            @PathVariable Long taskId,
            @PathVariable Long labelId) {
        TaskResponse response = taskService.detachLabel(taskId, labelId);
        return ResponseEntity.ok(response);
    }
}
