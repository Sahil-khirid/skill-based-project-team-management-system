import axios from 'axios';
import { AUTH_SESSION_EXPIRED_EVENT, clearAuthStorage, getAccessToken } from '../auth/tokenStorage';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const accessToken = getAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const requestUrl = error.config?.url || '';
    // A 401 from the login/register endpoints themselves just means "try again" -
    // there is no existing session to invalidate, so skip the session-expired flow.
    const isAuthEntryPoint = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/register');

    if (status === 401 && !isAuthEntryPoint) {
      clearAuthStorage();
      window.dispatchEvent(new Event(AUTH_SESSION_EXPIRED_EVENT));
    }

    return Promise.reject(error);
  },
);

export default apiClient;
