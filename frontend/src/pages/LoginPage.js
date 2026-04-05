import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login as loginApi } from '../api/authApi';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const [errorCode, setErrorCode] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setErrorCode('');
    try {
      const res = await loginApi(form);
      login(res.data.token, res.data.username);
      navigate('/dashboard');
    } catch (err) {
      const data = err.response?.data;
      setError(data?.error || 'Invalid username or password');
      setErrorCode(data?.code || '');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-panel">
        <div className="auth-panel-logo">💰</div>
        <div className="auth-panel-brand">BudgetWise</div>
        <div className="auth-panel-title">Take control of your finances</div>
        <div className="auth-panel-subtitle">
          Track income, expenses, and savings all in one beautiful dashboard.
        </div>
        <ul className="auth-panel-features">
          <li>Real-time spending insights</li>
          <li>Automatic bank sync via Plaid</li>
          <li>Category breakdowns &amp; trends</li>
          <li>AI-powered financial advice</li>
        </ul>
      </div>

      <div className="auth-form-side">
        <div className="auth-form-container">
          <span className="auth-form-logo-mobile">💰</span>
          <h1 className="auth-form-title">Welcome back</h1>
          <p className="auth-form-subtitle">Sign in to your Budget Tracker account</p>

          {error && (
            <div className="auth-error">
              <span>⚠</span>
              <div style={{ flex: 1 }}>
                <div>{error}</div>
                {errorCode === 'USER_NOT_FOUND' && (
                  <div style={{ marginTop: '0.4rem', fontSize: '0.82rem' }}>
                    Don't have an account?{' '}
                    <Link
                      to={`/register?username=${encodeURIComponent(form.username)}`}
                      style={{ color: '#dc2626', fontWeight: '700', textDecoration: 'underline' }}
                    >
                      Create one free →
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
                placeholder="Enter your username"
                autoFocus
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
                placeholder="Enter your password"
                required
              />
            </div>
            <button className="btn-primary" type="submit" disabled={loading}>
              {loading ? 'Signing in…' : 'Sign In →'}
            </button>
          </form>

          <div className="auth-footer">
            Don't have an account?{' '}
            <Link to="/register">Create one free</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
