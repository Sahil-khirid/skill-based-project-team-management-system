import apiClient from './apiClient';

export function listTasks() {
  return apiClient.get('/api/v1/tasks').then((response) => response.data);
}

export function getTask(id) {
  return apiClient.get(`/api/v1/tasks/${id}`).then((response) => response.data);
}

export function createTask({ projectId, title, description, priority, dueDate }) {
  return apiClient
    .post('/api/v1/tasks', { projectId, title, description, priority, dueDate })
    .then((response) => response.data);
}

export function updateTask(id, { title, description, status, priority, dueDate }) {
  return apiClient
    .put(`/api/v1/tasks/${id}`, { title, description, status, priority, dueDate })
    .then((response) => response.data);
}

export function deleteTask(id) {
  return apiClient.delete(`/api/v1/tasks/${id}`).then((response) => response.data);
}

export function assignTask(id, { assignedAuthUserId }) {
  return apiClient.patch(`/api/v1/tasks/${id}/assign`, { assignedAuthUserId }).then((response) => response.data);
}

export function updateTaskStatus(id, { status }) {
  return apiClient.patch(`/api/v1/tasks/${id}/status`, { status }).then((response) => response.data);
}

export function updateTaskProgress(id, { progressPercentage }) {
  return apiClient
    .patch(`/api/v1/tasks/${id}/progress`, { progressPercentage })
    .then((response) => response.data);
}

export function getTaskProgressSummary(projectId) {
  return apiClient
    .get(`/api/v1/projects/${projectId}/task-progress-summary`)
    .then((response) => response.data);
}
