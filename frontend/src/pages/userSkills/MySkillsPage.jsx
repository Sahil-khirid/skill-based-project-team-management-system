import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as userSkillApi from '../../api/userSkillApi';
import UserSkillForm from '../../components/userSkill/UserSkillForm';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/ui/EmptyState';
import ProficiencyBadge from '../../components/ui/ProficiencyBadge';

const PROFILE_MISSING_MESSAGE = 'No profile exists for this user.';

export default function MySkillsPage() {
  const [mySkills, setMySkills] = useState([]);
  const [catalogSkills, setCatalogSkills] = useState([]);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [mode, setMode] = useState('list');
  const [selectedUserSkill, setSelectedUserSkill] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [removingSkillId, setRemovingSkillId] = useState(null);
  const [actionError, setActionError] = useState('');
  const [profileMissing, setProfileMissing] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const submissionLockRef = useRef(false);
  const removeLockRef = useRef(false);

  const loadData = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const [mySkillsData, catalogData] = await Promise.all([userSkillApi.getMySkills(), userSkillApi.getSkills()]);
      setMySkills(mySkillsData);
      setCatalogSkills(catalogData);
    } catch (error) {
      setLoadError(extractErrorMessage(error));
    } finally {
      setInitialLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const eligibleSkills = useMemo(
    () => catalogSkills.filter((skill) => !mySkills.some((userSkill) => userSkill.skillId === skill.id)),
    [catalogSkills, mySkills],
  );

  function handleAddClick() {
    if (submissionLockRef.current || removeLockRef.current) {
      return;
    }
    setActionError('');
    setProfileMissing(false);
    setSuccessMessage('');
    setMode('add');
  }

  function handleEditClick(userSkill) {
    if (submissionLockRef.current || removeLockRef.current) {
      return;
    }
    setActionError('');
    setProfileMissing(false);
    setSuccessMessage('');
    setSelectedUserSkill(userSkill);
    setMode('edit');
  }

  function handleCancel() {
    setActionError('');
    setProfileMissing(false);
    setSelectedUserSkill(null);
    setMode('list');
  }

  async function handleAddSubmit(values) {
    if (submissionLockRef.current) {
      return;
    }
    submissionLockRef.current = true;
    setSubmitting(true);
    setActionError('');
    setProfileMissing(false);
    setSuccessMessage('');
    try {
      await userSkillApi.addMySkill(values);
      setMode('list');
      setSuccessMessage('Skill added to your profile successfully.');
      await loadData();
    } catch (error) {
      const message = extractErrorMessage(error);
      setActionError(message);
      setProfileMissing(error.response?.status === 404 && message === PROFILE_MISSING_MESSAGE);
    } finally {
      submissionLockRef.current = false;
      setSubmitting(false);
    }
  }

  async function handleUpdateSubmit(values) {
    if (submissionLockRef.current) {
      return;
    }
    submissionLockRef.current = true;
    setSubmitting(true);
    setActionError('');
    setSuccessMessage('');
    try {
      await userSkillApi.updateMySkillProficiency(selectedUserSkill.skillId, values);
      setSelectedUserSkill(null);
      setMode('list');
      setSuccessMessage('Skill proficiency updated successfully.');
      await loadData();
    } catch (error) {
      setActionError(extractErrorMessage(error));
    } finally {
      submissionLockRef.current = false;
      setSubmitting(false);
    }
  }

  async function handleRemove(userSkill) {
    if (removeLockRef.current || submissionLockRef.current) {
      return;
    }
    if (!window.confirm(`Remove "${userSkill.skillName}" from your profile?`)) {
      return;
    }
    removeLockRef.current = true;
    setRemovingSkillId(userSkill.skillId);
    setActionError('');
    setSuccessMessage('');
    try {
      await userSkillApi.removeMySkill(userSkill.skillId);
      setMySkills((prev) => prev.filter((item) => item.skillId !== userSkill.skillId));
      setSuccessMessage('Skill removed from your profile successfully.');
    } catch (error) {
      setActionError(extractErrorMessage(error));
    } finally {
      removeLockRef.current = false;
      setRemovingSkillId(null);
    }
  }

  if (initialLoading) {
    return <LoadingSpinner label="Loading your skills..." />;
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
          <h1 className="h4 mb-0">My Skills</h1>
          {mode === 'list' && mySkills.length > 0 && eligibleSkills.length > 0 && (
            <button type="button" className="btn btn-primary" onClick={handleAddClick} disabled={actionsDisabled}>
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

        {mode === 'add' && (
          <div className="card shadow-sm mb-4">
            <div className="card-body">
              <h2 className="h5 mb-4">Add Skill</h2>

              {actionError && (
                <div className="alert alert-danger" role="alert">
                  {actionError}
                  {profileMissing && (
                    <div className="mt-2">
                      <Link to="/profile">Create your profile</Link> before adding skills.
                    </div>
                  )}
                </div>
              )}

              {eligibleSkills.length === 0 ? (
                <>
                  <p className="text-muted mb-3">
                    All active catalog skills are already associated with your profile.
                  </p>
                  <button type="button" className="btn btn-outline-secondary" onClick={handleCancel}>
                    Cancel
                  </button>
                </>
              ) : (
                <UserSkillForm
                  mode="add"
                  eligibleSkills={eligibleSkills}
                  submitting={submitting}
                  submitLabel="Add Skill"
                  submittingLabel="Adding..."
                  onSubmit={handleAddSubmit}
                  onCancel={handleCancel}
                />
              )}
            </div>
          </div>
        )}

        {mode === 'edit' && selectedUserSkill && (
          <div className="card shadow-sm mb-4">
            <div className="card-body">
              <h2 className="h5 mb-4">Update Proficiency</h2>

              {actionError && (
                <div className="alert alert-danger" role="alert">
                  {actionError}
                </div>
              )}

              <UserSkillForm
                mode="edit"
                skillName={selectedUserSkill.skillName}
                initialProficiency={selectedUserSkill.proficiencyLevel}
                submitting={submitting}
                submitLabel="Save Changes"
                submittingLabel="Saving..."
                onSubmit={handleUpdateSubmit}
                onCancel={handleCancel}
              />
            </div>
          </div>
        )}

        {mode === 'list' && mySkills.length === 0 && (
          <EmptyState
            title="No skills added yet"
            description={
              eligibleSkills.length > 0
                ? 'Add skills to your profile so managers can find you for matching projects.'
                : 'No active catalog skills are available to add.'
            }
            action={
              eligibleSkills.length > 0 && (
                <button type="button" className="btn btn-primary" onClick={handleAddClick} disabled={actionsDisabled}>
                  Add Skill
                </button>
              )
            }
          />
        )}

        {mode === 'list' && mySkills.length > 0 && (
          <>
            {eligibleSkills.length === 0 && (
              <div className="alert alert-info" role="alert">
                {catalogSkills.length === 0
                  ? 'No active catalog skills are available to add.'
                  : 'All active catalog skills are already associated with your profile.'}
              </div>
            )}
            <div className="table-responsive">
              <table className="table table-striped table-hover align-middle">
                <thead>
                  <tr>
                    <th scope="col">Skill</th>
                    <th scope="col">Proficiency</th>
                    <th scope="col">Catalog status</th>
                    <th scope="col" className="text-end">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {mySkills.map((userSkill) => (
                    <tr key={userSkill.id}>
                      <td>{userSkill.skillName}</td>
                      <td>
                        <ProficiencyBadge level={userSkill.proficiencyLevel} />
                      </td>
                      <td>
                        {!userSkill.skillActive && <span className="badge bg-secondary">Disabled in catalog</span>}
                      </td>
                      <td className="text-end">
                        <div className="d-flex gap-2 justify-content-end">
                          <button
                            type="button"
                            className="btn btn-outline-secondary btn-sm"
                            onClick={() => handleEditClick(userSkill)}
                            disabled={actionsDisabled || !userSkill.skillActive}
                            title={
                              !userSkill.skillActive
                                ? 'This skill is disabled in the catalog and cannot be updated.'
                                : undefined
                            }
                          >
                            Update Proficiency
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            onClick={() => handleRemove(userSkill)}
                            disabled={actionsDisabled}
                          >
                            {removingSkillId === userSkill.skillId ? 'Removing...' : 'Remove'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
