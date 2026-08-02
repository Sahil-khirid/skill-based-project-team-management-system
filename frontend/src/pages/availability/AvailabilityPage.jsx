import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as userSkillApi from '../../api/userSkillApi';
import AvailabilityForm from '../../components/userSkill/AvailabilityForm';
import LoadingSpinner from '../../components/LoadingSpinner';

const PROFILE_NOT_FOUND_MESSAGE = 'No profile exists for this user.';
const AVAILABILITY_NOT_FOUND_MESSAGE = 'No availability exists for this user.';
const AVAILABILITY_ALREADY_EXISTS_MESSAGE = 'Availability already exists for this user.';

export default function AvailabilityPage() {
  const [availability, setAvailability] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [mode, setMode] = useState('create');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [profileMissing, setProfileMissing] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [noticeMessage, setNoticeMessage] = useState('');
  const submissionLockRef = useRef(false);

  const loadAvailability = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await userSkillApi.getMyAvailability();
      setAvailability(data);
      setMode('view');
    } catch (error) {
      const message = extractErrorMessage(error);

      if (error.response?.status === 404 && message === AVAILABILITY_NOT_FOUND_MESSAGE) {
        setAvailability(null);
        setMode('create');
      } else {
        setLoadError(message);
      }
    } finally {
      setInitialLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAvailability();
  }, [loadAvailability]);

  async function handleCreate(values) {
    if (submissionLockRef.current) {
      return;
    }
    submissionLockRef.current = true;
    setSubmitting(true);
    setSubmitError('');
    setProfileMissing(false);
    setSuccessMessage('');
    setNoticeMessage('');
    try {
      const created = await userSkillApi.createMyAvailability(values);
      setAvailability(created);
      setMode('view');
      setSuccessMessage('Availability created successfully.');
    } catch (error) {
      const status = error.response?.status;
      const message = extractErrorMessage(error);

      if (status === 409 && message === AVAILABILITY_ALREADY_EXISTS_MESSAGE) {
        try {
          const current = await userSkillApi.getMyAvailability();
          setAvailability(current);
          setMode('view');
          setNoticeMessage('Availability already exists for this user. The current value has been loaded.');
        } catch {
          setSubmitError(message);
        }
      } else {
        setSubmitError(message);
        setProfileMissing(status === 404 && message === PROFILE_NOT_FOUND_MESSAGE);
      }
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
      const updated = await userSkillApi.updateMyAvailability(values);
      setAvailability(updated);
      setMode('view');
      setSuccessMessage('Availability updated successfully.');
    } catch (error) {
      const status = error.response?.status;
      const message = extractErrorMessage(error);

      if (status === 404 && message === AVAILABILITY_NOT_FOUND_MESSAGE) {
        setAvailability(null);
        setMode('create');
        setSubmitError(message);
      } else {
        setSubmitError(message);
      }
    } finally {
      submissionLockRef.current = false;
      setSubmitting(false);
    }
  }

  function handleEditClick() {
    if (submissionLockRef.current) {
      return;
    }
    setSubmitError('');
    setSuccessMessage('');
    setNoticeMessage('');
    setMode('edit');
  }

  function handleCancelEdit() {
    if (submissionLockRef.current) {
      return;
    }
    setSubmitError('');
    setMode('view');
  }

  if (initialLoading) {
    return <LoadingSpinner label="Loading your availability..." />;
  }

  if (loadError) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-6">
          <div className="alert alert-danger" role="alert">
            {loadError}
          </div>
          <button type="button" className="btn btn-primary" onClick={loadAvailability}>
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
              <h1 className="h4 mb-2">Availability</h1>
              <p className="text-muted mb-4">
                Set how many hours per week you are currently available for project work.
              </p>

              {submitError && (
                <div className="alert alert-danger" role="alert">
                  {submitError}
                  {profileMissing && (
                    <div className="mt-2">
                      <Link to="/profile">Create your profile</Link> before setting availability.
                    </div>
                  )}
                </div>
              )}

              <AvailabilityForm
                submitting={submitting}
                submitLabel="Save Availability"
                submittingLabel="Saving..."
                onSubmit={handleCreate}
              />
            </div>
          </div>
        )}

        {mode === 'view' && availability && (
          <div className="card shadow-sm">
            <div className="card-body">
              <h1 className="h4 mb-4">Availability</h1>

              {successMessage && (
                <div className="alert alert-success" role="alert">
                  {successMessage}
                </div>
              )}

              {noticeMessage && (
                <div className="alert alert-warning" role="alert">
                  {noticeMessage}
                </div>
              )}

              <dl className="row mb-4">
                <dt className="col-sm-6">Available hours per week</dt>
                <dd className="col-sm-6">{availability.availableHoursPerWeek} hours</dd>
              </dl>

              <button type="button" className="btn btn-primary" onClick={handleEditClick}>
                Edit Availability
              </button>
            </div>
          </div>
        )}

        {mode === 'edit' && availability && (
          <div className="card shadow-sm">
            <div className="card-body">
              <h1 className="h4 mb-4">Edit Availability</h1>

              {submitError && (
                <div className="alert alert-danger" role="alert">
                  {submitError}
                </div>
              )}

              <AvailabilityForm
                initialValues={availability}
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
