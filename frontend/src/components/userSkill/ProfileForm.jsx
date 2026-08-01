import { useState } from 'react';

const DISPLAY_NAME_MIN = 2;
const DISPLAY_NAME_MAX = 100;
const HEADLINE_MAX = 160;
const BIO_MAX = 500;

function validate(form) {
  const errors = {};
  const trimmedName = form.displayName.trim();

  if (!trimmedName) {
    errors.displayName = 'Display name is required.';
  } else if (trimmedName.length < DISPLAY_NAME_MIN) {
    errors.displayName = `Display name must be at least ${DISPLAY_NAME_MIN} characters.`;
  } else if (trimmedName.length > DISPLAY_NAME_MAX) {
    errors.displayName = `Display name must be at most ${DISPLAY_NAME_MAX} characters.`;
  }

  if (form.headline.length > HEADLINE_MAX) {
    errors.headline = `Headline must be at most ${HEADLINE_MAX} characters.`;
  }

  if (form.bio.length > BIO_MAX) {
    errors.bio = `Bio must be at most ${BIO_MAX} characters.`;
  }

  return errors;
}

export default function ProfileForm({ initialValues, submitting, submitLabel, submittingLabel, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    displayName: initialValues?.displayName || '',
    headline: initialValues?.headline || '',
    bio: initialValues?.bio || '',
  });
  const [fieldErrors, setFieldErrors] = useState({});

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

  function handleSubmit(event) {
    event.preventDefault();
    if (submitting) {
      return;
    }

    const errors = validate(form);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    const trimmedHeadline = form.headline.trim();
    const trimmedBio = form.bio.trim();

    onSubmit({
      displayName: form.displayName.trim(),
      headline: trimmedHeadline === '' ? null : trimmedHeadline,
      bio: trimmedBio === '' ? null : trimmedBio,
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="mb-3">
        <label htmlFor="displayName" className="form-label">
          Display name
        </label>
        <input
          id="displayName"
          name="displayName"
          type="text"
          className={`form-control${fieldErrors.displayName ? ' is-invalid' : ''}`}
          value={form.displayName}
          onChange={handleChange}
          disabled={submitting}
          maxLength={DISPLAY_NAME_MAX}
          required
        />
        {fieldErrors.displayName && <div className="invalid-feedback">{fieldErrors.displayName}</div>}
      </div>

      <div className="mb-3">
        <label htmlFor="headline" className="form-label">
          Headline
        </label>
        <input
          id="headline"
          name="headline"
          type="text"
          className={`form-control${fieldErrors.headline ? ' is-invalid' : ''}`}
          value={form.headline}
          onChange={handleChange}
          disabled={submitting}
          maxLength={HEADLINE_MAX}
        />
        {fieldErrors.headline && <div className="invalid-feedback">{fieldErrors.headline}</div>}
      </div>

      <div className="mb-3">
        <label htmlFor="bio" className="form-label">
          Bio
        </label>
        <textarea
          id="bio"
          name="bio"
          className={`form-control${fieldErrors.bio ? ' is-invalid' : ''}`}
          value={form.bio}
          onChange={handleChange}
          disabled={submitting}
          maxLength={BIO_MAX}
          rows={4}
        />
        {fieldErrors.bio && <div className="invalid-feedback">{fieldErrors.bio}</div>}
      </div>

      <div className="d-flex gap-2">
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? submittingLabel : submitLabel}
        </button>
        {onCancel && (
          <button type="button" className="btn btn-outline-secondary" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
