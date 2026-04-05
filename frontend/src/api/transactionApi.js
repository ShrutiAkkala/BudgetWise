import axiosInstance from './axiosInstance';

export const getTransactions = (year, month) =>
  axiosInstance.get('/api/transactions', { params: { year, month } });

export const createTransaction = (data) =>
  axiosInstance.post('/api/transactions', data);

export const updateTransaction = (id, data) =>
  axiosInstance.put(`/api/transactions/${id}`, data);

export const deleteTransaction = (id) =>
  axiosInstance.delete(`/api/transactions/${id}`);

export const getSummary = (year, month) =>
  axiosInstance.get('/api/transactions/summary', { params: { year, month } });

export const getDailySpending = (year, month) =>
  axiosInstance.get('/api/transactions/daily', { params: { year, month } });

export const getMonthlySpending = (year) =>
  axiosInstance.get('/api/transactions/monthly', { params: { year } });

export const getTopExpenses = () =>
  axiosInstance.get('/api/transactions/top-expenses');
