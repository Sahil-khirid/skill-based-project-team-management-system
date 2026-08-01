import apiClient from './apiClient';

export function register({ email, password }) {
  return apiClient.post('/api/v1/auth/register', { email, password }).then((response) => response.data);
}

export function login({ email, password }) {
  return apiClient.post('/api/v1/auth/login', { email, password }).then((response) => response.data);
}

export function logout(refreshToken) {
  return apiClient.post('/api/v1/auth/logout', { refreshToken }).then((response) => response.data);
}

export function getCurrentUser() {
  return apiClient.get('/api/v1/auth/me').then((response) => response.data);
}
