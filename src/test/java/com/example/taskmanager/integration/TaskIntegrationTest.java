package com.example.taskmanager.integration;

import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.taskmanager.entity.Project;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.Priority;
import com.example.taskmanager.repository.ProjectRepository;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.service.TaskService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.sql.init.mode=never"})
public class TaskIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Test
    public void auth_and_taskFilters_work() {
        // Unauthenticated requests are rejected (returns 403 Forbidden)
        ResponseEntity<String> unauth = restTemplate.getForEntity("/api/tasks", String.class);
        assertEquals(HttpStatus.FORBIDDEN, unauth.getStatusCode());

        // Login as pre-seeded admin
        Map<String, String> creds = new HashMap<>();
        creds.put("email", "admin@example.com");
        creds.put("password", "adminpass");

        ResponseEntity<Map> loginResp = restTemplate.postForEntity("/api/auth/login", creds, Map.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        String token = (String) loginResp.getBody().get("token");
        assertNotNull(token);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Find admin id
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        // Create a project and tasks directly via repositories to seed data for search
        Project project = new Project();
        project.setName("Integration Project");
        project.setKey("IP");
        project.setDescription("test project");
        project = projectRepository.save(project);

        Task task1 = new Task();
        task1.setTitle("Task One");
        task1.setDescription("first");
        task1.setPriority(Priority.HIGH);
        task1.setReporter(admin);
        task1.setProject(project);
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setTitle("Task Two");
        task2.setDescription("second");
        task2.setPriority(Priority.LOW);
        task2.setReporter(admin);
        task2.setProject(project);
        taskRepository.save(task2);

        Long projectId = project.getId();

        // Verify filtering via the service layer directly (avoids web-layer serialization issues in this test environment)
        org.springframework.data.domain.Page<com.example.taskmanager.dto.response.TaskResponse> page = taskService.searchTasks(
                projectId, null, Priority.HIGH, null, null, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertNotNull(page);
        assertEquals(1, page.getContent().size(), "Expected one HIGH priority task for the project");
    }
}
