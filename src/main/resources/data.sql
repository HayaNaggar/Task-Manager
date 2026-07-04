-- Insert Users
INSERT INTO users (full_name, email, created_at) VALUES
('John Doe', 'john.doe@example.com', NOW()),
('Jane Smith', 'jane.smith@example.com', NOW()),
('Mike Johnson', 'mike.johnson@example.com', NOW()),
('Sarah Williams', 'sarah.williams@example.com', NOW());

-- Insert Projects
INSERT INTO projects (name, key, description, created_at, updated_at) VALUES
('Web Application', 'WEB', 'Main web application project', NOW(), NOW()),
('Mobile App', 'MOB', 'Mobile application for iOS and Android', NOW(), NOW());

-- Insert Labels
INSERT INTO labels (name, color) VALUES
('Bug', '#FF0000'),
('Feature', '#00FF00'),
('Documentation', '#0000FF'),
('Enhancement', '#FFA500'),
('Critical', '#FF00FF');

-- Insert Tasks
-- WEB Project Tasks
INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, project_id, reporter_id, assignee_id) VALUES
('Setup authentication system', 'Implement JWT authentication with Spring Security', 'TODO', 'HIGH', '2026-08-01', NOW(), NOW(), 1, 1, 2),
('Create user dashboard', 'Design and implement the main user dashboard', 'IN_PROGRESS', 'MEDIUM', '2026-08-15', NOW(), NOW(), 1, 2, 2),
('Fix login bug', 'Users cannot login with special characters in password', 'IN_REVIEW', 'URGENT', '2026-07-10', NOW(), NOW(), 1, 3, 1),
('Add email notifications', 'Send email notifications for important events', 'DONE', 'MEDIUM', '2026-07-01', NOW(), NOW(), 1, 1, 3),
('Database migration', 'Migrate from MySQL to PostgreSQL', 'TODO', 'LOW', '2026-09-01', NOW(), NOW(), 1, 4, NULL),

-- MOB Project Tasks
('Design onboarding flow', 'Create wireframes for user onboarding', 'IN_PROGRESS', 'HIGH', '2026-08-05', NOW(), NOW(), 2, 2, 4),
('Implement push notifications', 'Add FCM for Android and APNS for iOS', 'TODO', 'MEDIUM', '2026-08-20', NOW(), NOW(), 2, 1, 3),
('Test offline mode', 'Verify app functionality without internet', 'IN_REVIEW', 'HIGH', '2026-07-15', NOW(), NOW(), 2, 3, 2),
('Update splash screen', 'Redesign app splash screen with new branding', 'DONE', '2026-06-20', NOW(), NOW(), 2, 4, 1),
('Optimize battery usage', 'Reduce battery consumption for background tasks', 'TODO', 'MEDIUM', '2026-08-25', NOW(), NOW(), 2, 2, NULL);

-- Insert Task-Label Relationships
-- Task 1 (Setup authentication system)
INSERT INTO task_labels (task_id, label_id) VALUES (1, 2), (1, 4);

-- Task 2 (Create user dashboard)
INSERT INTO task_labels (task_id, label_id) VALUES (2, 2);

-- Task 3 (Fix login bug)
INSERT INTO task_labels (task_id, label_id) VALUES (3, 1), (3, 5);

-- Task 4 (Add email notifications)
INSERT INTO task_labels (task_id, label_id) VALUES (4, 2);

-- Task 6 (Design onboarding flow)
INSERT INTO task_labels (task_id, label_id) VALUES (6, 2), (6, 3);

-- Task 8 (Test offline mode)
INSERT INTO task_labels (task_id, label_id) VALUES (8, 1);

-- Insert Comments
INSERT INTO comments (body, created_at, task_id, author_id) VALUES
('I will start working on this today', NOW(), 1, 2),
('This is a high priority task', NOW(), 1, 1),
('The dashboard design looks great!', NOW(), 2, 1),
('I found a similar issue in ticket WEB-15', NOW(), 3, 3),
('This has been deployed to production', NOW(), 4, 3),
('The wireframes are ready for review', NOW(), 6, 4),
('We need to prioritize this for the next sprint', NOW(), 7, 1),
('Testing completed successfully', NOW(), 8, 2);
