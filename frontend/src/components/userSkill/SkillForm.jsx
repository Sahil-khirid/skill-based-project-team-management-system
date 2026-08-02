import { useState } from 'react';

const NAME_MAX = 100;
const DESCRIPTION_MAX = 500;

function validate(form) {
  const errors = {};
  const trimmedName = form.name.trim();

  if (!trimmedName) {
    errors.name = 'Name is required.';
  } else if (trimmedName.length > NAME_MAX) {
    errors.name = `Name must be at most ${NAME_MAX} characters.`;
  }

  if (form.description.length > DESCRIPTION_MAX) {
    errors.description = `Description must be at most ${DESCRIPTION_MAX} characters.`;
  }

  return errors;
}

export default function SkillForm({ initialValues, submitting, submitLabel, submittingLabel, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    name: initialValues?.name || '',
    description: initialValues?.description || '',
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

    const trimmedDescription = form.description.trim();

    onSubmit({
      name: form.name.trim(),
      description: trimmedDescription === '' ? null : trimmedDescription,
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="mb-3">
        <label htmlFor="name" className="form-label">
          Name
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
          rows={3}
        />
        {fieldErrors.description && <div className="invalid-feedback">{fieldErrors.description}</div>}
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
