package com.example.taskmanager.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

    @jakarta.validation.constraints.NotBlank(message = "Project name cannot be blank")
    private String name;

    private String description;
}
