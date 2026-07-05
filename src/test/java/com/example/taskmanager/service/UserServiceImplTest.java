package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateUserRequest;
import com.example.taskmanager.entity.Role;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.exception.ConflictException;
import com.example.taskmanager.mapper.UserMapper;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression coverage for the bug where POST /api/users inserted a user with no
 * password set at all (UserMapper.toEntity never copied it), causing a NOT NULL
 * constraint violation at the database. createUser must now hash and persist it.
 */
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskRepository taskRepository;

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private UserServiceImpl userService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.userMapper = new UserMapper();
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.userService = new UserServiceImpl(userRepository, taskRepository, userMapper,
                new com.example.taskmanager.mapper.TaskMapper(new com.example.taskmanager.mapper.LabelMapper()),
                passwordEncoder);
    }

    @Test
    public void createUser_setsHashedPassword_notNullAndNotPlainText() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("Test User");
        request.setEmail("test.user@example.com");
        request.setPassword("plainPassword123");

        Mockito.when(userRepository.existsByEmail("test.user@example.com")).thenReturn(false);
        Mockito.when(userRepository.save(ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> {
                    User saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        userService.createUser(request);

        Mockito.verify(userRepository).save(ArgumentMatchers.argThat(user ->
                user.getPassword() != null
                        && !user.getPassword().isBlank()
                        && !user.getPassword().equals("plainPassword123")
                        && passwordEncoder.matches("plainPassword123", user.getPassword())
        ));
    }

    @Test
    public void createUser_defaultsToMemberRole() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("Member User");
        request.setEmail("member.user@example.com");
        request.setPassword("password1234");

        Mockito.when(userRepository.existsByEmail("member.user@example.com")).thenReturn(false);
        Mockito.when(userRepository.save(ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(request);

        Mockito.verify(userRepository).save(ArgumentMatchers.argThat(user -> user.getRole() == Role.MEMBER));
    }

    @Test
    public void createUser_duplicateEmail_throwsConflictAndNeverSaves() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("Duplicate");
        request.setEmail("existing@example.com");
        request.setPassword("password1234");

        Mockito.when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createUser(request));
        Mockito.verify(userRepository, Mockito.never()).save(ArgumentMatchers.any());
    }
}
