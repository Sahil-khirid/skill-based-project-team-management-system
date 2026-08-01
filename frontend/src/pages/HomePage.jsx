import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export default function HomePage() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="py-5 text-center">
      <h1 className="mb-3">Skill-Based Project Team Management System</h1>
      <p className="lead text-muted mb-4">
        Form project teams based on member skills, assign tasks, and track progress in one place.
      </p>
      {isAuthenticated ? (
        <Link className="btn btn-primary btn-lg" to="/dashboard">
          Go to Dashboard
        </Link>
      ) : (
        <div className="d-flex justify-content-center gap-3">
          <Link className="btn btn-primary btn-lg" to="/login">
            Login
          </Link>
          <Link className="btn btn-outline-primary btn-lg" to="/register">
            Register
          </Link>
        </div>
      )}
    </div>
  );
}
