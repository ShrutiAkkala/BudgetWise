import React from 'react';
import {
  ComposedChart, Bar, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Cell, ReferenceLine, Legend
} from 'recharts';

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                     'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function getColor(amount, max) {
  if (amount === 0) return '#e5e7eb';
  const ratio = amount / max;
  if (ratio >= 0.8) return '#7c3aed';
  if (ratio >= 0.6) return '#8b5cf6';
  if (ratio >= 0.4) return '#a78bfa';
  if (ratio >= 0.2) return '#c4b5fd';
  return '#ddd6fe';
}

const CustomTooltip = ({ active, payload, label, average }) => {
  if (active && payload && payload.length) {
    const amount = payload[0]?.value || 0;
    const diff = amount - average;
    return (
      <div style={{
        background: '#fff', border: '1px solid var(--border)',
        borderRadius: 'var(--radius-sm)', padding: '0.75rem 1rem',
        boxShadow: 'var(--shadow)', fontSize: '0.82rem', minWidth: 160,
      }}>
        <div style={{ fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.4rem', fontSize: '0.9rem' }}>
          {label}
        </div>
        <div style={{ color: '#7c3aed', marginBottom: '0.25rem' }}>
          Spent: <strong>${amount.toFixed(2)}</strong>
        </div>
        <div style={{ color: 'var(--text-muted)', marginBottom: '0.25rem' }}>
          Avg: <strong>${average.toFixed(2)}</strong>
        </div>
        {amount > 0 && (
          <div style={{ color: diff > 0 ? '#ef4444' : '#16a34a', fontWeight: '600', marginTop: '0.3rem' }}>
            {diff > 0 ? `▲ $${diff.toFixed(2)} over avg` : `▼ $${Math.abs(diff).toFixed(2)} under avg`}
          </div>
        )}
      </div>
    );
  }
  return null;
};

const CustomLegend = ({ average }) => (
  <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', justifyContent: 'center', marginTop: '0.5rem', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
      <div style={{ width: 12, height: 12, borderRadius: 3, background: 'linear-gradient(135deg, #7c3aed, #c4b5fd)' }} />
      <span>Monthly Expenses</span>
    </div>
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
      <div style={{ width: 18, height: 2, background: '#f59e0b', borderTop: '2px dashed #f59e0b' }} />
      <span>Avg ${average.toFixed(0)}/mo</span>
    </div>
  </div>
);

export default function MonthlySpendingChart({ data, year }) {
  const chartData = MONTH_NAMES.map((name, i) => {
    const found = data?.find(d => d.day === i + 1);
    return { month: name, amount: found ? Number(found.amount) : 0 };
  });

  const nonZero = chartData.filter(d => d.amount > 0);
  const total = nonZero.reduce((sum, d) => sum + d.amount, 0);
  const average = nonZero.length > 0 ? total / nonZero.length : 0;
  const max = Math.max(...chartData.map(d => d.amount), 1);
  const hasData = nonZero.length > 0;

  return (
    <div className="card">
      <div className="card-header">
        <span className="card-title">📅 Monthly Spending</span>
        {hasData && (
          <div style={{ display: 'flex', gap: '1rem', fontSize: '0.78rem' }}>
            <span style={{ color: 'var(--text-muted)' }}>
              Total: <strong style={{ color: 'var(--text-primary)' }}>${total.toFixed(0)}</strong>
            </span>
            <span style={{ color: 'var(--text-muted)' }}>
              Avg: <strong style={{ color: '#f59e0b' }}>${average.toFixed(0)}/mo</strong>
            </span>
          </div>
        )}
      </div>
      <div className="card-body">
        {!hasData ? (
          <div style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem', fontSize: '0.875rem' }}>
            No expense data for {year}
          </div>
        ) : (
          <>
            <ResponsiveContainer width="100%" height={230}>
              <ComposedChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-light)" vertical={false} />
                <XAxis
                  dataKey="month"
                  tick={{ fontSize: 11, fill: 'var(--text-muted)' }}
                  axisLine={false} tickLine={false}
                />
                <YAxis
                  tick={{ fontSize: 11, fill: 'var(--text-muted)' }}
                  axisLine={false} tickLine={false}
                  tickFormatter={v => `$${v}`}
                  width={55}
                />
                <Tooltip content={<CustomTooltip average={average} />} cursor={{ fill: 'rgba(99,102,241,0.05)' }} />
                <ReferenceLine
                  y={average}
                  stroke="#f59e0b"
                  strokeDasharray="6 3"
                  strokeWidth={2}
                  label={{
                    value: `avg $${average.toFixed(0)}`,
                    position: 'insideTopRight',
                    fill: '#f59e0b',
                    fontSize: 10,
                    fontWeight: 600,
                  }}
                />
                <Bar dataKey="amount" radius={[6, 6, 0, 0]} maxBarSize={44}>
                  {chartData.map((d, i) => (
                    <Cell key={i} fill={getColor(d.amount, max)} />
                  ))}
                </Bar>
                <Line
                  type="monotone"
                  dataKey="amount"
                  stroke="#7c3aed"
                  strokeWidth={2}
                  dot={{ fill: '#7c3aed', r: 3, strokeWidth: 0 }}
                  activeDot={{ r: 5, fill: '#7c3aed' }}
                  connectNulls={false}
                />
              </ComposedChart>
            </ResponsiveContainer>
            <CustomLegend average={average} />
          </>
        )}
      </div>
    </div>
  );
}
