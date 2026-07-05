/* Task Flow front-end — talks to every endpoint in the Task Management API */

const API_BASE = '/api';

const state = {
  token: localStorage.getItem('taskflow-token') || null,
  projects: [],       // full cache (unpaged, size=100) for dropdowns
  users: [],
  labels: [],
  projectsPage: { content: [], number: 0, totalPages: 0, totalElements: 0 },
  tasksPage: { content: [], number: 0, totalPages: 0, totalElements: 0 },
  taskFilters: { size: 10, sort: 'createdAt,desc' },
  projectPageSize: 10,
  openProjectDetailId: null
};

const ALLOWED_TRANSITIONS = {
  TODO: ['IN_PROGRESS'],
  IN_PROGRESS: ['IN_REVIEW', 'TODO'],
  IN_REVIEW: ['DONE', 'IN_PROGRESS'],
  DONE: ['TODO']
};

/* ---------------------------------------------------------------- */
/* Low-level networking                                              */
/* ---------------------------------------------------------------- */

function getCurrentPage() {
  const path = window.location.pathname.replace(/\/+$/, '');
  if (path === '/dashboard.html' || path === '/dashboard' || path === '') return 'dashboard';
  if (path === '/projects.html' || path === '/projects') return 'projects';
  if (path === '/tasks.html' || path === '/tasks') return 'tasks';
  if (path === '/labels.html' || path === '/labels') return 'labels';
  return 'login';
}

async function apiRequest(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (state.token) headers.set('Authorization', `Bearer ${state.token}`);
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (response.status === 204) return null;

  let data = null;
  const text = await response.text();
  if (text) {
    try { data = JSON.parse(text); } catch (e) { data = null; }
  }

  if (!response.ok) {
    const message = (data && (data.message || data.error)) || `Request failed (${response.status})`;
    const error = new Error(message);
    error.status = response.status;
    error.body = data;
    throw error;
  }

  return data;
}

async function login(email, password) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  if (!response.ok) throw new Error('Authentication failed');

  const data = await response.json();
  state.token = data.token;
  localStorage.setItem('taskflow-token', state.token);
  return state.token;
}

function logout() {
  localStorage.removeItem('taskflow-token');
  state.token = null;
  window.location.href = '/';
}

/* ---------------------------------------------------------------- */
/* Toast + Modal infrastructure                                      */
/* ---------------------------------------------------------------- */

function ensureOverlayRoots() {
  if (!document.getElementById('toast-stack')) {
    const stack = document.createElement('div');
    stack.id = 'toast-stack';
    stack.className = 'toast-stack';
    document.body.appendChild(stack);
  }
  if (!document.getElementById('modal-root')) {
    const overlay = document.createElement('div');
    overlay.id = 'modal-root';
    overlay.className = 'modal-overlay hidden';
    overlay.innerHTML = '<div class="modal-box" id="modal-box"></div>';
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) closeModal();
    });
    document.body.appendChild(overlay);
  }
}

function toast(message, type = 'success') {
  ensureOverlayRoots();
  const stack = document.getElementById('toast-stack');
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = message;
  stack.appendChild(el);
  setTimeout(() => el.remove(), 4200);
}

function openModal(html) {
  ensureOverlayRoots();
  document.getElementById('modal-box').innerHTML = html;
  document.getElementById('modal-root').classList.remove('hidden');
}

function closeModal() {
  const root = document.getElementById('modal-root');
  if (root) root.classList.add('hidden');
}

/* ---------------------------------------------------------------- */
/* Formatting helpers                                                 */
/* ---------------------------------------------------------------- */

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatLabel(value) {
  return value ? String(value).replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()) : 'Unknown';
}

function formatStatusClass(status) {
  const v = (status || '').toUpperCase();
  if (v === 'IN_PROGRESS') return 'in_progress';
  if (v === 'IN_REVIEW') return 'in_review';
  if (v === 'DONE') return 'done';
  return 'todo';
}

function formatPriorityClass(priority) {
  const v = (priority || '').toUpperCase();
  if (v === 'MEDIUM') return 'medium';
  if (v === 'HIGH') return 'high';
  if (v === 'URGENT') return 'urgent';
  return 'low';
}

function formatDate(value) {
  if (!value) return '—';
  return String(value).slice(0, 10);
}

function normalizeList(payload) {
  if (Array.isArray(payload)) return payload;
  if (payload && Array.isArray(payload.content)) return payload.content;
  return [];
}

function userOptions(selectedId) {
  return `<option value="">— none —</option>` + state.users.map((u) =>
    `<option value="${u.id}" ${String(u.id) === String(selectedId) ? 'selected' : ''}>${escapeHtml(u.fullName)}</option>`
  ).join('');
}

function projectOptions(selectedId) {
  return state.projects.map((p) =>
    `<option value="${p.id}" ${String(p.id) === String(selectedId) ? 'selected' : ''}>${escapeHtml(p.key)} — ${escapeHtml(p.name)}</option>`
  ).join('');
}

