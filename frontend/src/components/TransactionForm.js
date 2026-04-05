import React, { useState, useEffect } from 'react';

const CATEGORIES = ['FOOD','TRANSPORT','HOUSING','ENTERTAINMENT','HEALTH','SHOPPING','EDUCATION','OTHER'];
const today = new Date().toISOString().split('T')[0];

export default function TransactionForm({ onSubmit, onClose, initialData }) {
  const [form, setForm] = useState({
    amount: '', description: '', category: 'FOOD', type: 'EXPENSE', date: today,
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (initialData) {
      setForm({
        amount: initialData.amount,
        description: initialData.description,
        category: initialData.category,
        type: initialData.type,
        date: initialData.date,
      });
    }
  }, [initialData]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.amount || !form.description) { setError('All fields are required'); return; }
    if (parseFloat(form.amount) < 0.1) { setError('Amount must be at least $0.10'); return; }
    setLoading(true);
    setError('');
    try {
      await onSubmit({ ...form, amount: parseFloat(form.amount) });
      onClose();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to save transaction');
    } finally {
      setLoading(false);
    }
  };

  const isEdit = !!initialData;

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <h2 className="modal-title">
          {isEdit ? '✏️ Edit Transaction' : '➕ Add Transaction'}
        </h2>

        {error && <div className="modal-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="modal-row">
            <div className="modal-field">
              <label className="modal-label">Type</label>
              <select className="modal-select" name="type" value={form.type} onChange={handleChange}>
                <option value="EXPENSE">💸 Expense</option>
                <option value="INCOME">💵 Income</option>
              </select>
            </div>
            <div className="modal-field">
              <label className="modal-label">Category</label>
              <select className="modal-select" name="category" value={form.category} onChange={handleChange}>
                {CATEGORIES.map(c => (
                  <option key={c} value={c}>{c.charAt(0) + c.slice(1).toLowerCase()}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="modal-field">
            <label className="modal-label">Amount ($)</label>
            <input
              className="modal-input"
              type="number"
              name="amount"
              step="0.01"
              min="0.10"
              value={form.amount}
              onChange={handleChange}
              placeholder="0.00"
              required
            />
          </div>

          <div className="modal-field">
            <label className="modal-label">Description</label>
            <input
              className="modal-input"
              type="text"
              name="description"
              value={form.description}
              onChange={handleChange}
              placeholder="e.g. Grocery shopping"
              required
            />
          </div>

          <div className="modal-field">
            <label className="modal-label">Date</label>
            <input
              className="modal-input"
              type="date"
              name="date"
              value={form.date}
              onChange={handleChange}
              required
            />
          </div>

          <div className="modal-btn-row">
            <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-save" disabled={loading}>
              {loading ? 'Saving…' : isEdit ? 'Save Changes' : 'Add Transaction'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
