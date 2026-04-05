import React from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';
import { formatCurrency } from '../utils/formatCurrency';

const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    return (
      <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px', padding: '0.6rem 1rem', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}>
        <p style={{ fontWeight: '700', color: '#374151', marginBottom: '0.25rem', fontSize: '0.82rem' }}>Day {label}</p>
        <p style={{ color: '#ef4444', fontSize: '0.875rem', fontWeight: '600' }}>{formatCurrency(payload[0].value)}</p>
      </div>
    );
  }
  return null;
};

export default function DailySpendingChart({ data, year, month }) {
  const daysInMonth = new Date(year, month, 0).getDate();
  const fullData = Array.from({ length: daysInMonth }, (_, i) => {
    const day = i + 1;
    const found = data?.find(d => d.day === day);
    return { day, amount: found ? parseFloat(found.amount) : 0 };
  });

  const hasData = fullData.some(d => d.amount > 0);

  if (!hasData) {
    return (
      <div className="chart-card" style={{ minHeight: '240px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
        <div className="chart-empty">
          <div className="chart-empty-icon">📊</div>
          <div style={{ fontWeight: '700', color: '#374151', marginBottom: '0.25rem', fontSize: '0.9rem' }}>Daily Spending</div>
          <div className="chart-empty-text">No expenses recorded this month</div>
        </div>
      </div>
    );
  }

  return (
    <div className="chart-card">
      <div className="chart-card-title">📊 Daily Spending</div>
      <ResponsiveContainer width="100%" height={220}>
        <BarChart data={fullData} margin={{ top: 5, right: 10, left: 0, bottom: 5 }}>
          <defs>
            <linearGradient id="barGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#818cf8" />
              <stop offset="100%" stopColor="#6366f1" />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" vertical={false} />
          <XAxis dataKey="day" tick={{ fontSize: 11, fill: '#9ca3af' }} interval={4} axisLine={false} tickLine={false} />
          <YAxis tick={{ fontSize: 11, fill: '#9ca3af' }} tickFormatter={(v) => `$${v}`} width={50} axisLine={false} tickLine={false} />
          <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(99,102,241,0.05)' }} />
          <Bar dataKey="amount" fill="url(#barGrad)" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
