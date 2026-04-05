import React from 'react';
import { render, screen, act } from '@testing-library/react';
import { AuthProvider, useAuth } from '../../context/AuthContext';

// Helper component to read context values
function AuthDisplay() {
  const { auth, login, logout } = useAuth();
  return (
    <div>
      <div data-testid="username">{auth?.username || 'none'}</div>
      <button onClick={() => login('tok123', 'alice')}>Login</button>
      <button onClick={logout}>Logout</button>
    </div>
  );
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test('starts unauthenticated when no token in storage', () => {
    render(<AuthProvider><AuthDisplay /></AuthProvider>);
    expect(screen.getByTestId('username').textContent).toBe('none');
  });

  test('login sets auth state and localStorage', () => {
    render(<AuthProvider><AuthDisplay /></AuthProvider>);

    act(() => {
      screen.getByText('Login').click();
    });

    expect(screen.getByTestId('username').textContent).toBe('alice');
    expect(localStorage.getItem('token')).toBe('tok123');
    expect(localStorage.getItem('username')).toBe('alice');
  });

  test('logout clears auth state and localStorage', () => {
    render(<AuthProvider><AuthDisplay /></AuthProvider>);

    act(() => { screen.getByText('Login').click(); });
    act(() => { screen.getByText('Logout').click(); });

    expect(screen.getByTestId('username').textContent).toBe('none');
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('username')).toBeNull();
  });

  test('restores auth from localStorage on mount', () => {
    localStorage.setItem('token', 'existing-tok');
    localStorage.setItem('username', 'bob');

    render(<AuthProvider><AuthDisplay /></AuthProvider>);
    expect(screen.getByTestId('username').textContent).toBe('bob');
  });
});
