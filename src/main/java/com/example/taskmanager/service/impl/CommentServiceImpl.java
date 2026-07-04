package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.request.CreateCommentRequest;
import com.example.taskmanager.dto.response.CommentResponse;
import com.example.taskmanager.entity.Comment;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.mapper.CommentMapper;
import com.example.taskmanager.repository.CommentRepository;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.service.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    public CommentServiceImpl(CommentRepository commentRepository, TaskRepository taskRepository,
                              UserRepository userRepository, CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.commentMapper = commentMapper;
    }

    @Override
    public CommentResponse addComment(Long taskId, CreateCommentRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> ResourceNotFoundException.task(taskId));

        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> ResourceNotFoundException.user(request.getAuthorId()));

        Comment comment = new Comment();
        comment.setBody(request.getBody());
        comment.setTask(task);
        comment.setAuthor(author);

        Comment savedComment = commentRepository.save(comment);
        return commentMapper.toResponse(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByTaskId(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw ResourceNotFoundException.task(taskId);
        }

        return commentRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());
    }
}
