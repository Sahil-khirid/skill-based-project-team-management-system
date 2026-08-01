import { useCallback, useEffect, useMemo, useState } from 'react';
import * as authApi from '../api/authApi';
import { extractErrorMessage } from '../api/errorMessage';
import { AuthContext } from './authContextBase';
import {
  AUTH_SESSION_EXPIRED_EVENT,
  clearAuthStorage,
  getAccessToken,
  getRefreshToken,
  setAuthStorage,
} from './tokenStorage';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refreshCurrentUser = useCallback(async () => {
    if (!getAccessToken()) {
      setUser(null);
      return null;
    }
    try {
      const currentUser = await authApi.getCurrentUser();
      setUser(currentUser);
      return currentUser;
    } catch {
      // Covers an expired/invalid stored token: the response interceptor already
      // clears storage on 401, this just makes sure local state matches it.
      clearAuthStorage();
      setUser(null);
      return null;
    }
  }, []);

  useEffect(() => {
    refreshCurrentUser().finally(() => setLoading(false));
  }, [refreshCurrentUser]);

  useEffect(() => {
    function handleSessionExpired() {
      setUser(null);
    }
    window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, handleSessionExpired);
    return () => window.removeEventListener(AUTH_SESSION_EXPIRED_EVENT, handleSessionExpired);
  }, []);

  const login = useCallback(async ({ email, password }) => {
    const response = await authApi.login({ email, password });
    setAuthStorage({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
    });
    setUser(response.user);
    return response.user;
  }, []);

  const register = useCallback(({ email, password }) => authApi.register({ email, password }), []);

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    try {
      if (refreshToken) {
        await authApi.logout(refreshToken);
      }
    } catch (error) {
      console.warn('Logout request failed; clearing local session anyway:', extractErrorMessage(error));
    } finally {
      clearAuthStorage();
      setUser(null);
    }
  }, []);

  const hasRole = useCallback((...roles) => !!user && roles.includes(user.role), [user]);

  const value = useMemo(
    () => ({
      user,
      loading,
      isAuthenticated: !!user,
      login,
      register,
      logout,
      refreshCurrentUser,
      hasRole,
    }),
    [user, loading, login, register, logout, refreshCurrentUser, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
