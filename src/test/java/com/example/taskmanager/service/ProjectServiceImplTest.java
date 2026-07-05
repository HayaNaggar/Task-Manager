package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateProjectRequest;
import com.example.taskmanager.entity.Project;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.exception.ConflictException;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.mapper.ProjectMapper;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.mapper.LabelMapper;
import com.example.taskmanager.repository.ProjectRepository;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers §4 rule #6 from the spec: deleting a project is blocked (409) while it still
 * has tasks that are not DONE, and allowed once every task is DONE (or there are none).
 */
public class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TaskRepository taskRepository;

    private ProjectMapper projectMapper;
    private TaskMapper taskMapper;
    private ProjectServiceImpl projectService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.projectMapper = new ProjectMapper();
        this.taskMapper = new TaskMapper(new LabelMapper());
        this.projectService = new ProjectServiceImpl(projectRepository, taskRepository, projectMapper, taskMapper);
    }

    @Test
    public void deleteProject_withOpenTasks_throwsConflict() {
        Project project = new Project();
        project.setId(10L);
        project.setName("Demo");
        project.setKey("DEMO");

        Task openTask = new Task();
        openTask.setId(1L);
        openTask.setStatus(TaskStatus.IN_PROGRESS);

        Mockito.when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        Mockito.when(taskRepository.findByProjectId(10L)).thenReturn(List.of(openTask));

        assertThrows(ConflictException.class, () -> projectService.deleteProject(10L));
        Mockito.verify(projectRepository, Mockito.never()).delete(ArgumentMatchers.any());
    }

    @Test
    public void deleteProject_withOnlyDoneTasks_succeeds() {
        Project project = new Project();
        project.setId(11L);
        project.setName("Demo");
        project.setKey("DEMO2");

        Task doneTask = new Task();
        doneTask.setId(2L);
        doneTask.setStatus(TaskStatus.DONE);

        Mockito.when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
        Mockito.when(taskRepository.findByProjectId(11L)).thenReturn(List.of(doneTask));

        assertDoesNotThrow(() -> projectService.deleteProject(11L));
        Mockito.verify(projectRepository, Mockito.times(1)).delete(project);
    }

    @Test
    public void deleteProject_withNoTasks_succeeds() {
        Project project = new Project();
        project.setId(12L);
        project.setName("Empty");
        project.setKey("EMPTY");

        Mockito.when(projectRepository.findById(12L)).thenReturn(Optional.of(project));
        Mockito.when(taskRepository.findByProjectId(12L)).thenReturn(List.of());

        assertDoesNotThrow(() -> projectService.deleteProject(12L));
        Mockito.verify(projectRepository, Mockito.times(1)).delete(project);
    }

    @Test
    public void deleteProject_missingProject_throwsNotFound() {
        Mockito.when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.deleteProject(999L));
    }

    @Test
    public void createProject_duplicateKey_throwsConflict() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Duplicate");
        request.setKey("DUP");
        request.setDescription("desc");

        Mockito.when(projectRepository.existsByKey("DUP")).thenReturn(true);

        assertThrows(ConflictException.class, () -> projectService.createProject(request));
        Mockito.verify(projectRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    public void createProject_newKey_succeeds() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Fresh");
        request.setKey("FRESH");
        request.setDescription("desc");

        Mockito.when(projectRepository.existsByKey("FRESH")).thenReturn(false);
        Mockito.when(projectRepository.save(ArgumentMatchers.any(Project.class)))
                .thenAnswer(invocation -> {
                    Project p = invocation.getArgument(0);
                    p.setId(1L);
                    return p;
                });

        var response = projectService.createProject(request);
        assertNotNull(response);
        assertEquals("FRESH", response.getKey());
    }
}
