import apiClient from './apiClient';

export function listProjects() {
  return apiClient.get('/api/v1/projects').then((response) => response.data);
}

export function getProject(id) {
  return apiClient.get(`/api/v1/projects/${id}`).then((response) => response.data);
}

export function createProject({ name, description }) {
  return apiClient.post('/api/v1/projects', { name, description }).then((response) => response.data);
}

export function updateProject(id, { name, description, status }) {
  return apiClient.put(`/api/v1/projects/${id}`, { name, description, status }).then((response) => response.data);
}

export function deleteProject(id) {
  return apiClient.delete(`/api/v1/projects/${id}`).then((response) => response.data);
}

export function getProjectMembers(projectId) {
  return apiClient.get(`/api/v1/projects/${projectId}/members`).then((response) => response.data);
}

export function listProjectRequiredSkills(projectId) {
  return apiClient.get(`/api/v1/projects/${projectId}/required-skills`).then((response) => response.data);
}

export function addProjectRequiredSkill(projectId, { skillId, proficiencyLevel }) {
  return apiClient
    .post(`/api/v1/projects/${projectId}/required-skills`, { skillId, proficiencyLevel })
    .then((response) => response.data);
}

export function removeProjectRequiredSkill(projectId, skillId) {
  return apiClient
    .delete(`/api/v1/projects/${projectId}/required-skills/${skillId}`)
    .then((response) => response.data);
}
