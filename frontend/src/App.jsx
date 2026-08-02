import { Route, Routes } from 'react-router-dom';
import LoadingSpinner from './components/LoadingSpinner';
import { useAuth } from './auth/useAuth';
import ProtectedRoute from './auth/ProtectedRoute';
import MainLayout from './layouts/MainLayout';
import AvailabilityPage from './pages/availability/AvailabilityPage';
import DashboardPage from './pages/DashboardPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';
import ProfilePage from './pages/profile/ProfilePage';
import RegisterPage from './pages/RegisterPage';
import SkillsPage from './pages/skills/SkillsPage';
import MySkillsPage from './pages/userSkills/MySkillsPage';
import UnauthorizedPage from './pages/UnauthorizedPage';

export default function App() {
  const { loading } = useAuth();

  if (loading) {
    return <LoadingSpinner label="Checking your session..." />;
  }

  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/skills"
          element={
            <ProtectedRoute>
              <SkillsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my-skills"
          element={
            <ProtectedRoute>
              <MySkillsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/availability"
          element={
            <ProtectedRoute>
              <AvailabilityPage />
            </ProtectedRoute>
          }
        />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
