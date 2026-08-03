import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as projectApi from '../../api/projectApi';
import * as taskApi from '../../api/taskApi';
import LoadingSpinner from '../../components/LoadingSpinner';

const SUCCESS_REDIRECT_DELAY_MS = 1200;

export default function TaskAssignmentPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [members, setMembers] = useState([]);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [selectedAuthUserId, setSelectedAuthUserId] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const submissionLockRef = useRef(false);
  const redirectTimeoutRef = useRef(null);

  const loadData = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const task = await taskApi.getTask(id);
      const memberList = await projectApi.getProjectMembers(task.projectId);
      setMembers(memberList);
    } catch (error) {
      setLoadError(extractErrorMessage(error));
    } finally {
      setInitialLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(
    () => () => {
      if (redirectTimeoutRef.current) {
        clearTimeout(redirectTimeoutRef.current);
      }
    },
    [],
  );

  function handleChange(event) {
    setSelectedAuthUserId(event.target.value);
    if (fieldError) {
      setFieldError('');
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submissionLockRef.current) {
      return;
    }

    if (!selectedAuthUserId) {
      setFieldError('A member selection is required.');
      return;
    }

    submissionLockRef.current = true;
    setSubmitting(true);
    setActionError('');
    setSuccessMessage('');

    try {
      await taskApi.assignTask(id, { assignedAuthUserId: Number(selectedAuthUserId) });
      setSuccessMessage('Task assigned successfully.');
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
    return <LoadingSpinner label="Loading project members..." />;
  }

  if (loadError) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8">
          <div className="alert alert-danger" role="alert">
            {loadError}
          </div>
          <button type="button" className="btn btn-primary" onClick={loadData}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <h1 className="h4 mb-4">Assign Task</h1>

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
            {members.length === 0 ? (
              <p className="text-muted mb-0">This project has no members to assign.</p>
            ) : (
              <form onSubmit={handleSubmit} noValidate>
                <div className="mb-3">
                  <label htmlFor="assignedAuthUserId" className="form-label">
                    Project Member
                  </label>
                  <select
                    id="assignedAuthUserId"
                    name="assignedAuthUserId"
                    className={`form-select${fieldError ? ' is-invalid' : ''}`}
                    value={selectedAuthUserId}
                    onChange={handleChange}
                    disabled={submitting}
                    required
                  >
                    <option value="">Select a member</option>
                    {members.map((member) => (
                      <option key={member.authUserId} value={member.authUserId}>
                        {member.authUserId}
                      </option>
                    ))}
                  </select>
                  {fieldError && <div className="invalid-feedback">{fieldError}</div>}
                </div>

                <div className="d-flex gap-2">
                  <button type="submit" className="btn btn-primary" disabled={submitting}>
                    {submitting ? 'Assigning...' : 'Assign Task'}
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
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
