import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export default function AppNavbar() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/login', { replace: true });
  }

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
      <div className="container flex-wrap">
        <Link className="navbar-brand" to="/">
          Skill Team
        </Link>

        <ul className="navbar-nav flex-row gap-3 me-auto">
          <li className="nav-item">
            <Link className="nav-link" to="/">
              Home
            </Link>
          </li>
          {isAuthenticated && (
            <li className="nav-item">
              <Link className="nav-link" to="/dashboard">
                Dashboard
              </Link>
            </li>
          )}
          {isAuthenticated && (
            <li className="nav-item">
              <Link className="nav-link" to="/profile">
                Profile
              </Link>
            </li>
          )}
          {isAuthenticated && (
            <li className="nav-item">
              <Link className="nav-link" to="/skills">
                Skills
              </Link>
            </li>
          )}
        </ul>

        <ul className="navbar-nav flex-row gap-3 align-items-lg-center">
          {!isAuthenticated && (
            <>
              <li className="nav-item">
                <Link className="nav-link" to="/login">
                  Login
                </Link>
              </li>
              <li className="nav-item">
                <Link className="btn btn-outline-light btn-sm" to="/register">
                  Register
                </Link>
              </li>
            </>
          )}
          {isAuthenticated && (
            <>
              <li className="nav-item text-light small">
                {user?.email} ({user?.role})
              </li>
              <li className="nav-item">
                <button type="button" className="btn btn-outline-light btn-sm" onClick={handleLogout}>
                  Logout
                </button>
              </li>
            </>
          )}
        </ul>
      </div>
    </nav>
  );
}
