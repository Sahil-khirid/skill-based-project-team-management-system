import { useCallback, useEffect, useRef, useState } from 'react';
import { extractErrorMessage } from '../../api/errorMessage';
import * as userSkillApi from '../../api/userSkillApi';
import SkillForm from '../../components/userSkill/SkillForm';
import LoadingSpinner from '../../components/LoadingSpinner';
import { useAuth } from '../../auth/useAuth';

export default function SkillsPage() {
  const { hasRole } = useAuth();
  const isManager = hasRole('PROJECT_MANAGER');

  const [skills, setSkills] = useState([]);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [mode, setMode] = useState('list');
  const [selectedSkill, setSelectedSkill] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [disablingSkillId, setDisablingSkillId] = useState(null);
  const submissionLockRef = useRef(false);
  const disableLockRef = useRef(false);

  const loadSkills = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await userSkillApi.getSkills();
      setSkills(data);
    } catch (error) {
      setLoadError(extractErrorMessage(error));
    } finally {
      setInitialLoading(false);
    }
  }, []);

  useEffect(() => {
    loadSkills();
  }, [loadSkills]);

  function handleAddClick() {
    if (disableLockRef.current) {
      return;
    }
    setActionError('');
    setSuccessMessage('');
    setMode('create');
  }

  function handleEditClick(skill) {
    if (disableLockRef.current) {
      return;
    }
    setActionError('');
    setSuccessMessage('');
    setSelectedSkill(skill);
    setMode('edit');
  }

  function handleCancel() {
    setActionError('');
    setSelectedSkill(null);
    setMode('list');
  }

  async function handleCreate(values) {
    if (submissionLockRef.current) {
      return;
    }
    submissionLockRef.current = true;
    setSubmitting(true);
    setActionError('');
    setSuccessMessage('');
    try {
      await userSkillApi.createSkill(values);
      setMode('list');
      setSuccessMessage('Skill created successfully.');
      await loadSkills();
    } catch (error) {
      setActionError(extractErrorMessage(error));
    } finally {
      submissionLockRef.current = false;
      setSubmitting(false);
    }
  }

  async function handleUpdate(values) {
    if (submissionLockRef.current) {
      return;
    }
    submissionLockRef.current = true;
    setSubmitting(true);
    setActionError('');
    setSuccessMessage('');
    try {
      await userSkillApi.updateSkill(selectedSkill.id, values);
      setSelectedSkill(null);
      setMode('list');
      setSuccessMessage('Skill updated successfully.');
      await loadSkills();
    } catch (error) {
      setActionError(extractErrorMessage(error));
    } finally {
      submissionLockRef.current = false;
      setSubmitting(false);
    }
  }

  async function handleDisable(skill) {
    if (disableLockRef.current) {
      return;
    }
    if (!window.confirm(`Disable "${skill.name}"? It will no longer appear in the active skills catalog.`)) {
      return;
    }
    disableLockRef.current = true;
    setDisablingSkillId(skill.id);
    setActionError('');
    setSuccessMessage('');
    try {
      await userSkillApi.disableSkill(skill.id);
      setSkills((prev) => prev.filter((item) => item.id !== skill.id));
      setSuccessMessage('Skill disabled successfully.');
    } catch (error) {
      setActionError(extractErrorMessage(error));
    } finally {
      disableLockRef.current = false;
      setDisablingSkillId(null);
    }
  }

  if (initialLoading) {
    return <LoadingSpinner label="Loading skills..." />;
  }

  if (loadError) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8">
          <div className="alert alert-danger" role="alert">
            {loadError}
          </div>
          <button type="button" className="btn btn-primary" onClick={loadSkills}>
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
          <h1 className="h4 mb-0">Skills Catalog</h1>
          {isManager && mode === 'list' && skills.length > 0 && (
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleAddClick}
              disabled={disablingSkillId !== null}
            >
              Add Skill
            </button>
          )}
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

        {mode === 'create' && (
          <div className="card shadow-sm mb-4">
            <div className="card-body">
              <h2 className="h5 mb-4">Add Skill</h2>

              {actionError && (
                <div className="alert alert-danger" role="alert">
                  {actionError}
                </div>
              )}

              <SkillForm
                submitting={submitting}
                submitLabel="Create Skill"
                submittingLabel="Creating..."
                onSubmit={handleCreate}
                onCancel={handleCancel}
              />
            </div>
          </div>
        )}

        {mode === 'edit' && selectedSkill && (
          <div className="card shadow-sm mb-4">
            <div className="card-body">
              <h2 className="h5 mb-4">Edit Skill</h2>

              {actionError && (
                <div className="alert alert-danger" role="alert">
                  {actionError}
                </div>
              )}

              <SkillForm
                initialValues={selectedSkill}
                submitting={submitting}
                submitLabel="Save Changes"
                submittingLabel="Saving..."
                onSubmit={handleUpdate}
                onCancel={handleCancel}
              />
            </div>
          </div>
        )}

        {mode === 'list' && skills.length === 0 && (
          <div className="card shadow-sm">
            <div className="card-body text-center py-5">
              <p className="text-muted mb-3">No active skills are available yet.</p>
              {isManager && (
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleAddClick}
                  disabled={disablingSkillId !== null}
                >
                  Add Skill
                </button>
              )}
            </div>
          </div>
        )}

        {mode === 'list' && skills.length > 0 && (
          <div className="table-responsive">
            <table className="table table-striped align-middle">
              <thead>
                <tr>
                  <th scope="col">Name</th>
                  <th scope="col">Description</th>
                  {isManager && (
                    <th scope="col" className="text-end">
                      Actions
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {skills.map((skill) => (
                  <tr key={skill.id}>
                    <td>{skill.name}</td>
                    <td>{skill.description || 'Not provided'}</td>
                    {isManager && (
                      <td className="text-end">
                        <div className="d-flex gap-2 justify-content-end">
                          <button
                            type="button"
                            className="btn btn-outline-secondary btn-sm"
                            onClick={() => handleEditClick(skill)}
                            disabled={disablingSkillId !== null}
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            onClick={() => handleDisable(skill)}
                            disabled={disablingSkillId !== null}
                          >
                            {disablingSkillId === skill.id ? 'Disabling...' : 'Disable'}
                          </button>
                        </div>
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
