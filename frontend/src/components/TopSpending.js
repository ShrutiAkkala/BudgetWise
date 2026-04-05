import React from 'react';
import { formatCurrency } from '../utils/formatCurrency';

const CATEGORY_ICONS = {
  FOOD: '🍔', TRANSPORT: '🚗', HOUSING: '🏠', ENTERTAINMENT: '🎬',
  HEALTH: '💊', SHOPPING: '🛒', EDUCATION: '📚', OTHER: '📦',
};

const CATEGORY_COLORS = {
  FOOD: '#f59e0b', TRANSPORT: '#3b82f6', HOUSING: '#8b5cf6', ENTERTAINMENT: '#ec4899',
  HEALTH: '#10b981', SHOPPING: '#f97316', EDUCATION: '#06b6d4', OTHER: '#6b7280',
};

const RANK_LABELS = ['🥇', '🥈', '🥉'];

export default function TopSpending({ expenses }) {
  if (!expenses || expenses.length === 0) {
    return (
      <div className="chart-card">
        <div className="chart-card-title">📊 Where Your Money Went</div>
        <div className="chart-empty" style={{ padding: '3rem 0' }}>
          <div className="chart-empty-icon">💸</div>
          <div className="chart-empty-text">No expenses this month</div>
        </div>
      </div>
    );
  }

  const sorted = [...expenses]
    .map(e => ({ ...e, total: parseFloat(e.total) }))
    .sort((a, b) => b.total - a.total);

  const max = sorted[0].total;
  const grandTotal = sorted.reduce((s, e) => s + e.total, 0);
  const top = sorted[0];

  return (
    <div className="chart-card">
      <div className="chart-card-title">📊 Where Your Money Went</div>

      {/* Top spender callout */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: '1rem',
        background: `${CATEGORY_COLORS[top.category]}12`,
        border: `1.5px solid ${CATEGORY_COLORS[top.category]}40`,
        borderRadius: '10px', padding: '0.875rem 1rem', marginBottom: '1.25rem',
      }}>
        <span style={{ fontSize: '2rem' }}>{CATEGORY_ICONS[top.category] || '📦'}</span>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: '0.72rem', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.06em', color: '#6b7280', marginBottom: '0.15rem' }}>
            Top Expense
          </div>
          <div style={{ fontWeight: '800', fontSize: '1rem', color: '#111827' }}>
            {top.category.charAt(0) + top.category.slice(1).toLowerCase()}
          </div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontWeight: '800', fontSize: '1.4rem', color: CATEGORY_COLORS[top.category], lineHeight: 1 }}>
            {formatCurrency(top.total)}
          </div>
          <div style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.15rem' }}>
            {((top.total / grandTotal) * 100).toFixed(0)}% of total
          </div>
        </div>
      </div>

      {/* Ranked bars */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
        {sorted.map((item, i) => {
          const pct = (item.total / max) * 100;
          const color = CATEGORY_COLORS[item.category] || '#6b7280';
          return (
            <div key={item.category}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', marginBottom: '0.35rem' }}>
                <span style={{ fontSize: '0.85rem', width: '1.5rem', textAlign: 'center', flexShrink: 0 }}>
                  {RANK_LABELS[i] || `#${i + 1}`}
                </span>
                <span style={{ fontSize: '1rem' }}>{CATEGORY_ICONS[item.category] || '📦'}</span>
                <span style={{ flex: 1, fontSize: '0.85rem', fontWeight: '600', color: '#374151' }}>
                  {item.category.charAt(0) + item.category.slice(1).toLowerCase()}
                </span>
                <span style={{ fontSize: '0.875rem', fontWeight: '700', color: '#111827' }}>
                  {formatCurrency(item.total)}
                </span>
                <span style={{ fontSize: '0.72rem', color: '#9ca3af', width: '2.5rem', textAlign: 'right' }}>
                  {((item.total / grandTotal) * 100).toFixed(0)}%
                </span>
              </div>
              <div style={{ background: '#f3f4f6', borderRadius: '999px', height: '8px', overflow: 'hidden', marginLeft: '3.5rem' }}>
                <div style={{
                  width: `${pct}%`, height: '100%',
                  background: color,
                  borderRadius: '999px',
                  transition: 'width 0.7s cubic-bezier(0.4,0,0.2,1)',
                  opacity: i === 0 ? 1 : 0.75 - i * 0.05,
                }} />
              </div>
            </div>
          );
        })}
      </div>

      {/* Total */}
      <div style={{
        marginTop: '1.25rem', paddingTop: '1rem',
        borderTop: '1px solid #f3f4f6',
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      }}>
        <span style={{ fontSize: '0.82rem', color: '#6b7280', fontWeight: '500' }}>Total Expenses</span>
        <span style={{ fontSize: '1rem', fontWeight: '800', color: '#ef4444' }}>{formatCurrency(grandTotal)}</span>
      </div>
    </div>
  );
}
