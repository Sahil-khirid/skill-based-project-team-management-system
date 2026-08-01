import { useAuth } from '../auth/useAuth';

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="py-4">
      <h1 className="h3 mb-4">Dashboard</h1>

      <div className="card mb-4">
        <div className="card-body">
          <h2 className="h5 card-title">Your account</h2>
          <dl className="row mb-0">
            <dt className="col-sm-3">User ID</dt>
            <dd className="col-sm-9">{user?.id}</dd>

            <dt className="col-sm-3">Email</dt>
            <dd className="col-sm-9">{user?.email}</dd>

            <dt className="col-sm-3">Role</dt>
            <dd className="col-sm-9">{user?.role}</dd>
          </dl>
        </div>
      </div>

      <div className="alert alert-info mb-0" role="alert">
        Project, skill, team, and task management modules will be integrated here by the rest of the team.
      </div>
    </div>
  );
}
