import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import RoleBadge from './ui/RoleBadge';

const USER_LINKS = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/profile', label: 'Profile' },
  { to: '/projects', label: 'Projects' },
  { to: '/skills', label: 'Skills' },
  { to: '/my-skills', label: 'My Skills' },
  { to: '/availability', label: 'Availability' },
  { to: '/tasks', label: 'Tasks' },
];

function navLinkClassName({ isActive }) {
  return isActive ? 'nav-link active' : 'nav-link';
}

export default function AppNavbar() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const [expanded, setExpanded] = useState(false);

  function closeMenu() {
    setExpanded(false);
  }

  async function handleLogout() {
    closeMenu();
    await logout();
    navigate('/login', { replace: true });
  }

  return (
    <nav className="navbar navbar-expand-lg app-navbar" aria-label="Main navigation">
      <div className="container">
        <Link className="navbar-brand app-navbar__brand" to="/" onClick={closeMenu}>
          <span className="app-navbar__logo" aria-hidden="true">
            ST
          </span>
          Skill Team
        </Link>

        <button
          type="button"
          className="navbar-toggler"
          aria-controls="appNavbarContent"
          aria-expanded={expanded}
          aria-label="Toggle navigation"
          onClick={() => setExpanded((prev) => !prev)}
        >
          <span className="navbar-toggler-icon" />
        </button>

        <div className={`collapse navbar-collapse${expanded ? ' show' : ''}`} id="appNavbarContent">
          <ul className="navbar-nav me-auto app-navbar__links">
            <li className="nav-item">
              <NavLink className={navLinkClassName} to="/" end onClick={closeMenu}>
                Home
              </NavLink>
            </li>
            {isAuthenticated &&
              USER_LINKS.map((link) => (
                <li className="nav-item" key={link.to}>
                  <NavLink className={navLinkClassName} to={link.to} onClick={closeMenu}>
                    {link.label}
                  </NavLink>
                </li>
              ))}
          </ul>

          <div className="app-navbar__actions">
            {!isAuthenticated && (
              <div className="d-flex align-items-center gap-2">
                <NavLink className={navLinkClassName} to="/login" onClick={closeMenu}>
                  Login
                </NavLink>
                <Link className="btn btn-light btn-sm" to="/register" onClick={closeMenu}>
                  Register
                </Link>
              </div>
            )}
            {isAuthenticated && (
              <div className="app-navbar__user">
                <span className="app-navbar__email" title={user?.email}>
                  {user?.email}
                </span>
                <RoleBadge role={user?.role} />
                <button type="button" className="btn btn-outline-light btn-sm" onClick={handleLogout}>
                  Logout
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
