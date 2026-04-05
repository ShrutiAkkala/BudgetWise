import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => { logout(); navigate('/login'); };

  const isActive = (path) => location.pathname === path;

  const avatarLetter = auth?.username?.charAt(0)?.toUpperCase() || 'U';

  return (
    <nav className="navbar">
      <Link to="/dashboard" className="navbar-brand">
        <div className="navbar-brand-icon">💰</div>
        <span className="navbar-brand-text">BudgetWise</span>
      </Link>

      <div className="navbar-links">
        <Link to="/dashboard" className={`navbar-link${isActive('/dashboard') ? ' active' : ''}`}>
          Dashboard
        </Link>
        <Link to="/transactions" className={`navbar-link${isActive('/transactions') ? ' active' : ''}`}>
          Transactions
        </Link>
        <Link to="/banks" className={`navbar-link${isActive('/banks') ? ' active' : ''}`}>
          Banks
        </Link>
      </div>

      <div className="navbar-right">
        <div className="navbar-avatar">{avatarLetter}</div>
        <span className="navbar-username">{auth?.username}</span>
        <button className="navbar-logout" onClick={handleLogout}>Sign out</button>
      </div>
    </nav>
  );
}
