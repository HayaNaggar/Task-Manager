package com.example.taskmanager.dto.request;

import com.example.taskmanager.entity.Priority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {

    private String title;
    private String description;
    private Priority priority;
    private LocalDate dueDate;
}
