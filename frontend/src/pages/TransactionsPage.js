import React, { useState, useEffect, useCallback } from 'react';
import Navbar from '../components/Navbar';
import TransactionList from '../components/TransactionList';
import TransactionForm from '../components/TransactionForm';
import { getTransactions, createTransaction, updateTransaction, deleteTransaction } from '../api/transactionApi';
import { MONTHS, getCurrentYear, getCurrentMonth, getYearOptions } from '../utils/dateHelpers';

export default function TransactionsPage() {
  const [year, setYear] = useState(getCurrentYear());
  const [month, setMonth] = useState(getCurrentMonth());
  const [showAll, setShowAll] = useState(false);
  const [transactions, setTransactions] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editTx, setEditTx] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getTransactions(showAll ? null : year, showAll ? null : month);
      setTransactions(res.data);
    } catch (err) {
      console.error('Failed to fetch transactions', err);
    } finally {
      setLoading(false);
    }
  }, [year, month, showAll]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleAdd = async (data) => { await createTransaction(data); fetchData(); };
  const handleEdit = (tx) => { setEditTx(tx); setShowForm(true); };
  const handleUpdate = async (data) => { await updateTransaction(editTx.id, data); setEditTx(null); fetchData(); };
  const handleDelete = async (id) => {
    if (window.confirm('Delete this transaction?')) { await deleteTransaction(id); fetchData(); }
  };

  return (
    <div className="page">
      <Navbar />
      <div className="page-content" style={{ maxWidth: '1000px' }}>
        <div className="page-header">
          <div>
            <h1 className="page-title">Transactions</h1>
            <div className="page-subtitle">
              {showAll ? 'Showing all transactions' : `${MONTHS[month - 1]} ${year}`}
            </div>
          </div>
          <div className="controls">
            {!showAll && (
              <>
                <select className="ctrl-select" value={month} onChange={e => setMonth(Number(e.target.value))}>
                  {MONTHS.map((m, i) => <option key={i + 1} value={i + 1}>{m}</option>)}
                </select>
                <select className="ctrl-select" value={year} onChange={e => setYear(Number(e.target.value))}>
                  {getYearOptions().map(y => <option key={y} value={y}>{y}</option>)}
                </select>
              </>
            )}
            <button
              className={`btn-outline${showAll ? ' active' : ''}`}
              onClick={() => setShowAll(!showAll)}
            >
              {showAll ? 'Filter by Month' : 'Show All'}
            </button>
            <button className="btn-add" onClick={() => { setEditTx(null); setShowForm(true); }}>
              + Add Transaction
            </button>
          </div>
        </div>

        {loading ? (
          <div className="loading-state">
            <div className="spinner" />
            <span>Loading transactions…</span>
          </div>
        ) : (
          <TransactionList
            transactions={transactions}
            onEdit={handleEdit}
            onDelete={handleDelete}
            title={`${transactions.length} transaction${transactions.length !== 1 ? 's' : ''}`}
          />
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
