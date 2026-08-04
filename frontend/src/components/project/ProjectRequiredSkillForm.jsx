import { useState } from 'react';

const PROFICIENCY_OPTIONS = [
  { value: 'BEGINNER', label: 'Beginner' },
  { value: 'INTERMEDIATE', label: 'Intermediate' },
  { value: 'ADVANCED', label: 'Advanced' },
  { value: 'EXPERT', label: 'Expert' },
];

const VALID_PROFICIENCY_LEVELS = PROFICIENCY_OPTIONS.map((option) => option.value);

function validate(form, eligibleSkills) {
  const errors = {};

  if (!form.skillId) {
    errors.skillId = 'Please select a skill.';
  } else if (!eligibleSkills.some((skill) => String(skill.id) === String(form.skillId))) {
    errors.skillId = 'Please select a valid available skill.';
  }

  if (!form.proficiencyLevel) {
    errors.proficiencyLevel = 'Please select a proficiency level.';
  } else if (!VALID_PROFICIENCY_LEVELS.includes(form.proficiencyLevel)) {
    errors.proficiencyLevel = 'Please select a valid proficiency level.';
  }

  return errors;
}

export default function ProjectRequiredSkillForm({
  eligibleSkills,
  submitting,
  submitLabel,
  submittingLabel,
  onSubmit,
  onCancel,
}) {
  const [form, setForm] = useState({ skillId: '', proficiencyLevel: '' });
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

    const errors = validate(form, eligibleSkills);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    onSubmit({ skillId: Number(form.skillId), proficiencyLevel: form.proficiencyLevel });
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="mb-3">
        <label htmlFor="skillId" className="form-label">
          Skill
        </label>
        <select
          id="skillId"
          name="skillId"
          className={`form-select${fieldErrors.skillId ? ' is-invalid' : ''}`}
          value={form.skillId}
          onChange={handleChange}
          disabled={submitting}
          required
        >
          <option value="">Select a skill...</option>
          {eligibleSkills.map((skill) => (
            <option key={skill.id} value={skill.id}>
              {skill.name}
            </option>
          ))}
        </select>
        {fieldErrors.skillId && <div className="invalid-feedback">{fieldErrors.skillId}</div>}
      </div>

      <div className="mb-3">
        <label htmlFor="proficiencyLevel" className="form-label">
          Proficiency level
        </label>
        <select
          id="proficiencyLevel"
          name="proficiencyLevel"
          className={`form-select${fieldErrors.proficiencyLevel ? ' is-invalid' : ''}`}
          value={form.proficiencyLevel}
          onChange={handleChange}
          disabled={submitting}
          required
        >
          <option value="">Select a proficiency level...</option>
          {PROFICIENCY_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        {fieldErrors.proficiencyLevel && <div className="invalid-feedback">{fieldErrors.proficiencyLevel}</div>}
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
