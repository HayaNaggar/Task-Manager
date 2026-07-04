const state = {
  projects: [],
  tasks: [],
  users: [],
  token: localStorage.getItem('taskflow-token') || null
};

function getCurrentPage() {
  const path = window.location.pathname.replace(/\/+$/, '');
  if (path === '/dashboard.html' || path === '/dashboard' || path === '') {
    return 'dashboard';
  }
  if (path === '/projects.html' || path === '/projects') {
    return 'projects';
  }
  if (path === '/tasks.html' || path === '/tasks') {
    return 'tasks';
  }
  return 'login';
}

async function login(email, password) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  if (!response.ok) {
    throw new Error('Authentication failed');
  }

  const data = await response.json();
  state.token = data.token;
  localStorage.setItem('taskflow-token', state.token);
  return state.token;
}

async function ensureAuthenticated() {
  if (state.token) {
    return state.token;
  }

  if (getCurrentPage() === 'login') {
    return null;
  }

  throw new Error('Authentication required');
}

async function authFetch(url, options = {}) {
  const token = await ensureAuthenticated();
  if (!token) {
    throw new Error('Authentication required');
  }

  const headers = new Headers(options.headers || {});
  headers.set('Authorization', `Bearer ${token}`);

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  return fetch(url, { ...options, headers });
}

function setStatus(message, isError = false) {
  const element = document.getElementById('auth-message');
  if (!element) {
    return;
  }

  element.textContent = message;
  element.classList.toggle('error', isError);
}

async function handleLoginSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const email = form.email.value.trim();
  const password = form.password.value;

  try {
    setStatus('Signing in…');
    await login(email, password);
    window.location.href = '/dashboard.html';
  } catch (error) {
    setStatus('Unable to sign in. Please check your credentials.', true);
    console.error(error);
  }
}

function logout() {
  localStorage.removeItem('taskflow-token');
  state.token = null;
  window.location.href = '/';
}

function attachGlobalEvents() {
  const loginForm = document.getElementById('login-form');
  if (loginForm) {
    loginForm.addEventListener('submit', handleLoginSubmit);
  }

  const logoutButton = document.getElementById('logout-btn');
  if (logoutButton) {
    logoutButton.addEventListener('click', logout);
  }
}

async function loadDashboardPage() {
  try {
    await ensureAuthenticated();
    updateAuthStatus('Connected as admin');

    const [projectsRes, tasksRes, usersRes] = await Promise.all([
      authFetch('/api/projects?page=0&size=5'),
      authFetch('/api/tasks?page=0&size=8'),
      authFetch('/api/users')
    ]);

    if (!projectsRes.ok || !tasksRes.ok || !usersRes.ok) {
      throw new Error('Unable to load dashboard data');
    }

    const projectsData = await projectsRes.json();
    const tasksData = await tasksRes.json();
    const usersData = await usersRes.json();

    state.projects = normalizeList(projectsData);
    state.tasks = normalizeList(tasksData);
    state.users = normalizeList(usersData);

    renderSummary();
    renderProjectsPreview();
    renderTasksPreview();
    renderUsers();
  } catch (error) {
    updateAuthStatus('Authentication required');
    if (document.getElementById('projects-list')) {
      document.getElementById('projects-list').innerHTML = '<div class="empty-state">Unable to load projects right now.</div>';
    }
    if (document.getElementById('tasks-list')) {
      document.getElementById('tasks-list').innerHTML = '<div class="empty-state">Unable to load tasks right now.</div>';
    }
    if (document.getElementById('users-list')) {
      document.getElementById('users-list').innerHTML = '<div class="empty-state">Unable to load team data right now.</div>';
    }
    console.error(error);
  }
}

async function loadProjectsPage() {
  try {
    await ensureAuthenticated();
    const response = await authFetch('/api/projects?page=0&size=20');
    if (!response.ok) {
      throw new Error('Unable to load projects');
    }

    const data = await response.json();
    state.projects = normalizeList(data);
    renderProjectsPage();
  } catch (error) {
    document.getElementById('projects-page-list').innerHTML = '<div class="empty-state">Unable to load projects right now.</div>';
    console.error(error);
  }
}

async function loadTasksPage() {
  try {
    await ensureAuthenticated();
    const response = await authFetch('/api/tasks?page=0&size=20');
    if (!response.ok) {
      throw new Error('Unable to load tasks');
    }

    const data = await response.json();
    state.tasks = normalizeList(data);
    renderTasksPage();
  } catch (error) {
    document.getElementById('tasks-page-list').innerHTML = '<div class="empty-state">Unable to load tasks right now.</div>';
    console.error(error);
  }
}

function normalizeList(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }
  if (payload && Array.isArray(payload.content)) {
    return payload.content;
  }
  return [];
}

function renderSummary() {
  const projectsCount = state.projects.length;
  const tasksCount = state.tasks.length;
  const usersCount = state.users.length;

  document.getElementById('summary-projects').textContent = projectsCount;
  document.getElementById('summary-tasks').textContent = tasksCount;
  document.getElementById('summary-users').textContent = usersCount;

  const openTasks = state.tasks.filter((task) => (task.status || '').toUpperCase() !== 'DONE').length;
  const doneTasks = state.tasks.filter((task) => (task.status || '').toUpperCase() === 'DONE').length;
  const highPriority = state.tasks.filter((task) => {
    const priority = (task.priority || '').toUpperCase();
    return priority === 'HIGH' || priority === 'URGENT';
  }).length;

  document.getElementById('open-tasks').textContent = openTasks;
  document.getElementById('done-tasks').textContent = doneTasks;
  document.getElementById('high-priority').textContent = highPriority;
}

