import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import TransactionForm from '../../components/TransactionForm';

describe('TransactionForm', () => {
  const onSubmit = jest.fn().mockResolvedValue(undefined);
  const onClose = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders form fields', () => {
    render(<TransactionForm onSubmit={onSubmit} onClose={onClose} initialData={null} />);
    expect(screen.getByLabelText(/Amount/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Description/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Type/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Category/i)).toBeInTheDocument();
  });

  test('shows Add title for new transaction', () => {
    render(<TransactionForm onSubmit={onSubmit} onClose={onClose} initialData={null} />);
    expect(screen.getByText('Add Transaction')).toBeInTheDocument();
  });

  test('shows Edit title when initialData provided', () => {
    const initialData = {
      amount: 50, description: 'Lunch', category: 'FOOD', type: 'EXPENSE', date: '2026-03-10',
    };
    render(<TransactionForm onSubmit={onSubmit} onClose={onClose} initialData={initialData} />);
    expect(screen.getByText('Edit Transaction')).toBeInTheDocument();
  });

  test('pre-fills fields from initialData', () => {
    const initialData = {
      amount: 50, description: 'Lunch', category: 'FOOD', type: 'EXPENSE', date: '2026-03-10',
    };
    render(<TransactionForm onSubmit={onSubmit} onClose={onClose} initialData={initialData} />);
    expect(screen.getByDisplayValue('Lunch')).toBeInTheDocument();
    expect(screen.getByDisplayValue('50')).toBeInTheDocument();
  });

  test('calls onClose when Cancel is clicked', () => {
    render(<TransactionForm onSubmit={onSubmit} onClose={onClose} initialData={null} />);
    fireEvent.click(screen.getByText('Cancel'));
    expect(onClose).toHaveBeenCalled();
  });

  test('shows validation error when fields are empty', async () => {
    render(<TransactionForm onSubmit={onSubmit} onClose={onClose} initialData={null} />);
    fireEvent.click(screen.getByText('Save Transaction'));
    await waitFor(() => {
      expect(screen.getByText(/All fields required/i)).toBeInTheDocument();
    });
  });

  test('calls onSubmit with form data when valid', async () => {
    render(<TransactionForm onSubmit={onSubmit} onClose={onClose} initialData={null} />);

    fireEvent.change(screen.getByLabelText(/Amount/i), { target: { value: '75' } });
    fireEvent.change(screen.getByLabelText(/Description/i), { target: { value: 'Dinner' } });

    fireEvent.click(screen.getByText('Save Transaction'));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
        amount: 75,
        description: 'Dinner',
      }));
    });
  });
});
