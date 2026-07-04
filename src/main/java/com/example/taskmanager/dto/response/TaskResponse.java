package com.example.taskmanager.dto.response;

import com.example.taskmanager.entity.Priority;
import com.example.taskmanager.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Related entities (simplified)
    private Long projectId;
    private String projectKey;
    private String projectName;

    private Long reporterId;
    private String reporterName;

    private Long assigneeId;
    private String assigneeName;

    private List<LabelResponse> labels;
    private Integer commentCount;
}