function renderProjectsPreview() {
  const container = document.getElementById('projects-list');
  if (!container) {
    return;
  }

  if (!state.projects.length) {
    container.innerHTML = '<div class="empty-state">No projects available yet.</div>';
    return;
  }

  container.innerHTML = state.projects
    .slice(0, 4)
    .map((project) => `
      <div class="list-item">
        <strong>${escapeHtml(project.name || 'Untitled project')}</strong>
        <p>${escapeHtml(project.description || 'No description provided.')}</p>
        <span class="badge todo">${escapeHtml(project.projectKey || project.key || 'PRJ')}</span>
      </div>
    `)
    .join('');
}

function renderProjectsPage() {
  const container = document.getElementById('projects-page-list');
  if (!container) {
    return;
  }

  if (!state.projects.length) {
    container.innerHTML = '<div class="empty-state">No projects available yet.</div>';
    return;
  }

  container.innerHTML = state.projects
    .map((project) => `
      <article class="list-item project-card">
        <div>
          <strong>${escapeHtml(project.name || 'Untitled project')}</strong>
          <p>${escapeHtml(project.description || 'No description provided.')}</p>
        </div>
        <div class="card-meta">
          <span class="badge todo">${escapeHtml(project.projectKey || project.key || 'PRJ')}</span>
          <span class="badge low">${escapeHtml(formatLabel(project.status || 'Active'))}</span>
        </div>
      </article>
    `)
    .join('');
}

function renderTasksPreview() {
  const container = document.getElementById('tasks-list');
  if (!container) {
    return;
  }

  if (!state.tasks.length) {
    container.innerHTML = '<div class="empty-state">No tasks available yet.</div>';
    return;
  }

  container.innerHTML = state.tasks
    .slice(0, 6)
    .map((task) => `
      <div class="list-item">
        <strong>${escapeHtml(task.title || 'Untitled task')}</strong>
        <p>${escapeHtml(task.projectName || 'Unassigned project')} • ${escapeHtml(task.assigneeName || 'Unassigned')}</p>
        <div class="badge-row">
          <span class="badge ${formatStatusClass(task.status)}">${escapeHtml(formatLabel(task.status))}</span>
          <span class="badge ${formatPriorityClass(task.priority)}">${escapeHtml(formatLabel(task.priority))}</span>
        </div>
      </div>
    `)
    .join('');
}

function renderTasksPage() {
  const container = document.getElementById('tasks-page-list');
  if (!container) {
    return;
  }

  if (!state.tasks.length) {
    container.innerHTML = '<div class="empty-state">No tasks available yet.</div>';
    return;
  }

  container.innerHTML = state.tasks
    .map((task) => `
      <article class="list-item task-card">
        <div>
          <strong>${escapeHtml(task.title || 'Untitled task')}</strong>
          <p>${escapeHtml(task.projectName || 'Unassigned project')} • ${escapeHtml(task.assigneeName || 'Unassigned')}</p>
          <p>${escapeHtml(task.description || 'No task details available yet.')}</p>
        </div>
        <div class="card-meta">
          <span class="badge ${formatStatusClass(task.status)}">${escapeHtml(formatLabel(task.status))}</span>
          <span class="badge ${formatPriorityClass(task.priority)}">${escapeHtml(formatLabel(task.priority))}</span>
        </div>
      </article>
    `)
    .join('');
}

function renderUsers() {
  const container = document.getElementById('users-list');
  if (!container) {
    return;
  }

  if (!state.users.length) {
    container.innerHTML = '<div class="empty-state">No team members found.</div>';
    return;
  }

  container.innerHTML = state.users
    .map((user) => `
      <div class="user-card">
        <h4>${escapeHtml(user.fullName || user.email || 'Team member')}</h4>
        <p>${escapeHtml(user.email || 'No email listed')}</p>
      </div>
    `)
    .join('');
}

function formatStatusClass(status) {
  const value = (status || '').toUpperCase();
  if (value === 'IN_PROGRESS') return 'in_progress';
  if (value === 'IN_REVIEW') return 'in_review';
  if (value === 'DONE') return 'done';
  return 'todo';
}

function formatPriorityClass(priority) {
  const value = (priority || '').toUpperCase();
  if (value === 'MEDIUM') return 'medium';
  if (value === 'HIGH') return 'high';
  if (value === 'URGENT') return 'urgent';
  return 'low';
}

function formatLabel(value) {
  return value ? value.replace(/_/g, ' ').replace(/\b\w/g, (char) => char.toUpperCase()) : 'Unknown';
}

function updateAuthStatus(message) {
  const element = document.getElementById('auth-status');
  if (element) {
    element.textContent = message;
  }
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

document.addEventListener('DOMContentLoaded', async () => {
  attachGlobalEvents();

  const page = getCurrentPage();
  if (page === 'login') {
    if (state.token) {
      window.location.replace('/dashboard.html');
      return;
    }
    return;
  }

  if (!state.token) {
    window.location.replace('/');
    return;
  }

  if (page === 'dashboard') {
    await loadDashboardPage();
  } else if (page === 'projects') {
    await loadProjectsPage();
  } else if (page === 'tasks') {
    await loadTasksPage();
  }
});
