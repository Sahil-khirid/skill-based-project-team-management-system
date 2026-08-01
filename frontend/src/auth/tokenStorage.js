const STORAGE_KEYS = {
  ACCESS_TOKEN: 'skillteam.accessToken',
  REFRESH_TOKEN: 'skillteam.refreshToken',
};

export const AUTH_SESSION_EXPIRED_EVENT = 'auth:session-expired';

export function getAccessToken() {
  return localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
}

export function getRefreshToken() {
  return localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
}

export function setAuthStorage({ accessToken, refreshToken }) {
  localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
  if (refreshToken) {
    localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
  }
}

export function clearAuthStorage() {
  localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
  localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
}
