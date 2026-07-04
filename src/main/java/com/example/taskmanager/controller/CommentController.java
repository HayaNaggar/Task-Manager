package com.example.taskmanager.controller;

import com.example.taskmanager.dto.request.CreateCommentRequest;
import com.example.taskmanager.dto.response.CommentResponse;
import com.example.taskmanager.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/{taskId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.addComment(taskId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{taskId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsByTaskId(@PathVariable Long taskId) {
        List<CommentResponse> comments = commentService.getCommentsByTaskId(taskId);
        return ResponseEntity.ok(comments);
    }
}
