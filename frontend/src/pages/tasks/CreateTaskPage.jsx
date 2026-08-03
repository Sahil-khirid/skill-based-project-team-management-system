import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as taskApi from '../../api/taskApi';

const TITLE_MIN = 2;
const TITLE_MAX = 150;
const DESCRIPTION_MAX = 2000;
const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH'];
const PROJECT_ID_PATTERN = /^\d+$/;
const SUCCESS_REDIRECT_DELAY_MS = 1200;

function validate(form) {
  const errors = {};

  const trimmedProjectId = form.projectId.trim();
  if (!trimmedProjectId) {
    errors.projectId = 'Project ID is required.';
  } else if (!PROJECT_ID_PATTERN.test(trimmedProjectId)) {
    errors.projectId = 'Project ID must be a positive whole number.';
  }

  const trimmedTitle = form.title.trim();
  if (!trimmedTitle) {
    errors.title = 'Title is required.';
  } else if (trimmedTitle.length < TITLE_MIN || trimmedTitle.length > TITLE_MAX) {
    errors.title = `Title must be between ${TITLE_MIN} and ${TITLE_MAX} characters.`;
  }

  if (form.description.length > DESCRIPTION_MAX) {
    errors.description = `Description must be at most ${DESCRIPTION_MAX} characters.`;
  }

  if (!form.priority) {
    errors.priority = 'Priority is required.';
  }

  return errors;
}

export default function CreateTaskPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    projectId: '',
    title: '',
    description: '',
    priority: '',
    dueDate: '',
  });
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
      await taskApi.createTask({
        projectId: Number(form.projectId.trim()),
        title: form.title.trim(),
        description: trimmedDescription === '' ? null : trimmedDescription,
        priority: form.priority,
        dueDate: form.dueDate === '' ? null : form.dueDate,
      });
      setSuccessMessage('Task created successfully.');
      redirectTimeoutRef.current = setTimeout(() => {
        navigate('/tasks');
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
        <h1 className="h4 mb-4">Create Task</h1>

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
                <label htmlFor="projectId" className="form-label">
                  Project ID
                </label>
                <input
                  id="projectId"
                  name="projectId"
                  type="number"
                  min="1"
                  step="1"
                  className={`form-control${fieldErrors.projectId ? ' is-invalid' : ''}`}
                  value={form.projectId}
                  onChange={handleChange}
                  disabled={submitting}
                  required
                />
                {fieldErrors.projectId && <div className="invalid-feedback">{fieldErrors.projectId}</div>}
              </div>

              <div className="mb-3">
                <label htmlFor="title" className="form-label">
                  Title
                </label>
                <input
                  id="title"
                  name="title"
                  type="text"
                  className={`form-control${fieldErrors.title ? ' is-invalid' : ''}`}
                  value={form.title}
                  onChange={handleChange}
                  disabled={submitting}
                  maxLength={TITLE_MAX}
                  required
                />
                {fieldErrors.title && <div className="invalid-feedback">{fieldErrors.title}</div>}
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

              <div className="mb-3">
                <label htmlFor="priority" className="form-label">
                  Priority
                </label>
                <select
                  id="priority"
                  name="priority"
                  className={`form-select${fieldErrors.priority ? ' is-invalid' : ''}`}
                  value={form.priority}
                  onChange={handleChange}
                  disabled={submitting}
                  required
                >
                  <option value="">Select priority</option>
                  {PRIORITY_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
                {fieldErrors.priority && <div className="invalid-feedback">{fieldErrors.priority}</div>}
              </div>

              <div className="mb-3">
                <label htmlFor="dueDate" className="form-label">
                  Due Date
                </label>
                <input
                  id="dueDate"
                  name="dueDate"
                  type="date"
                  className="form-control"
                  value={form.dueDate}
                  onChange={handleChange}
                  disabled={submitting}
                />
              </div>

              <div className="d-flex gap-2">
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Creating...' : 'Create Task'}
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => navigate('/tasks')}
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
