export const MONTHS = [
  'January','February','March','April','May','June',
  'July','August','September','October','November','December'
];

export const getCurrentYear = () => new Date().getFullYear();
export const getCurrentMonth = () => new Date().getMonth() + 1;

export const getYearOptions = () => {
  const current = getCurrentYear();
  return [current - 2, current - 1, current, current + 1];
};

export const formatDate = (dateStr) => {
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
};
