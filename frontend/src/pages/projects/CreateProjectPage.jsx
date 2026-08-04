import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as projectApi from '../../api/projectApi';

const NAME_MIN = 2;
const NAME_MAX = 150;
const DESCRIPTION_MAX = 500;
const SUCCESS_REDIRECT_DELAY_MS = 1200;

function validate(form) {
  const errors = {};

  const trimmedName = form.name.trim();
  if (!trimmedName) {
    errors.name = 'Project name is required.';
  } else if (trimmedName.length < NAME_MIN || trimmedName.length > NAME_MAX) {
    errors.name = `Project name must be between ${NAME_MIN} and ${NAME_MAX} characters.`;
  }

  if (form.description.length > DESCRIPTION_MAX) {
    errors.description = `Description must be at most ${DESCRIPTION_MAX} characters.`;
  }

  return errors;
}

export default function CreateProjectPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState({ name: '', description: '' });
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const submissionLockRef = useRef(false);
  const redirectTimeoutRef = useRef(null);

  useEffect(
    () => () => {
      if (redirectTimeoutRef.current) {
        clearTimeout(redirectTimeoutRef.current);
      }
    },
    [],
  );

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setFieldErrors((prev) => {
      if (!prev[name]) {
        return prev;
      }
      const next = { ...prev };
      delete next[name];
      return next;
    });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submissionLockRef.current) {
      return;
    }

    const errors = validate(form);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    submissionLockRef.current = true;
    setSubmitting(true);
    setActionError('');
    setSuccessMessage('');

    const trimmedDescription = form.description.trim();

    try {
      await projectApi.createProject({
        name: form.name.trim(),
        description: trimmedDescription === '' ? null : trimmedDescription,
      });
      setSuccessMessage('Project created successfully.');
      redirectTimeoutRef.current = setTimeout(() => {
        navigate('/projects');
      }, SUCCESS_REDIRECT_DELAY_MS);
    } catch (error) {
      setActionError(extractErrorMessage(error));
      submissionLockRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <h1 className="h4 mb-4">Create Project</h1>

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
                <label htmlFor="name" className="form-label">
                  Project Name
                </label>
                <input
                  id="name"
                  name="name"
                  type="text"
                  className={`form-control${fieldErrors.name ? ' is-invalid' : ''}`}
                  value={form.name}
                  onChange={handleChange}
                  disabled={submitting}
                  maxLength={NAME_MAX}
                  required
                />
                {fieldErrors.name && <div className="invalid-feedback">{fieldErrors.name}</div>}
              </div>

              <div className="mb-3">
                <label htmlFor="description" className="form-label">
                  Description
                </label>
                <textarea
                  id="description"
                  name="description"
                  className={`form-control${fieldErrors.description ? ' is-invalid' : ''}`}
                  value={form.description}
                  onChange={handleChange}
                  disabled={submitting}
                  maxLength={DESCRIPTION_MAX}
                  rows={4}
                />
                {fieldErrors.description && <div className="invalid-feedback">{fieldErrors.description}</div>}
              </div>

              <div className="alert alert-info" role="status">
                New projects start with status PLANNING. Status can be changed afterwards from the Edit page.
              </div>

              <div className="d-flex gap-2">
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Creating...' : 'Create Project'}
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => navigate('/projects')}
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
