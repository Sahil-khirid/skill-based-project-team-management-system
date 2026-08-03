import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as taskApi from '../../api/taskApi';
import LoadingSpinner from '../../components/LoadingSpinner';
import { useAuth } from '../../auth/useAuth';

const SUCCESS_REDIRECT_DELAY_MS = 1200;

const ALLOWED_STATUS_TRANSITIONS = {
  TODO: ['IN_PROGRESS', 'COMPLETED', 'BLOCKED'],
  IN_PROGRESS: ['COMPLETED', 'BLOCKED'],
  BLOCKED: ['IN_PROGRESS'],
  COMPLETED: [],
};

export default function TaskStatusPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, hasRole } = useAuth();
  const isManager = hasRole('PROJECT_MANAGER');

  const [task, setTask] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const submissionLockRef = useRef(false);
  const redirectTimeoutRef = useRef(null);

  const isAssignee = task != null && task.assignedAuthUserId != null && user?.id === task.assignedAuthUserId;
  const canUpdate = isManager || isAssignee;
  const isTerminal = task?.status === 'COMPLETED';
  const allowedTransitions = task ? ALLOWED_STATUS_TRANSITIONS[task.status] || [] : [];

  const loadTask = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await taskApi.getTask(id);
      setTask(data);
      setSelectedStatus(data.status === 'COMPLETED' ? data.status : '');
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

  function handleChange(event) {
    setSelectedStatus(event.target.value);
    if (fieldError) {
      setFieldError('');
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (isTerminal || submissionLockRef.current) {
      return;
    }

    if (!selectedStatus) {
      setFieldError('Status is required.');
      return;
    }

    submissionLockRef.current = true;
    setSubmitting(true);
    setActionError('');
    setSuccessMessage('');

    try {
      await taskApi.updateTaskStatus(id, { status: selectedStatus });
      setSuccessMessage('Task status updated successfully.');
      redirectTimeoutRef.current = setTimeout(() => {
        navigate(`/tasks/${id}`);
      }, SUCCESS_REDIRECT_DELAY_MS);
    } catch (error) {
      setActionError(extractErrorMessage(error));
      submissionLockRef.current = false;
      setSubmitting(false);
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

  if (!canUpdate) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8">
          <div className="alert alert-danger" role="alert">
            You do not have permission to update this task&apos;s status.
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <h1 className="h4 mb-4">Update Task Status</h1>

        {successMessage && (
          <div className="alert alert-success" role="alert">
            {successMessage}
          </div>
        )}

        {actionError && (
          <div className="alert alert-danger" role="alert">
            {actionError}
          </div>
        )}

        <div className="card shadow-sm">
          <div className="card-body">
            {isTerminal && (
              <div className="alert alert-info" role="alert">
                This task is COMPLETED. Status cannot be changed further.
              </div>
            )}

            <form onSubmit={handleSubmit} noValidate>
              <div className="mb-3">
                <label htmlFor="status" className="form-label">
                  Status
                </label>
                <select
                  id="status"
                  name="status"
                  className={`form-select${fieldError ? ' is-invalid' : ''}`}
                  value={selectedStatus}
                  onChange={handleChange}
                  disabled={submitting || isTerminal}
                  required
                >
                  {isTerminal ? (
                    <option value={task.status}>{task.status}</option>
                  ) : (
                    <>
                      <option value="">Select status</option>
                      {allowedTransitions.map((option) => (
                        <option key={option} value={option}>
                          {option}
                        </option>
                      ))}
                    </>
                  )}
                </select>
                {fieldError && <div className="invalid-feedback">{fieldError}</div>}
              </div>

              <div className="d-flex gap-2">
                <button type="submit" className="btn btn-primary" disabled={submitting || isTerminal}>
                  {submitting ? 'Updating...' : 'Update Status'}
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => navigate(`/tasks/${id}`)}
                  disabled={submitting}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
