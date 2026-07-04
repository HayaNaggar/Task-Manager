package com.example.taskmanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private String body;
    private LocalDateTime createdAt;

    private Long taskId;

    private Long authorId;
    private String authorName;
}
