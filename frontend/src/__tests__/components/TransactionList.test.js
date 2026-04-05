import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import TransactionList from '../../components/TransactionList';

describe('TransactionList', () => {
  const transactions = [
    {
      id: 1, description: 'Lunch', amount: 50, category: 'FOOD',
      type: 'EXPENSE', date: '2026-03-10', plaidImported: false,
    },
    {
      id: 2, description: 'Salary', amount: 3000, category: 'OTHER',
      type: 'INCOME', date: '2026-03-01', plaidImported: true,
    },
  ];

  const onEdit = jest.fn();
  const onDelete = jest.fn();

  test('renders all transactions', () => {
    render(<TransactionList transactions={transactions} onEdit={onEdit} onDelete={onDelete} />);
    expect(screen.getByText('Lunch')).toBeInTheDocument();
    expect(screen.getByText('Salary')).toBeInTheDocument();
  });

  test('shows Auto-imported badge for plaid transactions', () => {
    render(<TransactionList transactions={transactions} onEdit={onEdit} onDelete={onDelete} />);
    expect(screen.getByText('Auto-imported')).toBeInTheDocument();
  });

  test('calls onEdit when Edit button clicked', () => {
    render(<TransactionList transactions={transactions} onEdit={onEdit} onDelete={onDelete} />);
    const editButtons = screen.getAllByText('Edit');
    fireEvent.click(editButtons[0]);
    expect(onEdit).toHaveBeenCalledWith(transactions[0]);
  });

  test('calls onDelete when Delete button clicked', () => {
    render(<TransactionList transactions={transactions} onEdit={onEdit} onDelete={onDelete} />);
    const deleteButtons = screen.getAllByText('Delete');
    fireEvent.click(deleteButtons[0]);
    expect(onDelete).toHaveBeenCalledWith(transactions[0].id);
  });

  test('shows empty state when no transactions', () => {
    render(<TransactionList transactions={[]} onEdit={onEdit} onDelete={onDelete} />);
    expect(screen.getByText(/No transactions found/i)).toBeInTheDocument();
  });

  test('shows empty state when transactions is null', () => {
    render(<TransactionList transactions={null} onEdit={onEdit} onDelete={onDelete} />);
    expect(screen.getByText(/No transactions found/i)).toBeInTheDocument();
  });
});
