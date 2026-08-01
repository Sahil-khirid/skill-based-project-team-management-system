import { Link } from 'react-router-dom';

export default function UnauthorizedPage() {
  return (
    <div className="text-center py-5">
      <h1 className="h3 mb-3">403 - Access denied</h1>
      <p className="text-muted mb-4">You do not have permission to view this page.</p>
      <Link className="btn btn-primary" to="/dashboard">
        Back to Dashboard
      </Link>
    </div>
  );
}
