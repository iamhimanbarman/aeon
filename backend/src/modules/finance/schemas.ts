import { z } from "zod";
import {
  currencySchema,
  monthSchema,
  moneyInputSchema,
  optionalTrimmedStringSchema,
  tagsSchema,
  timestampSchema,
  trimmedStringSchema
} from "../shared/schemas.js";

export const financeAccountTypeSchema = z.enum(["cash", "bank", "wallet", "upi"]);
export const financeTransactionTypeSchema = z.enum(["expense", "income", "transfer"]);
export const financeCategoryScopeSchema = z.enum(["expense", "income"]);
export const financeCategoryFamilySchema = z.enum([
  "core",
  "food",
  "transport",
  "money",
  "home",
  "health",
  "growth",
  "lifestyle"
]);

export const financeCategoryInputSchema = z.object({
  id: z.string().trim().min(1).optional(),
  label: trimmedStringSchema.max(120),
  iconKey: trimmedStringSchema.max(120),
  familyKey: financeCategoryFamilySchema.default("core"),
  scope: financeCategoryScopeSchema.default("expense")
});

export const financeAccountInputSchema = z.object({
  id: z.string().trim().min(1).optional(),
  name: trimmedStringSchema.max(120),
  accountType: financeAccountTypeSchema.default("cash"),
  currency: currencySchema.default("INR"),
  openingBalance: moneyInputSchema.default("0")
});

export const financeTransactionInputSchema = z.object({
  id: z.string().trim().min(1).optional(),
  accountId: z.string().trim().min(1).optional(),
  transactionType: financeTransactionTypeSchema.default("expense"),
  title: trimmedStringSchema.max(200),
  merchant: optionalTrimmedStringSchema,
  category: trimmedStringSchema.max(120),
  amount: moneyInputSchema,
  currency: currencySchema.default("INR"),
  paymentMethod: optionalTrimmedStringSchema,
  note: optionalTrimmedStringSchema,
  tags: tagsSchema,
  receiptUri: optionalTrimmedStringSchema,
  occurredAt: timestampSchema.default(new Date().toISOString())
});

export const financeBudgetAllocationInputSchema = z.object({
  category: trimmedStringSchema.max(120),
  amount: moneyInputSchema
});

export const financeSetMonthBudgetSchema = z.object({
  month: monthSchema,
  totalBudget: moneyInputSchema.optional(),
  currency: currencySchema.default("INR"),
  alertThreshold: z.number().min(0.1).max(1).default(0.8),
  categoryAllocations: z.array(financeBudgetAllocationInputSchema).default([])
});

export const financeTransactionQuerySchema = z.object({
  transactionType: financeTransactionTypeSchema.optional(),
  category: z.string().trim().min(1).optional(),
  accountId: z.string().trim().min(1).optional(),
  from: timestampSchema.optional(),
  to: timestampSchema.optional(),
  day: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
  month: monthSchema.optional(),
  updatedAfter: timestampSchema.optional(),
  limit: z.coerce.number().int().min(1).max(300).default(100)
});

export const financeTransactionMonthsQuerySchema = z.object({
  transactionType: financeTransactionTypeSchema.default("expense"),
  category: z.string().trim().min(1).optional()
});

export const financeBudgetQuerySchema = z.object({
  month: monthSchema.optional(),
  updatedAfter: timestampSchema.optional()
});
