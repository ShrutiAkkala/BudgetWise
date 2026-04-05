import React from 'react';
import { formatCurrency } from '../utils/formatCurrency';

export default function SummaryCards({ summary }) {
  if (!summary) return null;

  const balanceClass = summary.netBalance >= 0 ? 'balance-positive' : 'balance-negative';

  return (
    <div className="summary-grid">
      <div className="summary-card income">
        <span className="summary-card-icon">💵</span>
        <div className="summary-card-label">Total Income</div>
        <div className="summary-card-amount">{formatCurrency(summary.totalIncome)}</div>
        <div className="summary-card-change">This month</div>
      </div>

      <div className="summary-card expense">
        <span className="summary-card-icon">💸</span>
        <div className="summary-card-label">Total Expenses</div>
        <div className="summary-card-amount">{formatCurrency(summary.totalExpenses)}</div>
        <div className="summary-card-change">This month</div>
      </div>

      <div className={`summary-card ${balanceClass}`}>
        <span className="summary-card-icon">{summary.netBalance >= 0 ? '📈' : '📉'}</span>
        <div className="summary-card-label">Net Balance</div>
        <div className="summary-card-amount">{formatCurrency(summary.netBalance)}</div>
        <div className="summary-card-change">{summary.netBalance >= 0 ? 'Surplus' : 'Deficit'}</div>
      </div>
    </div>
  );
}
