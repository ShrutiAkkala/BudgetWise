import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from '../../pages/LoginPage';
import { AuthProvider } from '../../context/AuthContext';
import * as authApi from '../../api/authApi';

jest.mock('../../api/authApi');
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => jest.fn(),
}));

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  test('renders login form', () => {
    renderLoginPage();
    expect(screen.getByPlaceholderText(/Enter your username/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Enter your password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign In/i })).toBeInTheDocument();
  });

  test('shows link to register page', () => {
    renderLoginPage();
    expect(screen.getByText(/Create one/i)).toBeInTheDocument();
  });

  test('shows error on failed login', async () => {
    authApi.login.mockRejectedValue({
      response: { data: { error: 'Invalid username or password' } },
    });

    renderLoginPage();
    fireEvent.change(screen.getByPlaceholderText(/Enter your username/i), {
      target: { value: 'alice' },
    });
    fireEvent.change(screen.getByPlaceholderText(/Enter your password/i), {
      target: { value: 'wrongpass' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Sign In/i }));

    await waitFor(() => {
      expect(screen.getByText(/Invalid username or password/i)).toBeInTheDocument();
    });
  });

  test('calls login API with correct credentials', async () => {
    authApi.login.mockResolvedValue({
      data: { token: 'tok', username: 'alice', email: 'alice@test.com' },
    });

    renderLoginPage();
    fireEvent.change(screen.getByPlaceholderText(/Enter your username/i), {
      target: { value: 'alice' },
    });
    fireEvent.change(screen.getByPlaceholderText(/Enter your password/i), {
      target: { value: 'password123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Sign In/i }));

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({
        username: 'alice',
        password: 'password123',
      });
    });
  });
});
