import { formatCurrency } from '../../utils/formatCurrency';

describe('formatCurrency', () => {
  test('formats positive number as USD currency', () => {
    expect(formatCurrency(1500)).toBe('$1,500.00');
  });

  test('formats zero correctly', () => {
    expect(formatCurrency(0)).toBe('$0.00');
  });

  test('formats decimal amounts', () => {
    expect(formatCurrency(49.99)).toBe('$49.99');
  });

  test('handles null/undefined gracefully by using 0', () => {
    expect(formatCurrency(null)).toBe('$0.00');
    expect(formatCurrency(undefined)).toBe('$0.00');
  });

  test('formats large numbers with commas', () => {
    expect(formatCurrency(10000)).toBe('$10,000.00');
  });
});
