import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { register as registerApi } from '../api/authApi';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [errorCode, setErrorCode] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // Pre-fill username if redirected from login page
  useEffect(() => {
    const u = searchParams.get('username');
    if (u) setForm(f => ({ ...f, username: u }));
  }, [searchParams]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }
    setLoading(true);
    setError('');
    setErrorCode('');
    try {
      const res = await registerApi(form);
      login(res.data.token, res.data.username);
      navigate('/dashboard');
    } catch (err) {
      const data = err.response?.data;
      const msg = data?.error || 'Registration failed. Try a different username.';
      setError(msg);
      if (msg.toLowerCase().includes('username already taken')) setErrorCode('USERNAME_TAKEN');
      else if (msg.toLowerCase().includes('email already registered')) setErrorCode('EMAIL_TAKEN');
      else setErrorCode('');
    } finally {
      setLoading(false);
    }
  };

  const alreadyExists = errorCode === 'USERNAME_TAKEN' || errorCode === 'EMAIL_TAKEN';

  return (
    <div className="auth-page">
      <div className="auth-panel">
        <div className="auth-panel-logo">💰</div>
        <div className="auth-panel-brand">BudgetWise</div>
        <div className="auth-panel-title">Start your financial journey</div>
        <div className="auth-panel-subtitle">
          Join thousands of people who track their budgets smarter, not harder.
        </div>
        <ul className="auth-panel-features">
          <li>Free to get started</li>
          <li>Connect unlimited bank accounts</li>
          <li>Visual spending analytics</li>
          <li>AI-powered financial advice</li>
        </ul>
      </div>

      <div className="auth-form-side">
        <div className="auth-form-container">
          <span className="auth-form-logo-mobile">💰</span>
          <h1 className="auth-form-title">Create your account</h1>
          <p className="auth-form-subtitle">Start tracking your budget today — it's free</p>

          {error && (
            <div className="auth-error">
              <span>⚠</span>
              <div style={{ flex: 1 }}>
                <div>{error}</div>
                {alreadyExists && (
                  <div style={{ marginTop: '0.4rem', fontSize: '0.82rem' }}>
                    Already have an account?{' '}
                    <Link
                      to={`/login`}
                      style={{ color: '#dc2626', fontWeight: '700', textDecoration: 'underline' }}
                    >
                      Sign in instead →
                    </Link>
                  </div>
                )}
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-field">
              <label className="form-label">Username</label>
              <input
                className="form-input"
                type="text"
                value={form.username}
                onChange={e => setForm({ ...form, username: e.target.value })}
                placeholder="Choose a username"
                autoFocus
                required
              />
            </div>
            <div className="form-field">
              <label className="form-label">Email address</label>
              <input
                className="form-input"
                type="email"
                value={form.email}
                onChange={e => setForm({ ...form, email: e.target.value })}
                placeholder="you@email.com"
                required
              />
            </div>
            <div className="form-field">
              <label className="form-label">Password</label>
              <input
                className="form-input"
                type="password"
                value={form.password}
                onChange={e => setForm({ ...form, password: e.target.value })}
                placeholder="At least 6 characters"
                required
              />
            </div>
            <button className="btn-primary" type="submit" disabled={loading}>
              {loading ? 'Creating Account…' : 'Create Account →'}
            </button>
          </form>

          <div className="auth-footer">
            Already have an account?{' '}
            <Link to="/login">Sign in</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
