package com.example.taskmanager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveTaskRequest {

    @NotNull(message = "Target project ID is required")
    private Long projectId;
}
