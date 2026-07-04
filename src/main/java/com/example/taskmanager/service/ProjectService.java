package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateProjectRequest;
import com.example.taskmanager.dto.request.UpdateProjectRequest;
import com.example.taskmanager.dto.response.ProjectResponse;
import com.example.taskmanager.dto.response.ProjectSummaryResponse;
import com.example.taskmanager.dto.response.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectService {
    ProjectResponse createProject(CreateProjectRequest request);
    Page<ProjectResponse> getAllProjects(Pageable pageable);
    ProjectResponse getProjectById(Long id);
    ProjectResponse updateProject(Long id, UpdateProjectRequest request);
    void deleteProject(Long id);
    List<TaskResponse> getTasksByProjectId(Long projectId);
    ProjectSummaryResponse getProjectSummary(Long projectId);
}
