import { z } from "zod";

const moneyPattern = /^\d+(\.\d{1,2})?$/;

export const trimmedStringSchema = z.string().trim().min(1);
export const optionalTrimmedStringSchema = z
  .string()
  .trim()
  .min(1)
  .optional()
  .or(z.literal("").transform(() => undefined));

export const currencySchema = z.string().trim().regex(/^[A-Z]{3}$/);
export const dateSchema = z.string().regex(/^\d{4}-\d{2}-\d{2}$/);
export const monthSchema = z.string().regex(/^\d{4}-\d{2}$/);
export const timestampSchema = z.string().datetime({ offset: true });
export const tagsSchema = z.array(z.string().trim().min(1).max(40)).max(30).default([]);

export const moneyInputSchema = z
  .union([
    z.string().trim().regex(moneyPattern),
    z.number().finite().nonnegative().transform((value) => value.toFixed(2))
  ])
  .transform((value) => value.toString());

export const uuidSchema = z.string().uuid();
