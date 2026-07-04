package com.example.taskmanager.service;

import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.exception.InvalidStateTransitionException;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.repository.LabelRepository;
import com.example.taskmanager.repository.ProjectRepository;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TaskServiceImplWorkflowTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LabelRepository labelRepository;
    private TaskMapper taskMapper;

    private TaskServiceImpl taskService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        // use real mapper to avoid inline-mock instrumentation issues on newer JDKs
        this.taskMapper = new com.example.taskmanager.mapper.TaskMapper(new com.example.taskmanager.mapper.LabelMapper());
        taskService = new TaskServiceImpl(taskRepository, projectRepository, userRepository, labelRepository, taskMapper);
    }

    @Test
    public void changeStatus_allowedTransition_succeeds() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus(TaskStatus.TODO);
        // set security context as the reporter
        org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("reporter@example.com", null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        com.example.taskmanager.entity.User reporter = new com.example.taskmanager.entity.User();
        reporter.setId(1L);
        reporter.setEmail("reporter@example.com");
        task.setReporter(reporter);
        org.mockito.Mockito.when(userRepository.findByEmail("reporter@example.com")).thenReturn(java.util.Optional.of(reporter));

        Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        Mockito.when(taskRepository.save(ArgumentMatchers.any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskResponse resp = taskService.changeStatus(1L, TaskStatus.IN_PROGRESS);
        assertNotNull(resp);
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
    }

    @Test
    public void changeStatus_illegalTransition_throws() {
        Task task = new Task();
        task.setId(2L);
        task.setStatus(TaskStatus.TODO);

        // set security context as reporter for task id 2
        org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("reporter2@example.com", null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        com.example.taskmanager.entity.User reporter2 = new com.example.taskmanager.entity.User();
        reporter2.setId(2L);
        reporter2.setEmail("reporter2@example.com");
        task.setReporter(reporter2);
        org.mockito.Mockito.when(userRepository.findByEmail("reporter2@example.com")).thenReturn(java.util.Optional.of(reporter2));

        Mockito.when(taskRepository.findById(2L)).thenReturn(Optional.of(task));

        assertThrows(InvalidStateTransitionException.class, () -> {
            taskService.changeStatus(2L, TaskStatus.DONE);
        });
    }
}
