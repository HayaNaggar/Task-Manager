package com.example.taskmanager.spec;

import com.example.taskmanager.entity.*;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TaskSpecs {

    public static Specification<Task> hasProjectId(Long projectId) {
        return (root, query, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(Priority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> hasAssigneeId(Long assigneeId) {
        return (root, query, cb) -> assigneeId == null ? null : cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Task> hasReporterId(Long reporterId) {
        return (root, query, cb) -> reporterId == null ? null : cb.equal(root.get("reporter").get("id"), reporterId);
    }

    public static Specification<Task> hasLabelId(Long labelId) {
        return (root, query, cb) -> {
            if (labelId == null) {
                return null;
            }
            Join<Task, Label> labelsJoin = root.join("labels");
            return cb.equal(labelsJoin.get("id"), labelId);
        };
    }

    public static Specification<Task> hasDueBefore(LocalDate dueBefore) {
        return (root, query, cb) -> dueBefore == null ? null : cb.lessThanOrEqualTo(root.get("dueDate"), dueBefore);
    }

    public static Specification<Task> hasDueAfter(LocalDate dueAfter) {
        return (root, query, cb) -> dueAfter == null ? null : cb.greaterThanOrEqualTo(root.get("dueDate"), dueAfter);
    }

    public static Specification<Task> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
