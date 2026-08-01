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
