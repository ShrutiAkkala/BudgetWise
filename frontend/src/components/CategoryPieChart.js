import React from 'react';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { formatCurrency } from '../utils/formatCurrency';

const COLORS = ['#6366f1','#f59e0b','#10b981','#ef4444','#3b82f6','#8b5cf6','#ec4899','#14b8a6'];

export default function CategoryPieChart({ data, title }) {
  if (!data || data.length === 0) {
    return (
      <div className="chart-card">
        <div className="chart-card-title">{title}</div>
        <div className="chart-empty" style={{ padding: '3rem 0' }}>
          <div className="chart-empty-icon">🥧</div>
          <div className="chart-empty-text">No data available</div>
        </div>
      </div>
    );
  }

  const chartData = data.map(item => ({
    name: item.category.charAt(0) + item.category.slice(1).toLowerCase(),
    value: parseFloat(item.total),
  }));

  return (
    <div className="chart-card">
      <div className="chart-card-title">{title}</div>
      <ResponsiveContainer width="100%" height={280}>
        <PieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            outerRadius={100}
            innerRadius={40}
            dataKey="value"
            paddingAngle={2}
          >
            {chartData.map((_, index) => (
              <Cell key={index} fill={COLORS[index % COLORS.length]} stroke="none" />
            ))}
          </Pie>
          <Tooltip formatter={(value) => formatCurrency(value)} />
          <Legend iconType="circle" iconSize={8} formatter={(value) => <span style={{ fontSize: '0.78rem', color: '#374151' }}>{value}</span>} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
