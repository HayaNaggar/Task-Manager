-- Insert Users
INSERT INTO users (full_name, email, created_at) VALUES
('John Doe', 'john.doe@example.com', CURRENT_TIMESTAMP),
('Jane Smith', 'jane.smith@example.com', CURRENT_TIMESTAMP),
('Mike Johnson', 'mike.johnson@example.com', CURRENT_TIMESTAMP),
('Sarah Williams', 'sarah.williams@example.com', CURRENT_TIMESTAMP);

-- Insert Projects
INSERT INTO projects (name, project_key, description, created_at, updated_at) VALUES
('Web Application', 'WEB', 'Main web application project', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Mobile App', 'MOB', 'Mobile application for iOS and Android', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Labels
INSERT INTO labels (name, color) VALUES
('Bug', '#FF0000'),
('Feature', '#00FF00'),
('Documentation', '#0000FF'),
('Enhancement', '#FFA500'),
('Critical', '#FF00FF');

-- Insert Tasks
INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, project_id, reporter_id, assignee_id) VALUES
('Setup authentication system', 'Implement JWT authentication with Spring Security', 'TODO', 'HIGH', '2026-08-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 2);
INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, project_id, reporter_id, assignee_id) VALUES
('Create user dashboard', 'Design and implement the main user dashboard', 'IN_PROGRESS', 'MEDIUM', '2026-08-15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 2, 2);
INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, project_id, reporter_id, assignee_id) VALUES
('Fix login bug', 'Users cannot login with special characters in password', 'IN_REVIEW', 'URGENT', '2026-07-10', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 3, 1);
INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, project_id, reporter_id, assignee_id) VALUES
('Add email notifications', 'Send email notifications for important events', 'DONE', 'MEDIUM', '2026-07-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 3);
INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, project_id, reporter_id, assignee_id) VALUES
('Database migration', 'Migrate from MySQL to PostgreSQL', 'TODO', 'LOW', '2026-09-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 4, NULL);
INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, project_id, reporter_id, assignee_id) VALUES
('Design onboarding flow', 'Create wireframes for user onboarding', 'IN_PROGRESS', 'HIGH', '2026-08-05', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 2, 4);
INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, project_id, reporter_id, assignee_id) VALUES
('Implement push notifications', 'Add FCM for Android and APNS for iOS', 'TODO', 'MEDIUM', '2026-08-20', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 1, 3);
INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, project_id, reporter_id, assignee_id) VALUES
('Test offline mode', 'Verify app functionality without internet', 'IN_REVIEW', 'HIGH', '2026-07-15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 3, 2);
INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, project_id, reporter_id, assignee_id) VALUES
('Update splash screen', 'Redesign app splash screen with new branding', 'DONE', 'LOW', '2026-06-20', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 4, 1);
INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, project_id, reporter_id, assignee_id) VALUES
('Optimize battery usage', 'Reduce battery consumption for background tasks', 'TODO', 'MEDIUM', '2026-08-25', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 2, NULL);

-- Insert Task-Label Relationships
INSERT INTO task_labels (task_id, label_id) VALUES (1, 2), (1, 4);
INSERT INTO task_labels (task_id, label_id) VALUES (2, 2);
INSERT INTO task_labels (task_id, label_id) VALUES (3, 1), (3, 5);
INSERT INTO task_labels (task_id, label_id) VALUES (4, 2);
INSERT INTO task_labels (task_id, label_id) VALUES (6, 2), (6, 3);
INSERT INTO task_labels (task_id, label_id) VALUES (8, 1);

-- Insert Comments
INSERT INTO comments (body, created_at, task_id, author_id) VALUES
('I will start working on this today', CURRENT_TIMESTAMP, 1, 2);
INSERT INTO comments (body, created_at, task_id, author_id) VALUES
('This is a high priority task', CURRENT_TIMESTAMP, 1, 1);
INSERT INTO comments (body, created_at, task_id, author_id) VALUES
('The dashboard design looks great!', CURRENT_TIMESTAMP, 2, 1);
INSERT INTO comments (body, created_at, task_id, author_id) VALUES
('I found a similar issue in ticket WEB-15', CURRENT_TIMESTAMP, 3, 3);
INSERT INTO comments (body, created_at, task_id, author_id) VALUES
('This has been deployed to production', CURRENT_TIMESTAMP, 4, 3);
INSERT INTO comments (body, created_at, task_id, author_id) VALUES
('The wireframes are ready for review', CURRENT_TIMESTAMP, 6, 4);
INSERT INTO comments (body, created_at, task_id, author_id) VALUES
('We need to prioritize this for the next sprint', CURRENT_TIMESTAMP, 7, 1);
INSERT INTO comments (body, created_at, task_id, author_id) VALUES
('Testing completed successfully', CURRENT_TIMESTAMP, 8, 2);
