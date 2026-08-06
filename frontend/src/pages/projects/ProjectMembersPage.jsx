import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as projectApi from '../../api/projectApi';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/ui/EmptyState';
import RoleBadge from '../../components/ui/RoleBadge';
import { formatDateTime } from '../../utils/formatDate';
import { useAuth } from '../../auth/useAuth';

const ROLE_OPTIONS = ['MEMBER', 'LEADER'];

export default function ProjectMembersPage() {
  const { projectId } = useParams();
  const { hasRole } = useAuth();
  const isManager = hasRole('PROJECT_MANAGER');

  const [members, setMembers] = useState([]);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [mode, setMode] = useState('list');
  const [authUserId, setAuthUserId] = useState('');
  const [role, setRole] = useState('MEMBER');
  const [submitting, setSubmitting] = useState(false);
  const [removingAuthUserId, setRemovingAuthUserId] = useState(null);
  const [actionError, setActionError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const submissionLockRef = useRef(false);
  const removeLockRef = useRef(false);

  const loadMembers = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await projectApi.getProjectMembers(projectId);
      setMembers(data);
    } catch (error) {
      setLoadError(extractErrorMessage(error));
    } finally {
      setInitialLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadMembers();
  }, [loadMembers]);

  function handleAddClick() {
    if (submissionLockRef.current || removeLockRef.current) {
      return;
    }
    setActionError('');
    setSuccessMessage('');
    setAuthUserId('');
    setRole('MEMBER');
    setMode('add');
  }

  function handleCancel() {
    setActionError('');
    setMode('list');
  }

  async function handleAddSubmit(event) {
    event.preventDefault();
    if (submissionLockRef.current) {
      return;
    }
    if (!authUserId) {
      setActionError('A user ID is required.');
      return;
    }

    submissionLockRef.current = true;
    setSubmitting(true);
    setActionError('');
    setSuccessMessage('');
    try {
      await projectApi.addProjectMember(projectId, { authUserId: Number(authUserId), role });
      setMode('list');
      setSuccessMessage('Project member added successfully.');
      await loadMembers();
    } catch (error) {
      setActionError(extractErrorMessage(error));
    } finally {
      submissionLockRef.current = false;
      setSubmitting(false);
    }
  }

  async function handleRemove(member) {
    if (removeLockRef.current || submissionLockRef.current) {
      return;
    }
    if (!window.confirm(`Remove user #${member.authUserId} from this project?`)) {
      return;
    }
    removeLockRef.current = true;
    setRemovingAuthUserId(member.authUserId);
    setActionError('');
    setSuccessMessage('');
    try {
      await projectApi.removeProjectMember(projectId, member.authUserId);
      setMembers((prev) => prev.filter((item) => item.authUserId !== member.authUserId));
      setSuccessMessage('Project member removed successfully.');
    } catch (error) {
      setActionError(extractErrorMessage(error));
    } finally {
      removeLockRef.current = false;
      setRemovingAuthUserId(null);
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
          <button type="button" className="btn btn-primary" onClick={loadMembers}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  const actionsDisabled = removingAuthUserId !== null;

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h1 className="h4 mb-0">Project Members</h1>
          <div className="d-flex gap-2">
            {isManager && mode === 'list' && (
              <button type="button" className="btn btn-primary" onClick={handleAddClick} disabled={actionsDisabled}>
                Add Member
              </button>
            )}
            <Link className="btn btn-outline-secondary" to={`/projects/${projectId}`}>
              Back to Project
            </Link>
          </div>
        </div>

        {successMessage && mode === 'list' && (
          <div className="alert alert-success" role="alert">
            {successMessage}
          </div>
        )}

        {actionError && mode === 'list' && (
          <div className="alert alert-danger" role="alert">
            {actionError}
          </div>
        )}

        {mode === 'add' && (
          <div className="card shadow-sm mb-4">
            <div className="card-body">
              <h2 className="h5 mb-4">Add Project Member</h2>

              {actionError && (
                <div className="alert alert-danger" role="alert">
                  {actionError}
                </div>
              )}

              <form onSubmit={handleAddSubmit} noValidate>
                <div className="mb-3">
                  <label htmlFor="authUserId" className="form-label">
                    User ID
                  </label>
                  <input
                    id="authUserId"
                    name="authUserId"
                    type="number"
                    min="1"
                    className="form-control"
                    value={authUserId}
                    onChange={(event) => setAuthUserId(event.target.value)}
                    disabled={submitting}
                    required
                  />
                </div>

                <div className="mb-3">
                  <label htmlFor="role" className="form-label">
                    Role
                  </label>
                  <select
                    id="role"
                    name="role"
                    className="form-select"
                    value={role}
                    onChange={(event) => setRole(event.target.value)}
                    disabled={submitting}
                  >
                    {ROLE_OPTIONS.map((roleOption) => (
                      <option key={roleOption} value={roleOption}>
                        {roleOption}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="d-flex gap-2">
                  <button type="submit" className="btn btn-primary" disabled={submitting}>
                    {submitting ? 'Adding...' : 'Add Member'}
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={handleCancel}
                    disabled={submitting}
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {mode === 'list' && members.length === 0 && (
          <EmptyState
            title="No members yet"
            description={
              isManager
                ? 'Add members to this project, or check Member Recommendations for eligible candidates.'
                : 'This project has no members yet.'
            }
            action={
              isManager && (
                <button type="button" className="btn btn-primary" onClick={handleAddClick} disabled={actionsDisabled}>
                  Add Member
                </button>
              )
            }
          />
        )}

        {mode === 'list' && members.length > 0 && (
          <div className="table-responsive">
            <table className="table table-striped table-hover align-middle">
              <thead>
                <tr>
                  <th scope="col">User ID</th>
                  <th scope="col">Role</th>
                  <th scope="col">Added On</th>
                  <th scope="col">Updated On</th>
                  {isManager && (
                    <th scope="col" className="text-end">
                      Actions
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {members.map((member) => (
                  <tr key={member.id}>
                    <td>{member.authUserId}</td>
                    <td>
                      <RoleBadge role={member.role} />
                    </td>
                    <td>{formatDateTime(member.createdAt)}</td>
                    <td>{formatDateTime(member.updatedAt)}</td>
                    {isManager && (
                      <td className="text-end">
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => handleRemove(member)}
                          disabled={actionsDisabled}
                        >
                          {removingAuthUserId === member.authUserId ? 'Removing...' : 'Remove'}
                        </button>
                      </td>
                    )}
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
