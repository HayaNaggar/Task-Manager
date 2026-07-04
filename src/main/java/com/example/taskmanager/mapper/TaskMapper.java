package com.example.taskmanager.mapper;

import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.Task;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TaskMapper {

    private final LabelMapper labelMapper;

    public TaskMapper(LabelMapper labelMapper) {
        this.labelMapper = labelMapper;
    }

    public TaskResponse toResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setDueDate(task.getDueDate());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());

        // Project info
        if (task.getProject() != null) {
            response.setProjectId(task.getProject().getId());
            response.setProjectKey(task.getProject().getKey());
            response.setProjectName(task.getProject().getName());
        }

        // Reporter info
        if (task.getReporter() != null) {
            response.setReporterId(task.getReporter().getId());
            response.setReporterName(task.getReporter().getFullName());
        }

        // Assignee info
        if (task.getAssignee() != null) {
            response.setAssigneeId(task.getAssignee().getId());
            response.setAssigneeName(task.getAssignee().getFullName());
        }

        // Labels
        if (task.getLabels() != null) {
            response.setLabels(
                    task.getLabels().stream()
                            .map(labelMapper::toResponse)
                            .collect(Collectors.toList())
            );
        }

        // Comment count
        if (task.getComments() != null) {
            response.setCommentCount(task.getComments().size());
        }

        return response;
    }
}
