const state = {
  projects: [],
  tasks: [],
  users: []
};

async function loadDashboard() {
  try {
    const [projectsRes, tasksRes, usersRes] = await Promise.all([
      fetch('/api/projects?page=0&size=5'),
      fetch('/api/tasks?page=0&size=8'),
      fetch('/api/users')
    ]);

    if (!projectsRes.ok || !tasksRes.ok || !usersRes.ok) {
      throw new Error('Unable to load dashboard data');
    }

    const projectsData = await projectsRes.json();
    const tasksData = await tasksRes.json();
    const usersData = await usersRes.json();

    state.projects = projectsData.content || [];
    state.tasks = tasksData.content || [];
    state.users = usersData || [];

    renderSummary();
    renderProjects();
    renderTasks();
    renderUsers();
  } catch (error) {
    document.getElementById('projects-list').innerHTML = '<div class="empty-state">Unable to load projects right now.</div>';
    document.getElementById('tasks-list').innerHTML = '<div class="empty-state">Unable to load tasks right now.</div>';
    document.getElementById('users-list').innerHTML = '<div class="empty-state">Unable to load team data right now.</div>';
    console.error(error);
  }
}

function renderSummary() {
  document.getElementById('summary-projects').textContent = state.projects.length;
  document.getElementById('summary-tasks').textContent = state.tasks.length;
  document.getElementById('summary-users').textContent = state.users.length;

  const openTasks = state.tasks.filter((task) => task.status !== 'DONE').length;
  const doneTasks = state.tasks.filter((task) => task.status === 'DONE').length;
  const highPriority = state.tasks.filter((task) => task.priority === 'HIGH' || task.priority === 'URGENT').length;

  document.getElementById('open-tasks').textContent = openTasks;
  document.getElementById('done-tasks').textContent = doneTasks;
  document.getElementById('high-priority').textContent = highPriority;
}

function renderProjects() {
  const container = document.getElementById('projects-list');
  if (!state.projects.length) {
    container.innerHTML = '<div class="empty-state">No projects available yet.</div>';
    return;
  }

  container.innerHTML = state.projects
    .map((project) => `
      <div class="list-item">
        <strong>${project.name}</strong>
        <p>${project.description || 'No description provided.'}</p>
        <span class="badge todo">${project.key}</span>
      </div>
    `)
    .join('');
}

function renderTasks() {
  const container = document.getElementById('tasks-list');
  if (!state.tasks.length) {
    container.innerHTML = '<div class="empty-state">No tasks available yet.</div>';
    return;
  }

  container.innerHTML = state.tasks
    .map((task) => `
      <div class="list-item">
        <strong>${task.title}</strong>
        <p>${task.projectName || 'Unassigned project'} • ${task.assigneeName || 'Unassigned'}</p>
        <span class="badge ${formatStatusClass(task.status)}">${formatLabel(task.status)}</span>
        <span class="badge ${formatPriorityClass(task.priority)}">${formatLabel(task.priority)}</span>
      </div>
    `)
    .join('');
}

function renderUsers() {
  const container = document.getElementById('users-list');
  if (!state.users.length) {
    container.innerHTML = '<div class="empty-state">No team members found.</div>';
    return;
  }

  container.innerHTML = state.users
    .map((user) => `
      <div class="user-card">
        <h4>${user.fullName}</h4>
        <p>${user.email}</p>
      </div>
    `)
    .join('');
}

function formatStatusClass(status) {
  return (status || '').toLowerCase().replace(/_/g, '_');
}

function formatPriorityClass(priority) {
  return (priority || '').toLowerCase();
}

function formatLabel(value) {
  return value ? value.replace(/_/g, ' ') : 'Unknown';
}

document.addEventListener('DOMContentLoaded', loadDashboard);
