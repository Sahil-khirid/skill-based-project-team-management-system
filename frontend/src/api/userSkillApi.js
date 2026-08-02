import apiClient from './apiClient';

export function getMyProfile() {
  return apiClient.get('/api/v1/users/me').then((response) => response.data);
}

export function createMyProfile({ displayName, headline, bio }) {
  return apiClient.post('/api/v1/users/me', { displayName, headline, bio }).then((response) => response.data);
}

export function updateMyProfile({ displayName, headline, bio }) {
  return apiClient.put('/api/v1/users/me', { displayName, headline, bio }).then((response) => response.data);
}

export function getSkills() {
  return apiClient.get('/api/v1/skills').then((response) => response.data);
}

export function createSkill({ name, description }) {
  return apiClient.post('/api/v1/skills', { name, description }).then((response) => response.data);
}

export function updateSkill(id, { name, description }) {
  return apiClient.put(`/api/v1/skills/${id}`, { name, description }).then((response) => response.data);
}

export function disableSkill(id) {
  return apiClient.delete(`/api/v1/skills/${id}`).then((response) => response.data);
}

export function getMySkills() {
  return apiClient.get('/api/v1/users/me/skills').then((response) => response.data);
}

export function addMySkill({ skillId, proficiencyLevel }) {
  return apiClient.post('/api/v1/users/me/skills', { skillId, proficiencyLevel }).then((response) => response.data);
}

export function updateMySkillProficiency(skillId, { proficiencyLevel }) {
  return apiClient
    .put(`/api/v1/users/me/skills/${skillId}`, { proficiencyLevel })
    .then((response) => response.data);
}

export function removeMySkill(skillId) {
  return apiClient.delete(`/api/v1/users/me/skills/${skillId}`).then((response) => response.data);
}

export function getMyAvailability() {
  return apiClient.get('/api/v1/users/me/availability').then((response) => response.data);
}

export function createMyAvailability({ availableHoursPerWeek }) {
  return apiClient
    .post('/api/v1/users/me/availability', { availableHoursPerWeek })
    .then((response) => response.data);
}

export function updateMyAvailability({ availableHoursPerWeek }) {
  return apiClient
    .put('/api/v1/users/me/availability', { availableHoursPerWeek })
    .then((response) => response.data);
}
