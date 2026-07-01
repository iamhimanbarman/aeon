import { badRequest } from "./errors.js";

export function parseMonthKey(value: string): { monthKey: string; start: string; end: string } {
  if (!/^\d{4}-\d{2}$/.test(value)) {
    throw badRequest("Month must be in YYYY-MM format.");
  }

  const [yearText, monthText] = value.split("-");
  const year = Number(yearText);
  const month = Number(monthText);

  if (!Number.isInteger(year) || !Number.isInteger(month) || month < 1 || month > 12) {
    throw badRequest("Month must be in YYYY-MM format.");
  }

  const startDate = new Date(Date.UTC(year, month - 1, 1));
  const endDate = new Date(Date.UTC(year, month, 0));

  return {
    monthKey: value,
    start: startDate.toISOString().slice(0, 10),
    end: endDate.toISOString().slice(0, 10)
  };
}

export function parseDateOnly(value: string): string {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    throw badRequest("Date must be in YYYY-MM-DD format.");
  }

  return value;
}
