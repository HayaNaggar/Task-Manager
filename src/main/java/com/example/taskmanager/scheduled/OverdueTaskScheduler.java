package com.example.taskmanager.scheduled;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class OverdueTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueTaskScheduler.class);

    private final TaskRepository taskRepository;

    public OverdueTaskScheduler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // Run daily at 00:30
    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void markOverdueTasks() {
        LocalDate today = LocalDate.now();

        List<Task> toMark = taskRepository.findByDueDateBeforeAndStatusNot(today, TaskStatus.DONE);
        toMark.forEach(t -> {
            if (!t.isOverdue()) {
                t.setOverdue(true);
                taskRepository.save(t);
                log.info("Marked task {} as overdue", t.getId());
            }
        });

        // Clear overdue flag for tasks that no longer match
        List<Task> currentlyOverdue = taskRepository.findByOverdueTrue();
        currentlyOverdue.forEach(t -> {
            if (t.getDueDate() == null || !t.getDueDate().isBefore(today) || t.getStatus() == TaskStatus.DONE) {
                t.setOverdue(false);
                taskRepository.save(t);
                log.info("Cleared overdue flag for task {}", t.getId());
            }
        });
    }
}
