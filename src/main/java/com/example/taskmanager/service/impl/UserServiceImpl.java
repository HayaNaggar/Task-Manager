package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.request.CreateUserRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.dto.response.UserResponse;
import com.example.taskmanager.entity.Role;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.exception.ConflictException;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.mapper.UserMapper;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, TaskRepository taskRepository,
                           UserMapper userMapper, TaskMapper taskMapper,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.userMapper = userMapper;
        this.taskMapper = taskMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        // UserMapper only copies fullName/email; password must be hashed here, never stored raw.
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.MEMBER);

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.user(id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksAssignedToUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.user(userId);
        }

        return taskRepository.findByAssigneeId(userId).stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }
}
