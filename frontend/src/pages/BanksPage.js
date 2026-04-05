import React from 'react';
import Navbar from '../components/Navbar';
import BankConnectSection from '../components/BankConnectSection';

export default function BanksPage() {
  return (
    <div className="page">
      <Navbar />
      <div className="page-content" style={{ maxWidth: '800px' }}>
        <div style={{ marginBottom: '2rem' }}>
          <h1 className="page-title">Bank Connections</h1>
          <div className="page-subtitle">Securely connect your bank accounts to auto-import transactions</div>
        </div>

        <div className="info-card">
          <div className="info-card-title">🔒 Secure Connection via Plaid</div>
          <div className="info-card-body">
            Your bank credentials are never stored. We use Plaid — the same technology trusted
            by apps like Venmo, Coinbase, and Robinhood — to securely connect your accounts.
            You can disconnect at any time.
          </div>
        </div>

        <BankConnectSection />
      </div>
    </div>
  );
}
