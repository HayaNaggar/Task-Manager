package com.example.taskmanager.mapper;

import com.example.taskmanager.dto.request.CreateProjectRequest;
import com.example.taskmanager.dto.response.ProjectResponse;
import com.example.taskmanager.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public Project toEntity(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setKey(request.getKey());
        project.setDescription(request.getDescription());
        return project;
    }

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getKey(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
