import apiClient from './apiClient';

export function listProjects() {
  return apiClient.get('/api/v1/projects').then((response) => response.data);
}

export function getProject(id) {
  return apiClient.get(`/api/v1/projects/${id}`).then((response) => response.data);
}

export function getProjectMembers(projectId) {
  return apiClient.get(`/api/v1/projects/${projectId}/members`).then((response) => response.data);
}
