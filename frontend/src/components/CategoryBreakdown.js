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

export default function CategoryBreakdown({ expenses, title = 'Expense Breakdown' }) {
  if (!expenses || expenses.length === 0) {
    return (
      <div className="breakdown-card">
        <div className="breakdown-title">{title}</div>
        <div className="chart-empty" style={{ padding: '2rem 0' }}>
          <div className="chart-empty-icon">📊</div>
          <div className="chart-empty-text">No expense data available</div>
        </div>
      </div>
    );
  }

  const total = expenses.reduce((sum, e) => sum + parseFloat(e.total), 0);
  const sorted = [...expenses].sort((a, b) => parseFloat(b.total) - parseFloat(a.total));

  return (
    <div className="breakdown-card">
      <div className="breakdown-title">{title}</div>
      {sorted.map(item => {
        const pct = total > 0 ? (parseFloat(item.total) / total) * 100 : 0;
        const color = CATEGORY_COLORS[item.category] || '#6b7280';
        return (
          <div key={item.category} className="breakdown-item">
            <div className="breakdown-item-header">
              <div className="breakdown-item-left">
                <div className="breakdown-item-icon" style={{ background: `${color}18` }}>
                  {CATEGORY_ICONS[item.category] || '📦'}
                </div>
                <span className="breakdown-item-name">
                  {item.category.charAt(0) + item.category.slice(1).toLowerCase()}
                </span>
              </div>
              <div className="breakdown-item-right">
                <span className="breakdown-item-pct">{pct.toFixed(1)}%</span>
                <span className="breakdown-item-amount">{formatCurrency(item.total)}</span>
              </div>
            </div>
            <div className="breakdown-bar-track">
              <div
                className="breakdown-bar-fill"
                style={{ width: `${pct}%`, background: color }}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
}
