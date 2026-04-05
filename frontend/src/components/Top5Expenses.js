import React from 'react';

const CATEGORY_ICONS = {
  FOOD: '🍔', TRANSPORT: '🚗', HOUSING: '🏠',
  ENTERTAINMENT: '🎬', HEALTH: '💊', SHOPPING: '🛍️',
  EDUCATION: '📚', OTHER: '📦',
};

const COLORS = ['#7c3aed', '#8b5cf6', '#a78bfa', '#c4b5fd', '#ddd6fe'];
const BG_COLORS = ['#f5f3ff', '#f5f3ff', '#f5f3ff', '#f5f3ff', '#f5f3ff'];

export default function Top5Expenses({ data }) {
  if (!data || data.length === 0) {
    return (
      <div className="card">
        <div className="card-header"><span className="card-title">🏆 Top 5 Expenses Overall</span></div>
        <div className="card-body" style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem', fontSize: '0.875rem' }}>
          No expense data yet
        </div>
      </div>
    );
  }

  const top5 = data.slice(0, 5);
  const max = Math.max(...top5.map(d => Number(d.total)));
  const total = top5.reduce((s, d) => s + Number(d.total), 0);

  return (
    <div className="card">
      <div className="card-header">
        <span className="card-title">🏆 Top 5 Expenses Overall</span>
        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
          All time · <strong style={{ color: 'var(--text-primary)' }}>${total.toFixed(0)}</strong> total
        </span>
      </div>
      <div className="card-body" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {top5.map((item, i) => {
          const amount = Number(item.total);
          const pct = (amount / max) * 100;
          const label = item.category.charAt(0) + item.category.slice(1).toLowerCase();
          const icon = CATEGORY_ICONS[item.category] || '📦';
          return (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              {/* Rank */}
              <div style={{
                width: 24, height: 24, borderRadius: '50%',
                background: COLORS[i], color: '#fff',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '0.7rem', fontWeight: '700', flexShrink: 0,
              }}>
                {i + 1}
              </div>

              {/* Icon + label */}
              <div style={{ width: 100, display: 'flex', alignItems: 'center', gap: '0.4rem', flexShrink: 0 }}>
                <span style={{ fontSize: '1rem' }}>{icon}</span>
                <span style={{ fontSize: '0.82rem', fontWeight: '600', color: 'var(--text-primary)' }}>{label}</span>
              </div>

              {/* Bar */}
              <div style={{ flex: 1, background: '#f3f4f6', borderRadius: 99, height: 10, overflow: 'hidden' }}>
                <div style={{
                  width: `${pct}%`, height: '100%',
                  background: COLORS[i],
                  borderRadius: 99,
                  transition: 'width 0.6s ease',
                }} />
              </div>

              {/* Amount */}
              <div style={{ width: 70, textAlign: 'right', fontSize: '0.82rem', fontWeight: '700', color: COLORS[i], flexShrink: 0 }}>
                ${amount.toFixed(0)}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
