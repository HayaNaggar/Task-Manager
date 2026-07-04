# Task Flow

A Spring Boot REST API for task management (similar to Jira/Trello), demonstrating intermediate Spring Boot concepts including entity relationships, transactional business logic, workflow state machines, and dynamic search with pagination.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Setup and Installation](#setup-and-installation)
- [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
- [Business Rules](#business-rules)
- [Status Workflow](#status-workflow)
- [Search and Filtering](#search-and-filtering)
- [Design Decisions](#design-decisions)
- [Testing](#testing)

---

## Overview

This project is a task management system that goes beyond simple CRUD operations. It demonstrates:

- **Complex JPA relationships** (@OneToMany, @ManyToOne, @ManyToMany)
- **Transactional business logic** with proper rollback handling
- **State machine implementation** for task status workflow
- **Dynamic search** using Spring Data JPA Specifications
- **Clean architecture** with DTOs, mappers, and layered design
- **Comprehensive exception handling** with meaningful HTTP status codes

---

## Tech Stack

- **Java 21**
- **Spring Boot 3.2.0**
- **Maven**
- **Spring Web**
- **Spring Data JPA**
- **Spring Validation**
- **H2 Database** (in-memory)
- **Lombok**

---

## Project Structure

```
com.example.taskmanager
├── controller/
│   ├── UserController.java
│   ├── ProjectController.java
│   ├── TaskController.java
│   ├── CommentController.java
│   └── LabelController.java
│
├── service/
│   ├── UserService.java
│   ├── ProjectService.java
│   ├── TaskService.java
│   ├── CommentService.java
│   ├── LabelService.java
│   └── impl/
│       ├── UserServiceImpl.java
│       ├── ProjectServiceImpl.java
│       ├── TaskServiceImpl.java
│       ├── CommentServiceImpl.java
│       └── LabelServiceImpl.java
│
├── repository/
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   ├── TaskRepository.java
│   ├── CommentRepository.java
│   └── LabelRepository.java
│
├── entity/
│   ├── User.java
│   ├── Project.java
│   ├── Task.java
│   ├── Comment.java
│   ├── Label.java
│   ├── TaskStatus.java (enum)
│   └── Priority.java (enum)
│
├── dto/
│   ├── request/
│   │   ├── CreateUserRequest.java
│   │   ├── CreateProjectRequest.java
│   │   ├── UpdateProjectRequest.java
│   │   ├── CreateTaskRequest.java
│   │   ├── UpdateTaskRequest.java
│   │   ├── ChangeStatusRequest.java
│   │   ├── AssignTaskRequest.java
│   │   ├── MoveTaskRequest.java
│   │   ├── CreateCommentRequest.java
│   │   └── CreateLabelRequest.java
│   └── response/
│       ├── UserResponse.java
│       ├── ProjectResponse.java
│       ├── TaskResponse.java
│       ├── CommentResponse.java
│       ├── LabelResponse.java
│       └── ProjectSummaryResponse.java
│
├── mapper/
│   ├── UserMapper.java
│   ├── ProjectMapper.java
│   ├── TaskMapper.java
│   ├── CommentMapper.java
│   └── LabelMapper.java
│
├── spec/
│   └── TaskSpecs.java
│
└── exception/
    ├── ResourceNotFoundException.java
    ├── InvalidStateTransitionException.java
    ├── ConflictException.java
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java
```

---

## Setup and Installation

### Prerequisites

- **Java 21** or higher
- **Maven 3.6+**

### Installation Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd task-management-api
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

---

## Running the Application

### Using Maven

```bash
mvn spring-boot:run
```

### Using Java

```bash
java -jar target/taskmanager-0.0.1-SNAPSHOT.jar
```

The application will start on **http://localhost:8081**

### H2 Console

Access the H2 database console at: **http://localhost:8081/h2-console**

- **JDBC URL**: `jdbc:h2:mem:taskdb`
- **Username**: `sa`
- **Password**: (leave empty)

---

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT token |

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create a new user (requires JWT auth) |
| GET | `/api/users` | Get all users |
| GET | `/api/users/{id}` | Get user by ID |
| GET | `/api/users/{id}/tasks` | Get tasks assigned to user |

### Projects

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects` | Create a new project |
| GET | `/api/projects` | Get all projects (paginated) |
| GET | `/api/projects/{id}` | Get project by ID |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project (with validation) |
| GET | `/api/projects/{id}/tasks` | Get tasks in project |
| GET | `/api/projects/{id}/summary` | Get project summary (task counts by status) |

### Tasks

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects/{projectId}/tasks` | Create task in project |
| GET | `/api/tasks` | Search tasks with filters (paginated) |
| GET | `/api/tasks/{id}` | Get task by ID |
| PUT | `/api/tasks/{id}` | Update task |
| PATCH | `/api/tasks/{id}/status` | Change task status (validated) |
| PATCH | `/api/tasks/{id}/assignee` | Assign/unassign task |
| PATCH | `/api/tasks/{id}/project` | Move task to another project |
| DELETE | `/api/tasks/{id}` | Delete task |
| POST | `/api/tasks/{taskId}/labels/{labelId}` | Attach label to task |
| DELETE | `/api/tasks/{taskId}/labels/{labelId}` | Detach label from task |

### Comments

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tasks/{taskId}/comments` | Add comment to task |
| GET | `/api/tasks/{taskId}/comments` | Get comments for task |

### Labels

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/labels` | Create a new label |
| GET | `/api/labels` | Get all labels |

---

## Request/Response Examples

### Create User

**Request:**
```bash
POST /api/users
Content-Type: application/json
Authorization: Bearer <token>

{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "password": "TestPassword123"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "createdAt": "2026-07-04T10:30:00"
}
```

### Create Project

**Request:**
```bash
POST /api/projects
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "Web Application",
  "key": "WEB",
  "description": "Main web application project"
}
```

**Response:** `201 Created`

### Create Task

**Request:**
```bash
POST /api/projects/1/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Setup authentication system",
  "description": "Implement JWT authentication",
  "priority": "HIGH",
  "reporterId": 1,
  "assigneeId": 2,
  "dueDate": "2026-08-01"
}
```

**Response:** `201 Created`

### Change Task Status

**Request:**
```bash
PATCH /api/tasks/1/status
Content-Type: application/json
Authorization: Bearer <token>

{
  "status": "IN_PROGRESS"
}
```

**Response:** `200 OK` (if transition is valid)

**Error Response:** `409 Conflict` (if transition is invalid)
```json
{
  "timestamp": "2026-07-04T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Invalid status transition from TODO to DONE",
  "path": "/api/tasks/1/status"
}
```

### Search Tasks

**Request:**
```bash
GET /api/tasks?status=IN_PROGRESS&priority=HIGH&page=0&size=20&sort=dueDate,asc
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "content": [...],
  "totalElements": 15,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

## Business Rules

### 1. Task Creation
- Project must exist
- Reporter must exist
- Assignee (if provided) must exist
- New tasks always start as `TODO`
- Returns `404` for missing references

### 2. Status Changes
- Must follow the workflow state machine
- Returns `409` for illegal transitions

### 3. Assign/Unassign
- User must exist when assigning
- Setting `assigneeId` to `null` unassigns

### 4. Labels
- Task and label must exist
- No duplicate labels on same task
- Returns `409` if label already attached

### 5. Move Task
- Target project must exist
- Operation is transactional

### 6. Delete Project
- Only allowed if no tasks OR all tasks are `DONE`
- Cascades to delete tasks and comments
- Labels and users remain
- Returns `409` if project has unfinished tasks

### 7. Add Comment
- Task must exist
- Author must exist

---

## Status Workflow

The task status follows a **state machine** pattern with these allowed transitions:

```
TODO
  ↓
IN_PROGRESS
  ↓         ↖
IN_REVIEW    ← (can go back)
  ↓
DONE
  ↓
TODO (reopen)
```

### Allowed Transitions

| From | To (Allowed) |
|------|--------------|
| `TODO` | `IN_PROGRESS` |
| `IN_PROGRESS` | `IN_REVIEW`, `TODO` |
| `IN_REVIEW` | `DONE`, `IN_PROGRESS` |
| `DONE` | `TODO` (reopen) |

### Forbidden Examples

- `TODO` → `DONE`
- `TODO` → `IN_REVIEW`
- `DONE` → `IN_PROGRESS`

**Implementation:** The workflow is enforced in `TaskServiceImpl` using a `Map<TaskStatus, Set<TaskStatus>>` that defines allowed transitions.

---

## Search and Filtering

### Endpoint
```
GET /api/tasks
```

### Supported Filters

| Parameter | Type | Description |
|-----------|------|-------------|
| `projectId` | Long | Filter by project |
| `status` | TaskStatus | Filter by status (TODO, IN_PROGRESS, IN_REVIEW, DONE) |
| `priority` | Priority | Filter by priority (LOW, MEDIUM, HIGH, URGENT) |
| `assigneeId` | Long | Filter by assignee |
| `reporterId` | Long | Filter by reporter |
| `labelId` | Long | Filter by label |
| `dueBefore` | Date | Tasks due before this date |
| `dueAfter` | Date | Tasks due after this date |
| `keyword` | String | Search in title OR description (case-insensitive) |
| `page` | Integer | Page number (default: 0) |
| `size` | Integer | Page size (default: 20) |
| `sort` | String | Sorting (e.g., `dueDate,asc` or `priority,desc`) |

### Examples

**Find high-priority in-progress tasks:**
```
GET /api/tasks?status=IN_PROGRESS&priority=HIGH
```

**Find tasks with keyword "authentication":**
```
GET /api/tasks?keyword=authentication
```

**Find tasks due before a date, sorted by due date:**
```
GET /api/tasks?dueBefore=2026-08-01&sort=dueDate,asc
```

**Combine multiple filters:**
```
GET /api/tasks?projectId=1&status=IN_PROGRESS&assigneeId=2&page=0&size=10
```

---

## Design Decisions

### 1. Cascade Choices

**Project → Task → Comment:**
- Uses `CascadeType.ALL` and `orphanRemoval = true`
- When a project is deleted, all its tasks are deleted
- When a task is deleted, all its comments are deleted
- **Rationale:** Comments have no meaning without their parent task

**Task → Label (Many-to-Many):**
- No cascade delete
- Labels are reusable across tasks
- Deleting a task removes the relationship, but the label persists
- **Rationale:** Labels are shared resources and should not be deleted with tasks

**Task → User (Reporter/Assignee):**
- Uses `@ManyToOne` without cascade
- Deleting a task does NOT delete users
- **Rationale:** Users exist independently and may have other tasks

### 2. Why Workflow Lives in Service Layer

The status workflow state machine is implemented in `TaskServiceImpl`, **not** in the controller or entity.

**Reasons:**
1. **Business Logic Separation:** Controllers should only handle HTTP concerns
2. **Testability:** Service logic can be unit tested without HTTP context
3. **Reusability:** The workflow can be used from multiple entry points
4. **Transaction Management:** `@Transactional` methods ensure atomicity
5. **Single Responsibility:** The service owns the business rules

**Implementation:**
```java
private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
    TaskStatus.TODO, Set.of(TaskStatus.IN_PROGRESS),
    TaskStatus.IN_PROGRESS, Set.of(TaskStatus.IN_REVIEW, TaskStatus.TODO),
    TaskStatus.IN_REVIEW, Set.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS),
    TaskStatus.DONE, Set.of(TaskStatus.TODO)
);

private boolean isTransitionAllowed(TaskStatus from, TaskStatus to) {
    if (from == to) return true;
    Set<TaskStatus> allowedTargets = ALLOWED_TRANSITIONS.get(from);
    return allowedTargets != null && allowedTargets.contains(to);
}
```

### 3. Dynamic Search Implementation

**Technology:** Spring Data JPA Specifications (Criteria API)

**Rationale:**
- Composes only the filters that are provided (no `null` checks in SQL)
- Type-safe (compile-time checking)
- Reusable predicates
- Works seamlessly with `Pageable`
- Avoids messy `@Query` with many `OR param IS NULL` clauses

**Implementation Pattern:**
```java
// TaskSpecs.java
public static Specification<Task> hasStatus(TaskStatus status) {
    return (root, query, cb) ->
        status == null ? null : cb.equal(root.get("status"), status);
}

// TaskServiceImpl.java
Specification<Task> spec = Specification.where(TaskSpecs.hasProjectId(projectId))
    .and(TaskSpecs.hasStatus(status))
    .and(TaskSpecs.hasPriority(priority))
    // ... more filters

return taskRepository.findAll(spec, pageable).map(taskMapper::toResponse);
```

**Key Features:**
- Each `Specification` returns `null` if the parameter is not provided
- `Specification.where()` intelligently ignores `null` specifications
- Combines multiple filters with `.and()`
- Returns a `Page<TaskResponse>` with pagination metadata

---

## HTTP Status Codes

| Operation | Status Code |
|-----------|-------------|
| Create | `201 Created` |
| Read (success) | `200 OK` |
| Update | `200 OK` |
| Delete | `204 No Content` |
| Validation Error | `400 Bad Request` |
| Resource Not Found | `404 Not Found` |
| Illegal State / Blocked Delete | `409 Conflict` |

---

## Testing

### Manual Testing with cURL

**Login as admin:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"adminpass"}'
```

The application includes two pre-seeded users:
- `admin@example.com` / `adminpass`
- `member@example.com` / `memberpass`

Use the returned JWT token in the `Authorization` header for any protected endpoint.

**Create a user:**
```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"fullName":"Test User","email":"test@example.com","password":"pass1234"}'
```

**Get all tasks:**
```bash
curl http://localhost:8081/api/tasks \
  -H "Authorization: Bearer <token>"
```

**Search with filters:**
```bash
curl "http://localhost:8081/api/tasks?status=IN_PROGRESS&priority=HIGH&page=0&size=5" \
  -H "Authorization: Bearer <token>"
```

**Change task status:**
```bash
curl -X PATCH http://localhost:8081/api/tasks/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"status":"IN_PROGRESS"}'
```

**Try invalid transition (should return 409):**
```bash
curl -X PATCH http://localhost:8081/api/tasks/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"status":"DONE"}'
```

**Delete project with unfinished tasks (should return 409):**
```bash
curl -X DELETE http://localhost:8081/api/projects/1 \
  -H "Authorization: Bearer <token>"
```

---

## Seed Data

The application comes with pre-loaded seed data in `data.sql`:

- **4 Users:** John Doe, Jane Smith, Mike Johnson, Sarah Williams
- **2 Projects:** Web Application (WEB), Mobile App (MOB)
- **10 Tasks:** Various statuses, priorities, and assignments
- **5 Labels:** Bug, Feature, Documentation, Enhancement, Critical
- **8 Comments:** Attached to various tasks
- **Task-Label relationships:** Multiple tasks tagged with labels

---

## Learning Objectives Demonstrated

This project demonstrates:

1. **JPA Relationships:** `@OneToMany`, `@ManyToOne`, `@ManyToMany`
2. **Transactional Business Logic:** `@Transactional` with rollback
3. **State Machine:** Task status workflow enforcement
4. **Dynamic Search:** Specifications with pagination and sorting
5. **Clean Architecture:** DTOs, mappers, layered design
6. **Exception Handling:** Global `@RestControllerAdvice`
7. **Validation:** `@Valid` with custom error responses
8. **RESTful Design:** Proper HTTP methods and status codes

---

## License

This project is created for educational purposes as part of a Spring Boot intermediate workshop.

---

## Author

Created as a demonstration of intermediate Spring Boot development practices.

---

## Contributing

This is an educational project. Feel free to fork and experiment!
