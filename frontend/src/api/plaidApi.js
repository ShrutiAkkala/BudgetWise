import axiosInstance from './axiosInstance';

export const createLinkToken = () =>
  axiosInstance.post('/api/plaid/link-token');

export const exchangePublicToken = (data) =>
  axiosInstance.post('/api/plaid/exchange-token', data);

export const getConnectedAccounts = () =>
  axiosInstance.get('/api/plaid/accounts');

export const disconnectAccount = (id) =>
  axiosInstance.delete(`/api/plaid/accounts/${id}`);

export const syncTransactions = () =>
  axiosInstance.post('/api/plaid/sync');
