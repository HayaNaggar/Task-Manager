package com.example.taskmanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummaryResponse {

    private Long projectId;
    private String projectName;
    private String projectKey;

    private long totalTasks;
    private long todoCount;
    private long inProgressCount;
    private long inReviewCount;
    private long doneCount;
}
