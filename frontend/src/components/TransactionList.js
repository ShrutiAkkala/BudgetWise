import React from 'react';
import { formatCurrency } from '../utils/formatCurrency';
import { formatDate } from '../utils/dateHelpers';

const CATEGORY_ICONS = {
  FOOD: '🍔', TRANSPORT: '🚗', HOUSING: '🏠', ENTERTAINMENT: '🎬',
  HEALTH: '💊', SHOPPING: '🛒', EDUCATION: '📚', OTHER: '📦',
};

export default function TransactionList({ transactions, onEdit, onDelete, title }) {
  if (!transactions || transactions.length === 0) {
    return (
      <div className="tx-list">
        <div className="tx-empty">
          <div className="tx-empty-icon">📭</div>
          <div className="tx-empty-text">No transactions found</div>
        </div>
      </div>
    );
  }

  return (
    <div className="tx-list">
      {title && (
        <div className="tx-list-header">
          <span className="tx-list-title">{title}</span>
          <span className="tx-count-badge">{transactions.length}</span>
        </div>
      )}
      {transactions.map(tx => (
        <div key={tx.id} className="tx-row">
          <div className="tx-icon-wrap">
            {CATEGORY_ICONS[tx.category] || '📦'}
          </div>

          <div className="tx-info">
            <div className="tx-desc">
              {tx.description}
              {tx.plaidImported && <span className="plaid-badge">Auto-imported</span>}
            </div>
            <div className="tx-meta">
              <span className="tx-category-tag">
                {tx.category.charAt(0) + tx.category.slice(1).toLowerCase()}
              </span>
              {' · '}
              {formatDate(tx.date)}
            </div>
          </div>

          <div className={`tx-amount ${tx.type === 'INCOME' ? 'income' : 'expense'}`}>
            {tx.type === 'INCOME' ? '+' : '−'}{formatCurrency(tx.amount)}
          </div>

          <div className="tx-actions">
            <button className="btn-icon" onClick={() => onEdit(tx)} title="Edit">✏️</button>
            <button className="btn-icon danger" onClick={() => onDelete(tx.id)} title="Delete">🗑</button>
          </div>
        </div>
      ))}
    </div>
  );
}
