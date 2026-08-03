import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as taskApi from '../../api/taskApi';
import LoadingSpinner from '../../components/LoadingSpinner';
import { useAuth } from '../../auth/useAuth';

const SUCCESS_REDIRECT_DELAY_MS = 1200;

export default function TaskDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, hasRole } = useAuth();
  const isManager = hasRole('PROJECT_MANAGER');

  const [task, setTask] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const [deleteSuccess, setDeleteSuccess] = useState('');
  const deleteLockRef = useRef(false);
  const redirectTimeoutRef = useRef(null);

  const loadTask = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await taskApi.getTask(id);
      setTask(data);
    } catch (error) {
      setLoadError(extractErrorMessage(error));
    } finally {
      setInitialLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadTask();
  }, [loadTask]);

  useEffect(
    () => () => {
      if (redirectTimeoutRef.current) {
        clearTimeout(redirectTimeoutRef.current);
      }
    },
    [],
  );

  async function handleDelete() {
    if (deleteLockRef.current) {
      return;
    }
    if (!window.confirm(`Delete "${task.title}"? This action cannot be undone.`)) {
      return;
    }
    deleteLockRef.current = true;
    setDeleting(true);
    setDeleteError('');
    setDeleteSuccess('');
    try {
      await taskApi.deleteTask(id);
      setDeleteSuccess('Task deleted successfully.');
      redirectTimeoutRef.current = setTimeout(() => {
        navigate('/tasks');
      }, SUCCESS_REDIRECT_DELAY_MS);
    } catch (error) {
      setDeleteError(extractErrorMessage(error));
      deleteLockRef.current = false;
      setDeleting(false);
    }
  }

  if (initialLoading) {
    return <LoadingSpinner label="Loading task..." />;
  }

  if (loadError) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8">
          <div className="alert alert-danger" role="alert">
            {loadError}
          </div>
          <button type="button" className="btn btn-primary" onClick={loadTask}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  const isAssignee = task.assignedAuthUserId != null && user?.id === task.assignedAuthUserId;
  const canUpdateStatus = isManager || isAssignee;
  const canUpdateProgress = canUpdateStatus && (task.status === 'IN_PROGRESS' || task.status === 'BLOCKED');

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h1 className="h4 mb-0">Task Details</h1>
          {canUpdateStatus && (
            <div className="d-flex gap-2">
              {isManager && (
                <>
                  <Link className="btn btn-primary" to={`/tasks/${id}/edit`}>
                    Edit Task
                  </Link>
                  <Link className="btn btn-outline-primary" to={`/tasks/${id}/assign`}>
                    Assign Task
                  </Link>
                </>
              )}
              <Link className="btn btn-outline-primary" to={`/tasks/${id}/status`}>
                Update Status
              </Link>
              {canUpdateProgress && (
                <Link className="btn btn-outline-primary" to={`/tasks/${id}/progress`}>
                  Update Progress
                </Link>
              )}
              {isManager && (
                <button type="button" className="btn btn-outline-danger" onClick={handleDelete} disabled={deleting}>
                  {deleting ? 'Deleting...' : 'Delete Task'}
                </button>
              )}
            </div>
          )}
        </div>

        {deleteSuccess && (
          <div className="alert alert-success" role="alert">
            {deleteSuccess}
          </div>
        )}

        {deleteError && (
          <div className="alert alert-danger" role="alert">
            {deleteError}
          </div>
        )}

        <div className="card shadow-sm">
          <div className="card-body">
            <h2 className="h5 mb-3">{task.title}</h2>

            <dl className="row mb-0">
              <dt className="col-sm-4">Description</dt>
              <dd className="col-sm-8">{task.description || 'Not provided'}</dd>

              <dt className="col-sm-4">Status</dt>
              <dd className="col-sm-8">{task.status}</dd>

              <dt className="col-sm-4">Priority</dt>
              <dd className="col-sm-8">{task.priority}</dd>

              <dt className="col-sm-4">Progress Percentage</dt>
              <dd className="col-sm-8">{task.progressPercentage}%</dd>

              <dt className="col-sm-4">Due Date</dt>
              <dd className="col-sm-8">{task.dueDate || 'Not set'}</dd>

              <dt className="col-sm-4">Project ID</dt>
              <dd className="col-sm-8">{task.projectId}</dd>

              <dt className="col-sm-4">Assigned Auth User ID</dt>
              <dd className="col-sm-8">{task.assignedAuthUserId == null ? 'Unassigned' : task.assignedAuthUserId}</dd>

              <dt className="col-sm-4">Created At</dt>
              <dd className="col-sm-8">{task.createdAt}</dd>

              <dt className="col-sm-4">Updated At</dt>
              <dd className="col-sm-8">{task.updatedAt}</dd>
            </dl>
          </div>
        </div>
      </div>
    </div>
  );
}
