import type { ZodType } from "zod";
import { ZodError } from "zod";
import { badRequest } from "./errors.js";

export function parseWithSchema<T>(schema: ZodType<T>, value: unknown, message: string): T {
  const result = schema.safeParse(value);

  if (!result.success) {
    throw badRequest(message, flattenZodError(result.error));
  }

  return result.data;
}

export function flattenZodError(error: ZodError): Record<string, unknown> {
  return {
    issues: error.issues.map((issue) => ({
      path: issue.path.join("."),
      message: issue.message,
      code: issue.code
    }))
  };
}
