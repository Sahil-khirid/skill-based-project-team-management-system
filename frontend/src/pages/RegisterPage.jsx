import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { extractErrorMessage } from '../api/errorMessage';
import { useAuth } from '../auth/useAuth';

const initialForm = { email: '', password: '', confirmPassword: '' };

export default function RegisterPage() {
  const { register, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState(initialForm);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (form.password !== form.confirmPassword) {
      setErrorMessage('Passwords do not match.');
      return;
    }

    setSubmitting(true);
    try {
      // Registration DTO only accepts email and password; confirmPassword is client-only.
      await register({ email: form.email, password: form.password });
      setSuccessMessage('Registration successful. Redirecting to login...');
      setTimeout(() => navigate('/login', { replace: true }), 1200);
    } catch (error) {
      setErrorMessage(extractErrorMessage(error, 'Registration failed. Please try again.'));
      setSubmitting(false);
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-md-6 col-lg-5">
        <div className="card shadow-sm">
          <div className="card-body">
            <h1 className="h4 mb-4">Create an account</h1>

            {errorMessage && (
              <div className="alert alert-danger" role="alert">
                {errorMessage}
              </div>
            )}
            {successMessage && (
              <div className="alert alert-success" role="alert">
                {successMessage}
              </div>
            )}

            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label htmlFor="email" className="form-label">
                  Email
                </label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  className="form-control"
                  value={form.email}
                  onChange={handleChange}
                  required
                  autoComplete="email"
                />
              </div>

              <div className="mb-3">
                <label htmlFor="password" className="form-label">
                  Password
                </label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  className="form-control"
                  value={form.password}
                  onChange={handleChange}
                  required
                  minLength={8}
                  maxLength={72}
                  autoComplete="new-password"
                />
                <div className="form-text">At least 8 characters.</div>
              </div>

              <div className="mb-3">
                <label htmlFor="confirmPassword" className="form-label">
                  Confirm password
                </label>
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  className="form-control"
                  value={form.confirmPassword}
                  onChange={handleChange}
                  required
                  minLength={8}
                  maxLength={72}
                  autoComplete="new-password"
                />
              </div>

              <button type="submit" className="btn btn-primary w-100" disabled={submitting || !!successMessage}>
                {submitting ? 'Creating account...' : 'Register'}
              </button>
            </form>

            <p className="mt-3 mb-0 text-center">
              Already have an account? <Link to="/login">Login</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
