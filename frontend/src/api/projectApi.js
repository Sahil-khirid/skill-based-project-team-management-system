import apiClient from './apiClient';

export function getProjectMembers(projectId) {
  return apiClient.get(`/api/v1/projects/${projectId}/members`).then((response) => response.data);
}
