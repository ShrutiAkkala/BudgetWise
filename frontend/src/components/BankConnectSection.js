import React, { useState, useCallback, useEffect } from 'react';
import { usePlaidLink } from 'react-plaid-link';
import { createLinkToken, exchangePublicToken, getConnectedAccounts, disconnectAccount, syncTransactions } from '../api/plaidApi';

function PlaidLinkButton({ onSuccess, disabled }) {
  const [linkToken, setLinkToken] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchToken = useCallback(async () => {
    setLoading(true);
    try {
      const res = await createLinkToken();
      setLinkToken(res.data.linkToken);
    } catch (e) {
      console.error('Failed to create link token', e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchToken(); }, [fetchToken]);

  const { open, ready } = usePlaidLink({
    token: linkToken,
    onSuccess: (publicToken, metadata) => {
      onSuccess(publicToken, metadata);
      fetchToken();
    },
  });

  return (
    <button className="btn-add" onClick={() => open()} disabled={!ready || loading || disabled}>
      🏦 {loading ? 'Loading…' : 'Connect Bank'}
    </button>
  );
}

export default function BankConnectSection({ onSync }) {
  const [accounts, setAccounts] = useState([]);
  const [syncing, setSyncing] = useState(false);
  const [syncStatus, setSyncStatus] = useState(null);
  const [connecting, setConnecting] = useState(false);

  const fetchAccounts = useCallback(async () => {
    try {
      const res = await getConnectedAccounts();
      setAccounts(res.data);
    } catch (e) {
      console.error('Failed to fetch accounts', e);
    }
  }, []);

  useEffect(() => { fetchAccounts(); }, [fetchAccounts]);

  const handlePlaidSuccess = async (publicToken, metadata) => {
    setConnecting(true);
    try {
      const institution = metadata?.institution;
      const account = metadata?.accounts?.[0];
      await exchangePublicToken({
        publicToken,
        institutionName: institution?.name || 'Bank',
        accountName: account?.name || 'Account',
        accountMask: account?.mask,
        accountType: account?.subtype,
      });
      await fetchAccounts();
      setSyncStatus({ type: 'success', message: 'Bank connected & transactions imported!' });
      if (onSync) onSync();
    } catch (e) {
      setSyncStatus({ type: 'error', message: 'Failed to connect bank. Please try again.' });
    } finally {
      setConnecting(false);
      setTimeout(() => setSyncStatus(null), 4000);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    setSyncStatus(null);
    try {
      const res = await syncTransactions();
      setSyncStatus({ type: 'success', message: res.data.message });
      if (onSync) onSync();
    } catch (e) {
      setSyncStatus({ type: 'error', message: 'Sync failed. Please try again.' });
    } finally {
      setSyncing(false);
      setTimeout(() => setSyncStatus(null), 5000);
    }
  };

  const handleDisconnect = async (id, name) => {
    if (!window.confirm(`Disconnect ${name}?`)) return;
    try {
      await disconnectAccount(id);
      setAccounts(accounts.filter(a => a.id !== id));
    } catch (e) {
      alert('Failed to disconnect account');
    }
  };

  return (
    <div className="card" style={{ marginBottom: '1.5rem' }}>
      <div className="card-header">
        <div>
          <div className="card-title">🏦 Connected Banks</div>
          <div style={{ color: 'var(--text-muted)', fontSize: '0.78rem', marginTop: '0.2rem' }}>
            Auto-import transactions from your bank
          </div>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {accounts.length > 0 && (
            <button className="btn-outline" onClick={handleSync} disabled={syncing}>
              {syncing ? '⏳ Syncing…' : '🔄 Sync Now'}
            </button>
          )}
          <PlaidLinkButton onSuccess={handlePlaidSuccess} disabled={connecting} />
        </div>
      </div>

      <div className="card-body" style={{ padding: accounts.length === 0 ? '1.5rem' : '1rem 1.5rem' }}>
        {accounts.length === 0 ? (
          <div style={{ color: 'var(--text-muted)', fontSize: '0.875rem', textAlign: 'center', padding: '0.5rem 0' }}>
            No bank accounts connected. Click "Connect Bank" to get started.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
            {accounts.map(account => (
              <div key={account.id} style={{
                display: 'flex', alignItems: 'center', gap: '1rem',
                padding: '0.75rem 1rem', background: 'var(--bg)', borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--border-light)',
              }}>
                <div style={{
                  width: '40px', height: '40px', background: 'var(--primary-gradient)',
                  borderRadius: 'var(--radius-sm)', display: 'flex', alignItems: 'center',
                  justifyContent: 'center', fontSize: '1.2rem', flexShrink: 0,
                }}>🏦</div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: '600', color: 'var(--text-primary)', fontSize: '0.875rem' }}>
                    {account.institutionName}
                  </div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem', marginTop: '0.15rem' }}>
                    {account.accountName}{account.accountMask ? ` ••••${account.accountMask}` : ''} · {account.accountType}
                  </div>
                </div>
                <button
                  className="btn-icon danger"
                  onClick={() => handleDisconnect(account.id, account.institutionName)}
                  title="Disconnect"
                  style={{ width: 'auto', padding: '0.3rem 0.65rem', fontSize: '0.75rem' }}
                >
                  Disconnect
                </button>
              </div>
            ))}
          </div>
        )}

        {syncStatus && (
          <div style={{
            marginTop: '0.875rem',
            padding: '0.5rem 0.875rem',
            borderRadius: 'var(--radius-sm)',
            fontSize: '0.82rem',
            background: syncStatus.type === 'success' ? '#f0fdf4' : '#fef2f2',
            color: syncStatus.type === 'success' ? '#16a34a' : '#dc2626',
            border: `1px solid ${syncStatus.type === 'success' ? '#bbf7d0' : '#fecaca'}`,
          }}>
            {syncStatus.type === 'success' ? '✅' : '❌'} {syncStatus.message}
          </div>
        )}
      </div>
    </div>
  );
}