function labelOptionsNotOn(task) {
  const attached = new Set((task.labels || []).map((l) => l.id));
  return state.labels.filter((l) => !attached.has(l.id)).map((l) =>
    `<option value="${l.id}">${escapeHtml(l.name)}</option>`
  ).join('');
}

/* ---------------------------------------------------------------- */
/* Reference data (loaded once per authenticated page)                */
/* ---------------------------------------------------------------- */

async function loadReferenceData() {
  const [projectsPage, users, labels] = await Promise.all([
    apiRequest('/projects?page=0&size=100'),
    apiRequest('/users'),
    apiRequest('/labels')
  ]);
  state.projects = normalizeList(projectsPage);
  state.users = normalizeList(users);
  state.labels = normalizeList(labels);
}

/* ---------------------------------------------------------------- */
/* Auth / login page                                                  */
/* ---------------------------------------------------------------- */

function setStatus(message, isError = false) {
  const element = document.getElementById('auth-message');
  if (!element) return;
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

/* ---------------------------------------------------------------- */
/* Dashboard page                                                     */
/* ---------------------------------------------------------------- */

async function loadDashboardPage() {
  try {
    updateAuthStatus('Connected as admin');
    await loadReferenceData();

    const tasksData = await apiRequest('/tasks?page=0&size=8&sort=createdAt,desc');
    const allTasksForStats = await apiRequest('/tasks?page=0&size=200');

    const tasksPreview = normalizeList(tasksData);
    const allTasks = normalizeList(allTasksForStats);

    document.getElementById('summary-projects').textContent = state.projects.length;
    document.getElementById('summary-tasks').textContent = allTasksForStats.totalElements ?? allTasks.length;
    document.getElementById('summary-users').textContent = state.users.length;

    const openTasks = allTasks.filter((t) => (t.status || '').toUpperCase() !== 'DONE').length;
    const doneTasks = allTasks.filter((t) => (t.status || '').toUpperCase() === 'DONE').length;
    const highPriority = allTasks.filter((t) => ['HIGH', 'URGENT'].includes((t.priority || '').toUpperCase())).length;

    document.getElementById('open-tasks').textContent = openTasks;
    document.getElementById('done-tasks').textContent = doneTasks;
    document.getElementById('high-priority').textContent = highPriority;

    renderDashboardProjects();
    renderDashboardTasks(tasksPreview);
    renderDashboardUsers();
  } catch (error) {
    updateAuthStatus('Authentication required');
    console.error(error);
    toast(error.message || 'Unable to load dashboard', 'error');
  }
}

function renderDashboardProjects() {
  const container = document.getElementById('projects-list');
  if (!container) return;
  if (!state.projects.length) {
    container.innerHTML = '<div class="empty-state">No projects available yet.</div>';
    return;
  }
  container.innerHTML = state.projects.slice(0, 4).map((project) => `
    <div class="list-item">
      <strong>${escapeHtml(project.name)}</strong>
      <p>${escapeHtml(project.description || 'No description provided.')}</p>
      <span class="badge todo">${escapeHtml(project.key)}</span>
    </div>
  `).join('');
}

function renderDashboardTasks(tasks) {
  const container = document.getElementById('tasks-list');
  if (!container) return;
  if (!tasks.length) {
    container.innerHTML = '<div class="empty-state">No tasks available yet.</div>';
    return;
  }
  container.innerHTML = tasks.map((task) => `
    <div class="list-item">
      <strong>${escapeHtml(task.title)}</strong>
      <p>${escapeHtml(task.projectName || 'Unassigned project')} • ${escapeHtml(task.assigneeName || 'Unassigned')}</p>
      <div class="badge-row">
        <span class="badge ${formatStatusClass(task.status)}">${escapeHtml(formatLabel(task.status))}</span>
        <span class="badge ${formatPriorityClass(task.priority)}">${escapeHtml(formatLabel(task.priority))}</span>
      </div>
    </div>
  `).join('');
}

function renderDashboardUsers() {
  const container = document.getElementById('users-list');
  if (!container) return;
  if (!state.users.length) {
    container.innerHTML = '<div class="empty-state">No team members found.</div>';
    return;
  }
  container.innerHTML = state.users.map((user) => `
    <div class="user-card">
      <h4>${escapeHtml(user.fullName)}</h4>
      <p>${escapeHtml(user.email)}</p>
    </div>
  `).join('');
}

async function handleCreateUserSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const payload = {
    fullName: form.fullName.value.trim(),
    email: form.email.value.trim(),
    password: form.password.value
  };
  try {
    await apiRequest('/users', { method: 'POST', body: JSON.stringify(payload) });
    toast('User created');
    form.reset();
    await loadReferenceData();
    renderDashboardUsers();
  } catch (error) {
    toast(error.message, 'error');
  }
}

function updateAuthStatus(message) {
  const element = document.getElementById('auth-status');
  if (element) element.textContent = message;
}

/* ---------------------------------------------------------------- */
/* Projects page                                                      */
/* ---------------------------------------------------------------- */

async function loadProjectsPage() {
  try {
    await loadReferenceData();
    await refreshProjectsList();
  } catch (error) {
    console.error(error);
    document.getElementById('projects-page-list').innerHTML = '<div class="empty-state">Unable to load projects right now.</div>';
    toast(error.message || 'Unable to load projects', 'error');
  }
}

