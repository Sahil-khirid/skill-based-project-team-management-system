import { useState } from 'react';

const MIN_HOURS = 0;
const MAX_HOURS = 168;
const WHOLE_NUMBER_PATTERN = /^-?\d+$/;

function validate(rawValue) {
  const trimmed = rawValue.trim();

  if (trimmed === '') {
    return 'Available hours per week is required.';
  }

  if (!WHOLE_NUMBER_PATTERN.test(trimmed)) {
    return 'Available hours per week must be a whole number.';
  }

  const numberValue = Number(trimmed);

  if (!Number.isFinite(numberValue) || !Number.isInteger(numberValue)) {
    return 'Available hours per week must be a whole number.';
  }

  if (numberValue < MIN_HOURS || numberValue > MAX_HOURS) {
    return 'Available hours per week must be between 0 and 168.';
  }

  return '';
}

export default function AvailabilityForm({ initialValues, submitting, submitLabel, submittingLabel, onSubmit, onCancel }) {
  const [hoursInput, setHoursInput] = useState(
    initialValues?.availableHoursPerWeek !== undefined && initialValues?.availableHoursPerWeek !== null
      ? String(initialValues.availableHoursPerWeek)
      : '',
  );
  const [fieldError, setFieldError] = useState('');

  function handleChange(event) {
    setHoursInput(event.target.value);
    if (fieldError) {
      setFieldError('');
    }
  }

  function handleSubmit(event) {
    event.preventDefault();
    if (submitting) {
      return;
    }

    const error = validate(hoursInput);
    if (error) {
      setFieldError(error);
      return;
    }

    onSubmit({ availableHoursPerWeek: Number(hoursInput.trim()) });
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="mb-3">
        <label htmlFor="availableHoursPerWeek" className="form-label">
          Available hours per week
        </label>
        <input
          id="availableHoursPerWeek"
          name="availableHoursPerWeek"
          type="number"
          min="0"
          max="168"
          step="1"
          className={`form-control${fieldError ? ' is-invalid' : ''}`}
          value={hoursInput}
          onChange={handleChange}
          disabled={submitting}
          required
        />
        {fieldError && <div className="invalid-feedback">{fieldError}</div>}
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
