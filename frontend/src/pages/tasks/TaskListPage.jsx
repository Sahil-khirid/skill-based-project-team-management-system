import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as taskApi from '../../api/taskApi';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/ui/EmptyState';
import StatusBadge from '../../components/ui/StatusBadge';
import { useAuth } from '../../auth/useAuth';

export default function TaskListPage() {
  const { hasRole } = useAuth();
  const isManager = hasRole('PROJECT_MANAGER');

  const [tasks, setTasks] = useState([]);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  const loadTasks = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await taskApi.listTasks();
      setTasks(data);
    } catch (error) {
      setLoadError(extractErrorMessage(error));
    } finally {
      setInitialLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  if (initialLoading) {
    return <LoadingSpinner label="Loading tasks..." />;
  }

  if (loadError) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8">
          <div className="alert alert-danger" role="alert">
            {loadError}
          </div>
          <button type="button" className="btn btn-primary" onClick={loadTasks}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h1 className="h4 mb-0">Tasks</h1>
          {isManager && (
            <Link className="btn btn-primary" to="/tasks/create">
              Create Task
            </Link>
          )}
        </div>

        {tasks.length === 0 ? (
          <EmptyState
            title="No tasks yet"
            description={
              isManager ? 'Create a task to start assigning work.' : 'No tasks have been assigned to you yet.'
            }
            action={
              isManager && (
                <Link className="btn btn-primary" to="/tasks/create">
                  Create Task
                </Link>
              )
            }
          />
        ) : (
          <div className="table-responsive">
            <table className="table table-striped table-hover align-middle">
              <thead>
                <tr>
                  <th scope="col">Title</th>
                  <th scope="col">Status</th>
                  <th scope="col">Priority</th>
                  <th scope="col">Progress</th>
                  <th scope="col">Due Date</th>
                  <th scope="col" className="text-end">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {tasks.map((task) => (
                  <tr key={task.id}>
                    <td>
                      <Link to={`/tasks/${task.id}`}>{task.title}</Link>
                    </td>
                    <td>
                      <StatusBadge status={task.status} />
                    </td>
                    <td>
                      <StatusBadge status={task.priority} />
                    </td>
                    <td style={{ minWidth: '9rem' }}>
                      <div className="d-flex align-items-center gap-2">
                        <div className="progress flex-grow-1">
                          <div
                            className="progress-bar"
                            role="progressbar"
                            style={{ width: `${task.progressPercentage}%` }}
                            aria-valuenow={task.progressPercentage}
                            aria-valuemin="0"
                            aria-valuemax="100"
                          />
                        </div>
                        <span className="small text-muted">{task.progressPercentage}%</span>
                      </div>
                    </td>
                    <td>{task.dueDate || 'Not set'}</td>
                    <td className="text-end">
                      <Link
                        className="btn btn-outline-secondary btn-sm"
                        to={`/projects/${task.projectId}/task-progress-summary`}
                      >
                        Progress Summary
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
