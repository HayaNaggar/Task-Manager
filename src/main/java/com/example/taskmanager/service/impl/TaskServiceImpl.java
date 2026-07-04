package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.*;
import com.example.taskmanager.exception.ConflictException;
import com.example.taskmanager.exception.InvalidStateTransitionException;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.repository.*;
import com.example.taskmanager.service.TaskService;
import com.example.taskmanager.spec.TaskSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;
    private final TaskMapper taskMapper;

    // Status workflow state machine
    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            TaskStatus.TODO, Set.of(TaskStatus.IN_PROGRESS),
            TaskStatus.IN_PROGRESS, Set.of(TaskStatus.IN_REVIEW, TaskStatus.TODO),
            TaskStatus.IN_REVIEW, Set.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS),
            TaskStatus.DONE, Set.of(TaskStatus.TODO)
    );

    public TaskServiceImpl(TaskRepository taskRepository, ProjectRepository projectRepository,
                           UserRepository userRepository, LabelRepository labelRepository,
                           TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.labelRepository = labelRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public TaskResponse createTask(Long projectId, CreateTaskRequest request) {
        // Validate project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.project(projectId));

        // Validate reporter exists
        User reporter = userRepository.findById(request.getReporterId())
                .orElseThrow(() -> ResourceNotFoundException.user(request.getReporterId()));

        // Validate assignee exists (if provided)
        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> ResourceNotFoundException.user(request.getAssigneeId()));
        }

        // Create task
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setStatus(TaskStatus.TODO); // Always start as TODO
        task.setProject(project);
        task.setReporter(reporter);
        task.setAssignee(assignee);

        Task savedTask = taskRepository.save(task);
        return taskMapper.toResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> searchTasks(Long projectId, TaskStatus status, Priority priority,
                                          Long assigneeId, Long reporterId, Long labelId,
                                          LocalDate dueBefore, LocalDate dueAfter, String keyword,
                                          Pageable pageable) {
        Specification<Task> spec = Specification.where(TaskSpecs.hasProjectId(projectId))
                .and(TaskSpecs.hasStatus(status))
                .and(TaskSpecs.hasPriority(priority))
                .and(TaskSpecs.hasAssigneeId(assigneeId))
                .and(TaskSpecs.hasReporterId(reporterId))
                .and(TaskSpecs.hasLabelId(labelId))
                .and(TaskSpecs.hasDueBefore(dueBefore))
                .and(TaskSpecs.hasDueAfter(dueAfter))
                .and(TaskSpecs.hasKeyword(keyword));

        return taskRepository.findAll(spec, pageable)
                .map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.task(id));
        return taskMapper.toResponse(task);
    }

    @Override
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.task(id));

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    @Override
    public TaskResponse changeStatus(Long id, TaskStatus newStatus) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.task(id));

        TaskStatus currentStatus = task.getStatus();

        // Check if transition is allowed
        if (!isTransitionAllowed(currentStatus, newStatus)) {
            throw InvalidStateTransitionException.invalidTransition(currentStatus, newStatus);
        }

        task.setStatus(newStatus);
        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    @Override
    public TaskResponse assignTask(Long id, Long assigneeId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.task(id));

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> ResourceNotFoundException.user(assigneeId));

        task.setAssignee(assignee);
        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    @Override
    public TaskResponse unassignTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.task(id));

        task.setAssignee(null);
        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    @Override
    public TaskResponse moveTask(Long id, Long targetProjectId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.task(id));

        Project targetProject = projectRepository.findById(targetProjectId)
                .orElseThrow(() -> ResourceNotFoundException.project(targetProjectId));

        task.setProject(targetProject);
        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    @Override
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.task(id));
        taskRepository.delete(task);
    }

    @Override
    public TaskResponse attachLabel(Long taskId, Long labelId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> ResourceNotFoundException.task(taskId));

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> ResourceNotFoundException.label(labelId));

        // Check if label is already attached
        if (task.getLabels().contains(label)) {
            throw new ConflictException("Label already attached to this task");
        }

        task.getLabels().add(label);
        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    @Override
    public TaskResponse detachLabel(Long taskId, Long labelId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> ResourceNotFoundException.task(taskId));

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> ResourceNotFoundException.label(labelId));

        task.getLabels().remove(label);
        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    // Helper method to validate status transitions
    private boolean isTransitionAllowed(TaskStatus from, TaskStatus to) {
        if (from == to) {
            return true; // Same status is always allowed
        }

        Set<TaskStatus> allowedTargets = ALLOWED_TRANSITIONS.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }
}
