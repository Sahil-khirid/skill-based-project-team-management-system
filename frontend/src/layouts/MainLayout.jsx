import { Outlet } from 'react-router-dom';
import AppNavbar from '../components/AppNavbar';

export default function MainLayout() {
  return (
    <div className="d-flex flex-column min-vh-100 app-shell">
      <AppNavbar />
      <main className="flex-grow-1 app-main">
        <div className="container app-content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
