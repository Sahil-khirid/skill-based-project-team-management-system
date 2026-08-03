import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as taskApi from '../../api/taskApi';
import LoadingSpinner from '../../components/LoadingSpinner';
import { useAuth } from '../../auth/useAuth';

const SUCCESS_REDIRECT_DELAY_MS = 1200;
const PROGRESS_ALLOWED_STATUSES = ['IN_PROGRESS', 'BLOCKED'];

function validate(value) {
  if (value === '') {
    return 'Progress percentage is required.';
  }

  const numberValue = Number(value);

  if (!Number.isInteger(numberValue)) {
    return 'Progress percentage must be a whole number.';
  }

  if (numberValue < 0 || numberValue > 100) {
    return 'Progress percentage must be between 0 and 100.';
  }

  return '';
}

export default function TaskProgressPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, hasRole } = useAuth();
  const isManager = hasRole('PROJECT_MANAGER');

  const [task, setTask] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [progressInput, setProgressInput] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const submissionLockRef = useRef(false);
  const redirectTimeoutRef = useRef(null);

  const isAssignee = task != null && task.assignedAuthUserId != null && user?.id === task.assignedAuthUserId;
  const canUpdate = isManager || isAssignee;
  const statusAllowsProgress = task != null && PROGRESS_ALLOWED_STATUSES.includes(task.status);

  const loadTask = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await taskApi.getTask(id);
      setTask(data);
      setProgressInput(String(data.progressPercentage));
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
    setProgressInput(event.target.value);
    if (fieldError) {
      setFieldError('');
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submissionLockRef.current) {
      return;
    }

    const error = validate(progressInput);
    if (error) {
      setFieldError(error);
      return;
    }

    submissionLockRef.current = true;
    setSubmitting(true);
    setActionError('');
    setSuccessMessage('');

    try {
      await taskApi.updateTaskProgress(id, { progressPercentage: Number(progressInput) });
      setSuccessMessage('Task progress updated successfully.');
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
            You do not have permission to update this task&apos;s progress.
          </div>
        </div>
      </div>
    );
  }

  if (!statusAllowsProgress) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8">
          <div className="alert alert-info" role="alert">
            Progress cannot be updated while this task&apos;s status is {task.status}.
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <h1 className="h4 mb-4">Update Task Progress</h1>

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
            <form onSubmit={handleSubmit} noValidate>
              <div className="mb-3">
                <label htmlFor="progressPercentage" className="form-label">
                  Progress Percentage
                </label>
                <input
                  id="progressPercentage"
                  name="progressPercentage"
                  type="number"
                  min="0"
                  max="100"
                  step="1"
                  className={`form-control${fieldError ? ' is-invalid' : ''}`}
                  value={progressInput}
                  onChange={handleChange}
                  disabled={submitting}
                  required
                />
                {fieldError && <div className="invalid-feedback">{fieldError}</div>}
              </div>

              <div className="d-flex gap-2">
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Updating...' : 'Update Progress'}
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
