import React from 'react';
import { render, screen } from '@testing-library/react';
import CategoryBreakdown from '../../components/CategoryBreakdown';

describe('CategoryBreakdown', () => {
  const expenses = [
    { category: 'FOOD', total: '500.00' },
    { category: 'TRANSPORT', total: '150.00' },
    { category: 'HOUSING', total: '1000.00' },
  ];

  test('renders all categories', () => {
    render(<CategoryBreakdown expenses={expenses} />);
    expect(screen.getByText('Food')).toBeInTheDocument();
    expect(screen.getByText('Transport')).toBeInTheDocument();
    expect(screen.getByText('Housing')).toBeInTheDocument();
  });

  test('shows formatted amounts', () => {
    render(<CategoryBreakdown expenses={expenses} />);
    expect(screen.getByText('$500.00')).toBeInTheDocument();
    expect(screen.getByText('$150.00')).toBeInTheDocument();
    expect(screen.getByText('$1,000.00')).toBeInTheDocument();
  });

  test('renders title', () => {
    render(<CategoryBreakdown expenses={expenses} title="My Expenses" />);
    expect(screen.getByText('My Expenses')).toBeInTheDocument();
  });

  test('shows empty state when no expenses', () => {
    render(<CategoryBreakdown expenses={[]} />);
    expect(screen.getByText(/No expense data available/i)).toBeInTheDocument();
  });

  test('shows empty state when expenses is null', () => {
    render(<CategoryBreakdown expenses={null} />);
    expect(screen.getByText(/No expense data available/i)).toBeInTheDocument();
  });

  test('sorts categories by amount descending', () => {
    render(<CategoryBreakdown expenses={expenses} />);
    const amounts = screen.getAllByText(/\$\d/);
    // Housing (1000) should appear before Food (500) before Transport (150)
    const texts = amounts.map(el => el.textContent);
    const housingIdx = texts.findIndex(t => t.includes('1,000'));
    const foodIdx = texts.findIndex(t => t.includes('500'));
    const transportIdx = texts.findIndex(t => t.includes('150'));
    expect(housingIdx).toBeLessThan(foodIdx);
    expect(foodIdx).toBeLessThan(transportIdx);
  });
});