async function refreshProjectsList(page = 0) {
  const data = await apiRequest(`/projects?page=${page}&size=${state.projectPageSize}&sort=name,asc`);
  state.projectsPage = {
    content: normalizeList(data),
    number: data.number ?? 0,
    totalPages: data.totalPages ?? 1,
    totalElements: data.totalElements ?? normalizeList(data).length
  };
  state.projects = state.projects.length ? state.projects : normalizeList(data);
  renderProjectsPage();
}

function renderProjectsPage() {
  const container = document.getElementById('projects-page-list');
  const { content, number, totalPages, totalElements } = state.projectsPage;

  if (!content.length) {
    container.innerHTML = '<div class="empty-state">No projects available yet.</div>';
  } else {
    container.innerHTML = content.map((project) => renderProjectCard(project)).join('');
  }

  const paginationEl = document.getElementById('projects-pagination');
  if (paginationEl) {
    paginationEl.innerHTML = `
      <span>Page ${number + 1} of ${Math.max(totalPages, 1)} · ${totalElements} project${totalElements === 1 ? '' : 's'}</span>
      <div class="pagination-controls">
        <button class="btn-sm" data-action="projects-prev-page" ${number <= 0 ? 'disabled' : ''}>Prev</button>
        <button class="btn-sm" data-action="projects-next-page" ${number + 1 >= totalPages ? 'disabled' : ''}>Next</button>
      </div>`;
  }
}

function renderProjectCard(project) {
  const isEditing = state.editingProjectId === project.id;
  const isViewing = state.openProjectDetailId === project.id;

  if (isEditing) {
    return `
      <article class="list-item project-card" data-project-id="${project.id}">
        <div style="flex:1;">
          <div class="field-grid single">
            <div class="field">
              <label>Name</label>
              <input type="text" id="edit-name-${project.id}" value="${escapeHtml(project.name)}" />
            </div>
            <div class="field">
              <label>Description</label>
              <textarea id="edit-desc-${project.id}">${escapeHtml(project.description || '')}</textarea>
            </div>
          </div>
          <div class="action-row">
            <button class="btn-sm accent" data-action="save-project" data-id="${project.id}">Save</button>
            <button class="btn-sm" data-action="cancel-edit-project" data-id="${project.id}">Cancel</button>
          </div>
        </div>
      </article>`;
  }

  return `
    <article class="list-item project-card" data-project-id="${project.id}">
      <div style="flex:1;">
        <strong>${escapeHtml(project.name)}</strong>
        <p>${escapeHtml(project.description || 'No description provided.')}</p>
        <div class="card-meta">
          <span class="badge todo">${escapeHtml(project.key)}</span>
        </div>
        ${isViewing ? renderProjectDetail(project) : ''}
      </div>
      <div class="action-row">
        <button class="btn-sm" data-action="view-project" data-id="${project.id}">${isViewing ? 'Hide' : 'View'}</button>
        <button class="btn-sm" data-action="edit-project" data-id="${project.id}">Edit</button>
        <button class="btn-sm danger" data-action="delete-project" data-id="${project.id}">Delete</button>
      </div>
    </article>`;
}

function renderProjectDetail(project) {
  const detail = state.projectDetailCache && state.projectDetailCache[project.id];
  if (!detail) {
    return `<div class="editable-block"><span class="form-note">Loading…</span></div>`;
  }
  const { summary, tasks } = detail;
  return `
    <div class="editable-block">
      <div class="badge-row">
        <span class="badge todo">Total ${summary.totalTasks}</span>
        <span class="badge todo">Todo ${summary.todoCount}</span>
        <span class="badge in_progress">In progress ${summary.inProgressCount}</span>
        <span class="badge in_review">In review ${summary.inReviewCount}</span>
        <span class="badge done">Done ${summary.doneCount}</span>
        <span class="badge urgent">Overdue ${summary.overdueCount}</span>
      </div>
      <div class="list-stack" style="margin-top:10px;">
        ${tasks.length ? tasks.map((t) => `
          <div class="list-item">
            <strong>${escapeHtml(t.title)}</strong>
            <div class="badge-row">
              <span class="badge ${formatStatusClass(t.status)}">${escapeHtml(formatLabel(t.status))}</span>
              <span class="badge ${formatPriorityClass(t.priority)}">${escapeHtml(formatLabel(t.priority))}</span>
            </div>
          </div>`).join('') : '<div class="empty-state">No tasks in this project yet.</div>'}
      </div>
    </div>`;
}

