package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateCommentRequest;
import com.example.taskmanager.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {
    CommentResponse addComment(Long taskId, CreateCommentRequest request);
    List<CommentResponse> getCommentsByTaskId(Long taskId);
}
