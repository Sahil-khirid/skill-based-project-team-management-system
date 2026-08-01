import { useCallback, useEffect, useRef, useState } from 'react';
import { extractErrorMessage } from '../../api/errorMessage';
import * as userSkillApi from '../../api/userSkillApi';
import ProfileForm from '../../components/userSkill/ProfileForm';
import LoadingSpinner from '../../components/LoadingSpinner';

export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [mode, setMode] = useState('create');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const submissionLockRef = useRef(false);

  const loadProfile = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await userSkillApi.getMyProfile();
      setProfile(data);
      setMode('view');
    } catch (error) {
      if (error.response?.status === 404) {
        setProfile(null);
        setMode('create');
      } else {
        setLoadError(extractErrorMessage(error));
      }
    } finally {
      setInitialLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  async function handleCreate(values) {
    if (submissionLockRef.current) {
      return;
    }
    submissionLockRef.current = true;
    setSubmitting(true);
    setSubmitError('');
    setSuccessMessage('');
    try {
      const created = await userSkillApi.createMyProfile(values);
      setProfile(created);
      setMode('view');
      setSuccessMessage('Profile created successfully.');
    } catch (error) {
      setSubmitError(extractErrorMessage(error));
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
    setSubmitError('');
    setSuccessMessage('');
    try {
      const updated = await userSkillApi.updateMyProfile(values);
      setProfile(updated);
      setMode('view');
      setSuccessMessage('Profile updated successfully.');
    } catch (error) {
      setSubmitError(extractErrorMessage(error));
    } finally {
      submissionLockRef.current = false;
      setSubmitting(false);
    }
  }

  function handleEditClick() {
    setSubmitError('');
    setSuccessMessage('');
    setMode('edit');
  }

  function handleCancelEdit() {
    setSubmitError('');
    setMode('view');
  }

  if (initialLoading) {
    return <LoadingSpinner label="Loading your profile..." />;
  }

  if (loadError) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-6">
          <div className="alert alert-danger" role="alert">
            {loadError}
          </div>
          <button type="button" className="btn btn-primary" onClick={loadProfile}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-md-8 col-lg-6">
        {mode === 'create' && (
          <div className="card shadow-sm">
            <div className="card-body">
              <h1 className="h4 mb-2">Complete your profile</h1>
              <p className="text-muted mb-4">
                You have not created a profile yet. Fill in the details below to get started.
              </p>

              {submitError && (
                <div className="alert alert-danger" role="alert">
                  {submitError}
                </div>
              )}

              <ProfileForm
                submitting={submitting}
                submitLabel="Create Profile"
                submittingLabel="Creating..."
                onSubmit={handleCreate}
              />
            </div>
          </div>
        )}

        {mode === 'view' && profile && (
          <div className="card shadow-sm">
            <div className="card-body">
              <h1 className="h4 mb-4">My Profile</h1>

              {successMessage && (
                <div className="alert alert-success" role="alert">
                  {successMessage}
                </div>
              )}

              <dl className="row mb-4">
                <dt className="col-sm-4">Display name</dt>
                <dd className="col-sm-8">{profile.displayName}</dd>

                <dt className="col-sm-4">Headline</dt>
                <dd className="col-sm-8">{profile.headline || 'Not provided'}</dd>

                <dt className="col-sm-4">Bio</dt>
                <dd className="col-sm-8">{profile.bio || 'Not provided'}</dd>
              </dl>

              <button type="button" className="btn btn-primary" onClick={handleEditClick}>
                Edit Profile
              </button>
            </div>
          </div>
        )}

        {mode === 'edit' && profile && (
          <div className="card shadow-sm">
            <div className="card-body">
              <h1 className="h4 mb-4">Edit Profile</h1>

              {submitError && (
                <div className="alert alert-danger" role="alert">
                  {submitError}
                </div>
              )}

              <ProfileForm
                initialValues={profile}
                submitting={submitting}
                submitLabel="Save Changes"
                submittingLabel="Saving..."
                onSubmit={handleUpdate}
                onCancel={handleCancelEdit}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
