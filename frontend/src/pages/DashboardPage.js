import React, { useState, useEffect, useCallback } from 'react';
import Navbar from '../components/Navbar';
import SummaryCards from '../components/SummaryCards';
import CategoryPieChart from '../components/CategoryPieChart';
import Top5Expenses from '../components/Top5Expenses';
import MonthlySpendingChart from '../components/MonthlySpendingChart';
import CategoryBreakdown from '../components/CategoryBreakdown';
import TransactionList from '../components/TransactionList';
import TransactionForm from '../components/TransactionForm';
import TopSpending from '../components/TopSpending';
import AiChat from '../components/AiChat';
import { getSummary, getTransactions, createTransaction, updateTransaction, deleteTransaction, getMonthlySpending, getTopExpenses } from '../api/transactionApi';
import { MONTHS, getCurrentYear, getCurrentMonth, getYearOptions } from '../utils/dateHelpers';
import { useAuth } from '../context/AuthContext';

export default function DashboardPage() {
  const [year, setYear] = useState(getCurrentYear());
  const [month, setMonth] = useState(getCurrentMonth());
  const [summary, setSummary] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [monthlySpending, setMonthlySpending] = useState([]);
  const [topExpenses, setTopExpenses] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editTx, setEditTx] = useState(null);
  const [loading, setLoading] = useState(true);
  const { auth } = useAuth();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [summaryRes, txRes, monthlyRes, topRes] = await Promise.all([
        getSummary(year, month),
        getTransactions(year, month),
        getMonthlySpending(year),
        getTopExpenses(),
      ]);
      setSummary(summaryRes.data);
      setTransactions(txRes.data);
      setMonthlySpending(monthlyRes.data);
      setTopExpenses(topRes.data);
    } catch (err) {
      console.error('Failed to fetch dashboard data', err);
    } finally {
      setLoading(false);
    }
  }, [year, month]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleAdd = async (data) => { await createTransaction(data); setShowForm(false); await fetchData(); };
  const handleEdit = (tx) => { setEditTx(tx); setShowForm(true); };
  const handleUpdate = async (data) => { await updateTransaction(editTx.id, data); setEditTx(null); setShowForm(false); await fetchData(); };
  const handleDelete = async (id) => {
    if (window.confirm('Delete this transaction?')) { await deleteTransaction(id); await fetchData(); }
  };

  const greeting = () => {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 17) return 'Good afternoon';
    return 'Good evening';
  };

  return (
    <div className="page">
      <Navbar />
      <div className="page-content">
        {/* Header */}
        <div className="page-header">
          <div>
            <h1 className="page-title">{greeting()}, {auth?.username} 👋</h1>
            <div className="page-subtitle">{MONTHS[month - 1]} {year} overview</div>
          </div>
          <div className="controls">
            <select className="ctrl-select" value={month} onChange={e => setMonth(Number(e.target.value))}>
              {MONTHS.map((m, i) => <option key={i + 1} value={i + 1}>{m}</option>)}
            </select>
            <select className="ctrl-select" value={year} onChange={e => setYear(Number(e.target.value))}>
              {getYearOptions().map(y => <option key={y} value={y}>{y}</option>)}
            </select>
            <button className="btn-add" onClick={() => { setEditTx(null); setShowForm(true); }}>
              + Add Transaction
            </button>
          </div>
        </div>

        {loading ? (
          <div className="loading-state">
            <div className="spinner" />
            <span>Loading your data…</span>
          </div>
        ) : (
          <>
            {/* Summary Cards */}
            <SummaryCards summary={summary} />

            {/* Top Spending — full width */}
            <TopSpending expenses={summary?.expensesByCategory} />

            {/* Monthly Spending — full width */}
            <MonthlySpendingChart data={monthlySpending} year={year} />

            {/* AI Chat */}
            <AiChat />

            {/* Recent Transactions */}
            <div style={{ marginTop: '1.5rem' }}>
              <div className="section-title">
                Recent Transactions
                <span className="section-title-count">({transactions.length} this month)</span>
              </div>
              <TransactionList
                transactions={transactions.slice(0, 8)}
                onEdit={handleEdit}
                onDelete={handleDelete}
              />
            </div>
          </>
        )}
      </div>

      {showForm && (
        <TransactionForm
          onSubmit={editTx ? handleUpdate : handleAdd}
          onClose={() => { setShowForm(false); setEditTx(null); }}
          initialData={editTx}
        />
      )}
    </div>
  );
}
