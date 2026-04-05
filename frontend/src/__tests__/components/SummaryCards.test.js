import React from 'react';
import { render, screen } from '@testing-library/react';
import SummaryCards from '../../components/SummaryCards';

describe('SummaryCards', () => {
  const summary = {
    totalIncome: 3000,
    totalExpenses: 1200,
    netBalance: 1800,
  };

  test('renders all three cards', () => {
    render(<SummaryCards summary={summary} />);
    expect(screen.getByText(/Total Income/i)).toBeInTheDocument();
    expect(screen.getByText(/Total Expenses/i)).toBeInTheDocument();
    expect(screen.getByText(/Net Balance/i)).toBeInTheDocument();
  });

  test('shows correct income amount', () => {
    render(<SummaryCards summary={summary} />);
    expect(screen.getByText('$3,000.00')).toBeInTheDocument();
  });

  test('shows correct expenses amount', () => {
    render(<SummaryCards summary={summary} />);
    expect(screen.getByText('$1,200.00')).toBeInTheDocument();
  });

  test('shows Surplus when net balance is positive', () => {
    render(<SummaryCards summary={summary} />);
    expect(screen.getByText('Surplus')).toBeInTheDocument();
  });

  test('shows Deficit when net balance is negative', () => {
    render(<SummaryCards summary={{ ...summary, netBalance: -200 }} />);
    expect(screen.getByText('Deficit')).toBeInTheDocument();
  });

  test('renders nothing when summary is null', () => {
    const { container } = render(<SummaryCards summary={null} />);
    expect(container).toBeEmptyDOMElement();
  });
});
