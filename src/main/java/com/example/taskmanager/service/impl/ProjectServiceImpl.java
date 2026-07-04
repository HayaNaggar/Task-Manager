package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.request.CreateProjectRequest;
import com.example.taskmanager.dto.request.UpdateProjectRequest;
import com.example.taskmanager.dto.response.ProjectResponse;
import com.example.taskmanager.dto.response.ProjectSummaryResponse;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.Project;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.exception.ConflictException;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.mapper.ProjectMapper;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.repository.ProjectRepository;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;

    public ProjectServiceImpl(ProjectRepository projectRepository, TaskRepository taskRepository,
                              ProjectMapper projectMapper, TaskMapper taskMapper) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.projectMapper = projectMapper;
        this.taskMapper = taskMapper;
    }

    @Override
    public ProjectResponse createProject(CreateProjectRequest request) {
        if (projectRepository.existsByKey(request.getKey())) {
            throw new ConflictException("Project key already exists: " + request.getKey());
        }

        Project project = projectMapper.toEntity(request);
        Project savedProject = projectRepository.save(project);
        return projectMapper.toResponse(savedProject);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getAllProjects(Pageable pageable) {
        return projectRepository.findAll(pageable)
                .map(projectMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.project(id));
        return projectMapper.toResponse(project);
    }

    @Override
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.project(id));

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        Project updatedProject = projectRepository.save(project);
        return projectMapper.toResponse(updatedProject);
    }

    @Override
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.project(id));

        List<Task> tasks = taskRepository.findByProjectId(id);

        // Check if there are any tasks that are not DONE
        boolean hasNonDoneTasks = tasks.stream()
                .anyMatch(task -> task.getStatus() != TaskStatus.DONE);

        if (hasNonDoneTasks) {
            throw new ConflictException("Cannot delete project with unfinished tasks");
        }

        // Delete project (cascade will handle tasks and comments)
        projectRepository.delete(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw ResourceNotFoundException.project(projectId);
        }

        return taskRepository.findByProjectId(projectId).stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectSummaryResponse getProjectSummary(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.project(projectId));

        List<Task> tasks = taskRepository.findByProjectId(projectId);

        long totalTasks = tasks.size();
        long todoCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        long inProgressCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long inReviewCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_REVIEW).count();
        long doneCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long overdueCount = tasks.stream().filter(t -> t.isOverdue()).count();

        return new ProjectSummaryResponse(
                project.getId(),
                project.getName(),
                project.getKey(),
                totalTasks,
                todoCount,
                inProgressCount,
                inReviewCount,
                doneCount
            , overdueCount
        );
    }
}
