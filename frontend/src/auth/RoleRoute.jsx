import { Navigate } from 'react-router-dom';
import LoadingSpinner from '../components/LoadingSpinner';
import { useAuth } from './useAuth';

export default function RoleRoute({ allowedRoles, children }) {
  const { isAuthenticated, hasRole, loading } = useAuth();

  if (loading) {
    return <LoadingSpinner />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!hasRole(...allowedRoles)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
}
