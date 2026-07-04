package com.example.taskmanager.mapper;

import com.example.taskmanager.dto.response.CommentResponse;
import com.example.taskmanager.entity.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setBody(comment.getBody());
        response.setCreatedAt(comment.getCreatedAt());

        if (comment.getTask() != null) {
            response.setTaskId(comment.getTask().getId());
        }

        if (comment.getAuthor() != null) {
            response.setAuthorId(comment.getAuthor().getId());
            response.setAuthorName(comment.getAuthor().getFullName());
        }

        return response;
    }
}
