import { MONTHS, getYearOptions, getCurrentYear, getCurrentMonth, formatDate } from '../../utils/dateHelpers';

describe('dateHelpers', () => {
  test('MONTHS has 12 entries', () => {
    expect(MONTHS).toHaveLength(12);
    expect(MONTHS[0]).toBe('January');
    expect(MONTHS[11]).toBe('December');
  });

  test('getCurrentYear returns current year', () => {
    expect(getCurrentYear()).toBe(new Date().getFullYear());
  });

  test('getCurrentMonth returns 1-12', () => {
    const month = getCurrentMonth();
    expect(month).toBeGreaterThanOrEqual(1);
    expect(month).toBeLessThanOrEqual(12);
  });

  test('getYearOptions includes current year', () => {
    const options = getYearOptions();
    expect(options).toContain(new Date().getFullYear());
    expect(options).toHaveLength(4);
  });

  test('formatDate formats a date string', () => {
    const formatted = formatDate('2026-03-15');
    expect(formatted).toContain('2026');
    expect(formatted).toContain('15');
  });
});
