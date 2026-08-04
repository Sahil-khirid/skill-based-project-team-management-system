import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as projectApi from '../../api/projectApi';
import * as userSkillApi from '../../api/userSkillApi';
import ProjectRequiredSkillForm from '../../components/project/ProjectRequiredSkillForm';
import LoadingSpinner from '../../components/LoadingSpinner';
import { useAuth } from '../../auth/useAuth';

const PROFICIENCY_LABELS = {
  BEGINNER: 'Beginner',
  INTERMEDIATE: 'Intermediate',
  ADVANCED: 'Advanced',
  EXPERT: 'Expert',
};

export default function ProjectRequiredSkillsPage() {
  const { projectId } = useParams();
  const { hasRole } = useAuth();
  const isManager = hasRole('PROJECT_MANAGER');

  const [requiredSkills, setRequiredSkills] = useState([]);
  const [catalogSkills, setCatalogSkills] = useState([]);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [mode, setMode] = useState('list');
  const [submitting, setSubmitting] = useState(false);
  const [removingSkillId, setRemovingSkillId] = useState(null);
  const [actionError, setActionError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const submissionLockRef = useRef(false);
  const removeLockRef = useRef(false);

  const loadData = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const [requiredSkillsData, catalogData] = await Promise.all([
        projectApi.listProjectRequiredSkills(projectId),
        userSkillApi.getSkills(),
      ]);
      setRequiredSkills(requiredSkillsData);
      setCatalogSkills(catalogData);
    } catch (error) {
      setLoadError(extractErrorMessage(error));
    } finally {
      setInitialLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const skillNameById = useMemo(() => {
    const map = new Map();
    catalogSkills.forEach((skill) => map.set(skill.id, skill.name));
    return map;
  }, [catalogSkills]);

  const eligibleSkills = useMemo(
    () => catalogSkills.filter((skill) => !requiredSkills.some((requiredSkill) => requiredSkill.skillId === skill.id)),
    [catalogSkills, requiredSkills],
  );

  function handleAddClick() {
    if (submissionLockRef.current || removeLockRef.current) {
      return;
    }
    setActionError('');
    setSuccessMessage('');
    setMode('add');
  }

  function handleCancel() {
    setActionError('');
    setMode('list');
  }

  async function handleAddSubmit(values) {
    if (submissionLockRef.current) {
      return;
    }
    submissionLockRef.current = true;
    setSubmitting(true);
    setActionError('');
    setSuccessMessage('');
    try {
      await projectApi.addProjectRequiredSkill(projectId, values);
      setMode('list');
      setSuccessMessage('Required skill added successfully.');
      await loadData();
    } catch (error) {
      setActionError(extractErrorMessage(error));
    } finally {
      submissionLockRef.current = false;
      setSubmitting(false);
    }
  }

  async function handleRemove(requiredSkill) {
    if (removeLockRef.current || submissionLockRef.current) {
      return;
    }
    const skillLabel = skillNameById.get(requiredSkill.skillId) || `Skill ${requiredSkill.skillId}`;
    if (!window.confirm(`Remove "${skillLabel}" from this project's required skills?`)) {
      return;
    }
    removeLockRef.current = true;
    setRemovingSkillId(requiredSkill.skillId);
    setActionError('');
    setSuccessMessage('');
    try {
      await projectApi.removeProjectRequiredSkill(projectId, requiredSkill.skillId);
      setRequiredSkills((prev) => prev.filter((item) => item.skillId !== requiredSkill.skillId));
      setSuccessMessage('Required skill removed successfully.');
    } catch (error) {
      setActionError(extractErrorMessage(error));
    } finally {
      removeLockRef.current = false;
      setRemovingSkillId(null);
    }
  }

  if (initialLoading) {
    return <LoadingSpinner label="Loading required skills..." />;
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

  const actionsDisabled = removingSkillId !== null;

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h1 className="h4 mb-0">Required Skills</h1>
          <div className="d-flex gap-2">
            {isManager && mode === 'list' && eligibleSkills.length > 0 && (
              <button type="button" className="btn btn-primary" onClick={handleAddClick} disabled={actionsDisabled}>
                Add Required Skill
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
              <h2 className="h5 mb-4">Add Required Skill</h2>

              {actionError && (
                <div className="alert alert-danger" role="alert">
                  {actionError}
                </div>
              )}

              {eligibleSkills.length === 0 ? (
                <>
                  <p className="text-muted mb-3">All active catalog skills are already required for this project.</p>
                  <button type="button" className="btn btn-outline-secondary" onClick={handleCancel}>
                    Cancel
                  </button>
                </>
              ) : (
                <ProjectRequiredSkillForm
                  eligibleSkills={eligibleSkills}
                  submitting={submitting}
                  submitLabel="Add Required Skill"
                  submittingLabel="Adding..."
                  onSubmit={handleAddSubmit}
                  onCancel={handleCancel}
                />
              )}
            </div>
          </div>
        )}

        {mode === 'list' && requiredSkills.length === 0 && (
          <div className="card shadow-sm">
            <div className="card-body text-center py-5">
              <p className="text-muted mb-3">No required skills have been added to this project yet.</p>
              {isManager &&
                (eligibleSkills.length > 0 ? (
                  <button type="button" className="btn btn-primary" onClick={handleAddClick} disabled={actionsDisabled}>
                    Add Required Skill
                  </button>
                ) : (
                  <p className="text-muted mb-0">No active catalog skills are available to add.</p>
                ))}
            </div>
          </div>
        )}

        {mode === 'list' && requiredSkills.length > 0 && (
          <div className="table-responsive">
            <table className="table table-striped align-middle">
              <thead>
                <tr>
                  <th scope="col">Skill</th>
                  <th scope="col">Proficiency Level</th>
                  {isManager && (
                    <th scope="col" className="text-end">
                      Actions
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {requiredSkills.map((requiredSkill) => (
                  <tr key={requiredSkill.id}>
                    <td>{skillNameById.get(requiredSkill.skillId) || `Skill ${requiredSkill.skillId}`}</td>
                    <td>{PROFICIENCY_LABELS[requiredSkill.proficiencyLevel] || requiredSkill.proficiencyLevel}</td>
                    {isManager && (
                      <td className="text-end">
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => handleRemove(requiredSkill)}
                          disabled={actionsDisabled}
                        >
                          {removingSkillId === requiredSkill.skillId ? 'Removing...' : 'Remove'}
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
