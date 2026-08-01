import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="text-center py-5">
      <h1 className="h3 mb-3">404 - Page not found</h1>
      <p className="text-muted mb-4">The page you are looking for does not exist.</p>
      <Link className="btn btn-primary" to="/">
        Back to Home
      </Link>
    </div>
  );
}
