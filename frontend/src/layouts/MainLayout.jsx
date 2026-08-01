import { Outlet } from 'react-router-dom';
import AppNavbar from '../components/AppNavbar';

export default function MainLayout() {
  return (
    <div className="d-flex flex-column min-vh-100">
      <AppNavbar />
      <main className="flex-grow-1">
        <div className="container pb-5">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