async function toggleProjectView(id) {
  if (state.openProjectDetailId === id) {
    state.openProjectDetailId = null;
    renderProjectsPage();
    return;
  }
  state.openProjectDetailId = id;
  renderProjectsPage();
  try {
    const [summary, tasks] = await Promise.all([
      apiRequest(`/projects/${id}/summary`),
      apiRequest(`/projects/${id}/tasks`)
    ]);
    state.projectDetailCache = state.projectDetailCache || {};
    state.projectDetailCache[id] = { summary, tasks: normalizeList(tasks) };
    renderProjectsPage();
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function handleCreateProjectSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const payload = {
    name: form.name.value.trim(),
    key: form.key.value.trim().toUpperCase(),
    description: form.description.value.trim()
  };
  try {
    await apiRequest('/projects', { method: 'POST', body: JSON.stringify(payload) });
    toast('Project created');
    form.reset();
    await loadReferenceData();
    await refreshProjectsList(0);
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function saveProjectEdit(id) {
  const name = document.getElementById(`edit-name-${id}`).value.trim();
  const description = document.getElementById(`edit-desc-${id}`).value.trim();
  try {
    await apiRequest(`/projects/${id}`, { method: 'PUT', body: JSON.stringify({ name, description }) });
    toast('Project updated');
    state.editingProjectId = null;
    await loadReferenceData();
    await refreshProjectsList(state.projectsPage.number);
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function deleteProject(id) {
  if (!window.confirm('Delete this project? This is blocked if it has open (non-DONE) tasks.')) return;
  try {
    await apiRequest(`/projects/${id}`, { method: 'DELETE' });
    toast('Project deleted');
    await loadReferenceData();
    await refreshProjectsList(0);
  } catch (error) {
    toast(error.message, 'error');
  }
}

/* ---------------------------------------------------------------- */
/* Tasks page                                                         */
/* ---------------------------------------------------------------- */

function buildTaskQuery(page) {
  const f = state.taskFilters;
  const params = new URLSearchParams();
  params.set('page', page ?? 0);
  params.set('size', f.size || 10);
  if (f.sort) params.set('sort', f.sort);
  if (f.projectId) params.set('projectId', f.projectId);
  if (f.status) params.set('status', f.status);
  if (f.priority) params.set('priority', f.priority);
  if (f.assigneeId) params.set('assigneeId', f.assigneeId);
  if (f.reporterId) params.set('reporterId', f.reporterId);
  if (f.labelId) params.set('labelId', f.labelId);
  if (f.dueBefore) params.set('dueBefore', f.dueBefore);
  if (f.dueAfter) params.set('dueAfter', f.dueAfter);
  if (f.keyword) params.set('keyword', f.keyword);
  return params.toString();
}

async function loadTasksPage() {
  try {
    await loadReferenceData();
    renderFilterBar();
    await refreshTasksList(0);
  } catch (error) {
    console.error(error);
    document.getElementById('tasks-page-list').innerHTML = '<div class="empty-state">Unable to load tasks right now.</div>';
    toast(error.message || 'Unable to load tasks', 'error');
  }
}

function renderFilterBar() {
  const bar = document.getElementById('task-filter-bar');
  if (!bar) return;
  const f = state.taskFilters;
  bar.innerHTML = `
    <div class="field">
      <label>Project</label>
      <select id="filter-projectId"><option value="">All projects</option>${projectOptions(f.projectId)}</select>
    </div>
    <div class="field">
      <label>Status</label>
      <select id="filter-status">
        <option value="">All statuses</option>
        ${['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'].map((s) => `<option value="${s}" ${f.status === s ? 'selected' : ''}>${formatLabel(s)}</option>`).join('')}
      </select>
    </div>
    <div class="field">
      <label>Priority</label>
      <select id="filter-priority">
        <option value="">All priorities</option>
        ${['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((p) => `<option value="${p}" ${f.priority === p ? 'selected' : ''}>${formatLabel(p)}</option>`).join('')}
      </select>
    </div>
    <div class="field">
      <label>Assignee</label>
      <select id="filter-assigneeId"><option value="">Anyone</option>${userOptions(f.assigneeId)}</select>
    </div>
    <div class="field">
      <label>Reporter</label>
      <select id="filter-reporterId"><option value="">Anyone</option>${userOptions(f.reporterId)}</select>
    </div>
    <div class="field">
      <label>Label</label>
      <select id="filter-labelId"><option value="">Any label</option>${state.labels.map((l) => `<option value="${l.id}" ${f.labelId === String(l.id) ? 'selected' : ''}>${escapeHtml(l.name)}</option>`).join('')}</select>
    </div>
    <div class="field">
      <label>Due after</label>
      <input type="date" id="filter-dueAfter" value="${f.dueAfter || ''}" />
    </div>
    <div class="field">
      <label>Due before</label>
      <input type="date" id="filter-dueBefore" value="${f.dueBefore || ''}" />
    </div>
    <div class="field">
      <label>Keyword</label>
      <input type="text" id="filter-keyword" placeholder="title or description" value="${escapeHtml(f.keyword || '')}" />
    </div>
    <div class="field">
      <label>Sort</label>
      <select id="filter-sort">
        ${[
          ['createdAt,desc', 'Newest first'],
          ['dueDate,asc', 'Due date ↑'],
          ['dueDate,desc', 'Due date ↓'],
          ['priority,asc', 'Priority ↑'],
          ['priority,desc', 'Priority ↓'],
          ['title,asc', 'Title A–Z']
        ].map(([val, label]) => `<option value="${val}" ${f.sort === val ? 'selected' : ''}>${label}</option>`).join('')}
      </select>
    </div>
    <div class="field">
      <label>Page size</label>
      <select id="filter-size">
        ${[5, 10, 20, 50].map((s) => `<option value="${s}" ${Number(f.size) === s ? 'selected' : ''}>${s}</option>`).join('')}
      </select>
    </div>
    <div class="field" style="align-self:end;">
      <div class="action-row">
        <button class="btn-sm accent" data-action="apply-filters">Apply</button>
        <button class="btn-sm" data-action="reset-filters">Reset</button>
      </div>
    </div>`;
}

function readFiltersFromForm() {
  const val = (id) => document.getElementById(id)?.value?.trim() || '';
  state.taskFilters = {
    projectId: val('filter-projectId'),
    status: val('filter-status'),
    priority: val('filter-priority'),
    assigneeId: val('filter-assigneeId'),
    reporterId: val('filter-reporterId'),
    labelId: val('filter-labelId'),
    dueAfter: val('filter-dueAfter'),
    dueBefore: val('filter-dueBefore'),
    keyword: val('filter-keyword'),
    sort: val('filter-sort') || 'createdAt,desc',
    size: Number(val('filter-size')) || 10
  };
}

async function refreshTasksList(page = 0) {
  const query = buildTaskQuery(page);
  const data = await apiRequest(`/tasks?${query}`);
  state.tasksPage = {
    content: normalizeList(data),
    number: data.number ?? 0,
    totalPages: data.totalPages ?? 1,
    totalElements: data.totalElements ?? normalizeList(data).length
  };
  renderTasksPage();
}

function renderTasksPage() {
  const container = document.getElementById('tasks-page-list');
  const { content, number, totalPages, totalElements } = state.tasksPage;

  if (!content.length) {
    container.innerHTML = '<div class="empty-state">No tasks match these filters.</div>';
  } else {
    container.innerHTML = content.map((task) => renderTaskCard(task)).join('');
  }

  const paginationEl = document.getElementById('tasks-pagination');
  if (paginationEl) {
    paginationEl.innerHTML = `
      <span>Page ${number + 1} of ${Math.max(totalPages, 1)} · ${totalElements} task${totalElements === 1 ? '' : 's'}</span>
      <div class="pagination-controls">
        <button class="btn-sm" data-action="tasks-prev-page" ${number <= 0 ? 'disabled' : ''}>Prev</button>
        <button class="btn-sm" data-action="tasks-next-page" ${number + 1 >= totalPages ? 'disabled' : ''}>Next</button>
      </div>`;
  }
}

function renderTaskCard(task) {
  const nextStatuses = ALLOWED_TRANSITIONS[(task.status || '').toUpperCase()] || [];
  const labelChips = (task.labels || []).map((l) =>
    `<span class="chip"><span class="dot" style="background:${escapeHtml(l.color)}"></span>${escapeHtml(l.name)}</span>`
  ).join('');

  return `
    <article class="list-item task-card" data-task-id="${task.id}">
      <div class="task-card-top">
        <div>
          <strong>${escapeHtml(task.title)}</strong>
          <p>${escapeHtml(task.projectKey || '')} · ${escapeHtml(task.projectName || 'Unassigned project')} • ${escapeHtml(task.assigneeName || 'Unassigned')} • due ${formatDate(task.dueDate)}</p>
        </div>
        <div class="card-meta">
          <span class="badge ${formatStatusClass(task.status)}">${escapeHtml(formatLabel(task.status))}</span>
          <span class="badge ${formatPriorityClass(task.priority)}">${escapeHtml(formatLabel(task.priority))}</span>
        </div>
      </div>
      ${labelChips ? `<div class="chip-row">${labelChips}</div>` : ''}
      <div class="action-row">
        ${nextStatuses.map((s) => `<button class="btn-sm" data-action="status-transition" data-id="${task.id}" data-status="${s}">${s === 'TODO' ? 'Reopen' : '→ ' + formatLabel(s)}</button>`).join('')}
        <button class="btn-sm accent" data-action="open-task-detail" data-id="${task.id}">Details</button>
        <button class="btn-sm danger" data-action="delete-task" data-id="${task.id}">Delete</button>
      </div>
    </article>`;
}

function openCreateTaskModal() {
  openModal(`
    <h3>New task</h3>
    <p class="modal-sub">Create a task inside one of your projects.</p>
    <form id="create-task-form">
      <div class="field-grid single">
        <div class="field"><label>Title</label><input type="text" name="title" required minlength="3" /></div>
        <div class="field"><label>Description</label><textarea name="description"></textarea></div>
      </div>
      <div class="field-grid">
        <div class="field"><label>Project</label><select name="projectId" required>${projectOptions()}</select></div>
        <div class="field"><label>Priority</label>
          <select name="priority" required>
            ${['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((p) => `<option value="${p}">${formatLabel(p)}</option>`).join('')}
          </select>
        </div>
        <div class="field"><label>Reporter</label><select name="reporterId" required>${userOptions()}</select></div>
        <div class="field"><label>Assignee (optional)</label><select name="assigneeId">${userOptions()}</select></div>
        <div class="field"><label>Due date</label><input type="date" name="dueDate" /></div>
      </div>
      <p class="form-note" id="create-task-note"></p>
      <div class="form-actions">
        <button type="submit" class="primary-btn">Create task</button>
        <button type="button" class="secondary-btn" data-action="close-modal">Cancel</button>
      </div>
    </form>
  `);
  document.getElementById('create-task-form').addEventListener('submit', handleCreateTaskSubmit);
}

async function handleCreateTaskSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const projectId = form.projectId.value;
  const payload = {
    title: form.title.value.trim(),
    description: form.description.value.trim(),
    priority: form.priority.value,
    reporterId: Number(form.reporterId.value),
    assigneeId: form.assigneeId.value ? Number(form.assigneeId.value) : null,
    dueDate: form.dueDate.value || null
  };
  try {
    await apiRequest(`/projects/${projectId}/tasks`, { method: 'POST', body: JSON.stringify(payload) });
    toast('Task created');
    closeModal();
    await refreshTasksList(0);
  } catch (error) {
    document.getElementById('create-task-note').textContent = error.message;
    document.getElementById('create-task-note').classList.add('error');
  }
}

async function openTaskDetailModal(id) {
  try {
    const task = await apiRequest(`/tasks/${id}`);
    renderTaskDetailModal(task);
  } catch (error) {
    toast(error.message, 'error');
  }
}

function renderTaskDetailModal(task) {
  const comments = task.comments || [];
  const nextStatuses = ALLOWED_TRANSITIONS[(task.status || '').toUpperCase()] || [];
  const labelChips = (task.labels || []).map((l) =>
    `<span class="chip"><span class="dot" style="background:${escapeHtml(l.color)}"></span>${escapeHtml(l.name)}<button data-action="detach-label" data-taskid="${task.id}" data-labelid="${l.id}">×</button></span>`
  ).join('');
  const availableLabels = labelOptionsNotOn(task);

  openModal(`
    <h3>${escapeHtml(task.title)}</h3>
    <p class="modal-sub">${escapeHtml(task.projectKey || '')} · ${escapeHtml(task.projectName || '')}</p>

    <div class="badge-row">
      <span class="badge ${formatStatusClass(task.status)}">${escapeHtml(formatLabel(task.status))}</span>
      <span class="badge ${formatPriorityClass(task.priority)}">${escapeHtml(formatLabel(task.priority))}</span>
    </div>

    <form id="edit-task-form" style="margin-top:14px;">
      <div class="field-grid single">
        <div class="field"><label>Title</label><input type="text" name="title" value="${escapeHtml(task.title)}" minlength="3" /></div>
        <div class="field"><label>Description</label><textarea name="description">${escapeHtml(task.description || '')}</textarea></div>
      </div>
      <div class="field-grid">
        <div class="field"><label>Priority</label>
          <select name="priority">${['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((p) => `<option value="${p}" ${task.priority === p ? 'selected' : ''}>${formatLabel(p)}</option>`).join('')}</select>
        </div>
        <div class="field"><label>Due date</label><input type="date" name="dueDate" value="${task.dueDate || ''}" /></div>
      </div>
      <div class="form-actions"><button type="submit" class="btn-sm accent" data-id="${task.id}">Save changes</button></div>
    </form>

    <div class="detail-section">
      <h4>Status</h4>
      <div class="action-row">
        ${nextStatuses.length
          ? nextStatuses.map((s) => `<button class="btn-sm" data-action="status-transition-modal" data-id="${task.id}" data-status="${s}">${s === 'TODO' ? 'Reopen' : '→ ' + formatLabel(s)}</button>`).join('')
          : '<span class="form-note">No further transitions.</span>'}
      </div>
    </div>

    <div class="detail-section">
      <h4>Assignment</h4>
      <div class="field-grid">
        <div class="field">
          <label>Assignee</label>
          <select id="assignee-select-${task.id}">${userOptions(task.assigneeId)}</select>
        </div>
        <div class="field" style="align-self:end;">
          <div class="action-row">
            <button class="btn-sm accent" data-action="assign-task" data-id="${task.id}">Set assignee</button>
            <button class="btn-sm" data-action="unassign-task" data-id="${task.id}">Unassign</button>
          </div>
        </div>
      </div>
      <div class="field-grid" style="margin-top:10px;">
        <div class="field">
          <label>Move to project</label>
          <select id="move-select-${task.id}">${projectOptions(task.projectId)}</select>
        </div>
        <div class="field" style="align-self:end;">
          <button class="btn-sm accent" data-action="move-task" data-id="${task.id}">Move</button>
        </div>
      </div>
    </div>

    <div class="detail-section">
      <h4>Labels</h4>
      <div class="chip-row">${labelChips || '<span class="form-note">No labels attached.</span>'}</div>
      <div class="field-grid" style="margin-top:10px;">
        <div class="field"><label>Attach label</label><select id="label-select-${task.id}">${availableLabels || '<option value="">No more labels</option>'}</select></div>
        <div class="field" style="align-self:end;"><button class="btn-sm accent" data-action="attach-label" data-id="${task.id}">Attach</button></div>
      </div>
    </div>

    <div class="detail-section">
      <h4>Comments (${comments.length})</h4>
      <div id="comments-list">
        ${comments.length ? comments.map((c) => `
          <div class="comment-item">
            <div class="comment-meta"><strong>${escapeHtml(c.authorName || 'Unknown')}</strong><span>${formatDate(c.createdAt)}</span></div>
            <p style="margin:0;">${escapeHtml(c.body)}</p>
          </div>`).join('') : '<div class="empty-state">No comments yet.</div>'}
      </div>
      <form id="add-comment-form" class="comment-form">
        <select id="comment-author-${task.id}" style="width:140px;">${userOptions(task.reporterId)}</select>
        <input type="text" name="body" placeholder="Add a comment…" required />
        <button type="submit" class="btn-sm accent" data-id="${task.id}">Post</button>
      </form>
    </div>

    <div class="modal-close-row"><button class="secondary-btn" data-action="close-modal">Close</button></div>
  `);

  document.getElementById('edit-task-form').addEventListener('submit', (e) => handleEditTaskSubmit(e, task.id));
  document.getElementById('add-comment-form').addEventListener('submit', (e) => handleAddCommentSubmit(e, task.id));
}

async function handleEditTaskSubmit(event, id) {
  event.preventDefault();
  const form = event.currentTarget;
  const payload = {
    title: form.title.value.trim() || null,
    description: form.description.value,
    priority: form.priority.value,
    dueDate: form.dueDate.value || null
  };
  try {
    await apiRequest(`/tasks/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
    toast('Task updated');
    closeModal();
    await refreshTasksList(state.tasksPage.number);
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function handleAddCommentSubmit(event, taskId) {
  event.preventDefault();
  const form = event.currentTarget;
  const authorId = Number(document.getElementById(`comment-author-${taskId}`).value);
  const body = form.body.value.trim();
  if (!body) return;
  try {
    await apiRequest(`/tasks/${taskId}/comments`, { method: 'POST', body: JSON.stringify({ body, authorId }) });
    toast('Comment added');
    const task = await apiRequest(`/tasks/${taskId}`);
    renderTaskDetailModal(task);
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function changeTaskStatus(id, status, refreshModal = false) {
  try {
    await apiRequest(`/tasks/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) });
    toast(`Task moved to ${formatLabel(status)}`);
    if (refreshModal) {
      const task = await apiRequest(`/tasks/${id}`);
      renderTaskDetailModal(task);
    }
    await refreshTasksList(state.tasksPage.number);
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function assignTask(id) {
  const select = document.getElementById(`assignee-select-${id}`);
  const assigneeId = select.value;
  if (!assigneeId) { toast('Choose a user to assign', 'error'); return; }
  try {
    await apiRequest(`/tasks/${id}/assignee`, { method: 'PATCH', body: JSON.stringify({ assigneeId: Number(assigneeId) }) });
    toast('Task assigned');
    const task = await apiRequest(`/tasks/${id}`);
    renderTaskDetailModal(task);
    await refreshTasksList(state.tasksPage.number);
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function unassignTask(id) {
  try {
    await apiRequest(`/tasks/${id}/assignee`, { method: 'PATCH', body: JSON.stringify({ assigneeId: null }) });
    toast('Task unassigned');
    const task = await apiRequest(`/tasks/${id}`);
    renderTaskDetailModal(task);
    await refreshTasksList(state.tasksPage.number);
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function moveTask(id) {
  const select = document.getElementById(`move-select-${id}`);
  const projectId = select.value;
  try {
    await apiRequest(`/tasks/${id}/project`, { method: 'PATCH', body: JSON.stringify({ projectId: Number(projectId) }) });
    toast('Task moved');
    const task = await apiRequest(`/tasks/${id}`);
    renderTaskDetailModal(task);
    await refreshTasksList(state.tasksPage.number);
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function attachLabel(id) {
  const select = document.getElementById(`label-select-${id}`);
  const labelId = select.value;
  if (!labelId) { toast('No label selected', 'error'); return; }
  try {
    await apiRequest(`/tasks/${id}/labels/${labelId}`, { method: 'POST' });
    toast('Label attached');
    const task = await apiRequest(`/tasks/${id}`);
    renderTaskDetailModal(task);
    await refreshTasksList(state.tasksPage.number);
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function detachLabel(taskId, labelId) {
  try {
    await apiRequest(`/tasks/${taskId}/labels/${labelId}`, { method: 'DELETE' });
    toast('Label removed');
    const task = await apiRequest(`/tasks/${taskId}`);
    renderTaskDetailModal(task);
    await refreshTasksList(state.tasksPage.number);
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function deleteTask(id) {
  if (!window.confirm('Delete this task? This also removes its comments.')) return;
  try {
    await apiRequest(`/tasks/${id}`, { method: 'DELETE' });
    toast('Task deleted');
    await refreshTasksList(state.tasksPage.number);
  } catch (error) {
    toast(error.message, 'error');
  }
}

/* ---------------------------------------------------------------- */
/* Labels page                                                        */
/* ---------------------------------------------------------------- */

async function loadLabelsPage() {
  try {
    await loadReferenceData();
    renderLabelsList();
  } catch (error) {
    console.error(error);
    toast(error.message || 'Unable to load labels', 'error');
  }
}

function renderLabelsList() {
  const container = document.getElementById('labels-page-list');
  if (!container) return;
  if (!state.labels.length) {
    container.innerHTML = '<div class="empty-state">No labels yet. Create one to start tagging tasks.</div>';
    return;
  }
  container.innerHTML = state.labels.map((label) => `
    <div class="list-item" style="display:flex; align-items:center; justify-content:space-between;">
      <span class="chip"><span class="dot" style="background:${escapeHtml(label.color)}"></span>${escapeHtml(label.name)}</span>
      <span class="form-note">${escapeHtml(label.color)}</span>
    </div>
  `).join('');
}

async function handleCreateLabelSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const payload = { name: form.name.value.trim(), color: form.color.value };
  try {
    await apiRequest('/labels', { method: 'POST', body: JSON.stringify(payload) });
    toast('Label created');
    form.reset();
    await loadReferenceData();
    renderLabelsList();
  } catch (error) {
    toast(error.message, 'error');
  }
}

/* ---------------------------------------------------------------- */
/* Global event delegation                                            */
/* ---------------------------------------------------------------- */

function attachGlobalEvents() {
  const loginForm = document.getElementById('login-form');
  if (loginForm) loginForm.addEventListener('submit', handleLoginSubmit);

  const logoutButton = document.getElementById('logout-btn');
  if (logoutButton) logoutButton.addEventListener('click', logout);

  const createUserForm = document.getElementById('create-user-form');
  if (createUserForm) createUserForm.addEventListener('submit', handleCreateUserSubmit);

  const createProjectForm = document.getElementById('create-project-form');
  if (createProjectForm) createProjectForm.addEventListener('submit', handleCreateProjectSubmit);

  const createLabelForm = document.getElementById('create-label-form');
  if (createLabelForm) createLabelForm.addEventListener('submit', handleCreateLabelSubmit);

  const newTaskBtn = document.getElementById('new-task-btn');
  if (newTaskBtn) newTaskBtn.addEventListener('click', openCreateTaskModal);

  const applyFiltersBtnHolder = document.getElementById('task-filter-bar');
  if (applyFiltersBtnHolder) {
    applyFiltersBtnHolder.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      if (btn.dataset.action === 'apply-filters') {
        readFiltersFromForm();
        refreshTasksList(0);
      } else if (btn.dataset.action === 'reset-filters') {
        state.taskFilters = { size: 10, sort: 'createdAt,desc' };
        renderFilterBar();
        refreshTasksList(0);
      }
    });
  }

  document.body.addEventListener('click', (event) => {
    const target = event.target.closest('[data-action]');
    if (!target) return;
    const { action, id, status, taskid, labelid } = target.dataset;

    switch (action) {
      case 'close-modal': closeModal(); break;

      case 'view-project': toggleProjectView(Number(id)); break;
      case 'edit-project': state.editingProjectId = Number(id); renderProjectsPage(); break;
      case 'cancel-edit-project': state.editingProjectId = null; renderProjectsPage(); break;
      case 'save-project': saveProjectEdit(Number(id)); break;
      case 'delete-project': deleteProject(Number(id)); break;
      case 'projects-prev-page': refreshProjectsList(Math.max(0, state.projectsPage.number - 1)); break;
      case 'projects-next-page': refreshProjectsList(state.projectsPage.number + 1); break;

      case 'status-transition': changeTaskStatus(Number(id), status, false); break;
      case 'status-transition-modal': changeTaskStatus(Number(id), status, true); break;
      case 'open-task-detail': openTaskDetailModal(Number(id)); break;
      case 'delete-task': deleteTask(Number(id)); break;
      case 'assign-task': assignTask(Number(id)); break;
      case 'unassign-task': unassignTask(Number(id)); break;
      case 'move-task': moveTask(Number(id)); break;
      case 'attach-label': attachLabel(Number(id)); break;
      case 'detach-label': detachLabel(Number(taskid), Number(labelid)); break;
      case 'tasks-prev-page': refreshTasksList(Math.max(0, state.tasksPage.number - 1)); break;
      case 'tasks-next-page': refreshTasksList(state.tasksPage.number + 1); break;
      default: break;
    }
  });
}

/* ---------------------------------------------------------------- */
/* Bootstrap                                                           */
/* ---------------------------------------------------------------- */

document.addEventListener('DOMContentLoaded', async () => {
  ensureOverlayRoots();
  attachGlobalEvents();

  const page = getCurrentPage();

  if (page === 'login') {
    if (state.token) window.location.replace('/dashboard.html');
    return;
  }

  if (!state.token) {
    window.location.replace('/');
    return;
  }

  if (page === 'dashboard') await loadDashboardPage();
  else if (page === 'projects') await loadProjectsPage();
  else if (page === 'tasks') await loadTasksPage();
  else if (page === 'labels') await loadLabelsPage();
});
