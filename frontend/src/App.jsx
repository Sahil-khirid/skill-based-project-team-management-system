import { Route, Routes } from 'react-router-dom';
import LoadingSpinner from './components/LoadingSpinner';
import { useAuth } from './auth/useAuth';
import ProtectedRoute from './auth/ProtectedRoute';
import RoleRoute from './auth/RoleRoute';
import MainLayout from './layouts/MainLayout';
import AvailabilityPage from './pages/availability/AvailabilityPage';
import DashboardPage from './pages/DashboardPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';
import ProfilePage from './pages/profile/ProfilePage';
import RegisterPage from './pages/RegisterPage';
import SkillsPage from './pages/skills/SkillsPage';
import CreateTaskPage from './pages/tasks/CreateTaskPage';
import EditTaskPage from './pages/tasks/EditTaskPage';
import ProjectTaskProgressSummaryPage from './pages/tasks/ProjectTaskProgressSummaryPage';
import TaskAssignmentPage from './pages/tasks/TaskAssignmentPage';
import TaskDetailsPage from './pages/tasks/TaskDetailsPage';
import TaskListPage from './pages/tasks/TaskListPage';
import TaskProgressPage from './pages/tasks/TaskProgressPage';
import TaskStatusPage from './pages/tasks/TaskStatusPage';
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
        <Route
          path="/tasks"
          element={
            <ProtectedRoute>
              <TaskListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/tasks/create"
          element={
            <RoleRoute allowedRoles={['PROJECT_MANAGER']}>
              <CreateTaskPage />
            </RoleRoute>
          }
        />
        <Route
          path="/tasks/:id"
          element={
            <ProtectedRoute>
              <TaskDetailsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/tasks/:id/edit"
          element={
            <RoleRoute allowedRoles={['PROJECT_MANAGER']}>
              <EditTaskPage />
            </RoleRoute>
          }
        />
        <Route
          path="/tasks/:id/assign"
          element={
            <RoleRoute allowedRoles={['PROJECT_MANAGER']}>
              <TaskAssignmentPage />
            </RoleRoute>
          }
        />
        <Route
          path="/tasks/:id/status"
          element={
            <ProtectedRoute>
              <TaskStatusPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/tasks/:id/progress"
          element={
            <ProtectedRoute>
              <TaskProgressPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/projects/:projectId/task-progress-summary"
          element={
            <ProtectedRoute>
              <ProjectTaskProgressSummaryPage />
            </ProtectedRoute>
          }
        />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
