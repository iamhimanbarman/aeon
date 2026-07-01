import { z } from "zod";

const emailSchema = z
  .string()
  .trim()
  .email("Enter a valid email address.")
  .transform((value) => value.toLowerCase());

const otpCodeSchema = z
  .string()
  .trim()
  .regex(/^\d{6}$/, "Enter the 6-digit code.");

const passwordSchema = z
  .string()
  .min(10, "Password must be at least 10 characters.")
  .max(128, "Password must be 128 characters or fewer.")
  .refine((value) => /[a-z]/.test(value), "Password must include a lowercase letter.")
  .refine((value) => /[A-Z]/.test(value), "Password must include an uppercase letter.")
  .refine((value) => /\d/.test(value), "Password must include a number.");

export const authOtpRequestSchema = z.object({
  email: emailSchema
});

export const authOtpVerifySchema = z.object({
  email: emailSchema,
  code: otpCodeSchema
});

export const authSignupCompleteSchema = z.object({
  signupToken: z.string().min(1),
  displayName: z
    .string()
    .trim()
    .max(80, "Name must be 80 characters or fewer.")
    .optional()
    .transform((value) => value?.trim() || undefined),
  password: passwordSchema
});

export const authPasswordSignInSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, "Password is required.")
});

export const authRefreshSchema = z.object({
  refreshToken: z.string().min(1, "Refresh token is required.")
});

export const authSignOutSchema = z.object({
  refreshToken: z.string().min(1, "Refresh token is required.")
});

export const authGoogleStartQuerySchema = z.object({
  mobileRedirectUri: z
    .string()
    .url("Invalid mobile redirect URI.")
    .refine((value) => {
      try {
        const url = new URL(value);
        return url.protocol === "aeon:" && url.hostname === "auth";
      } catch {
        return false;
      }
    }, "Unsupported mobile redirect URI.")
});

export const authGoogleExchangeSchema = z.object({
  exchangeCode: z.string().min(1, "Exchange code is required.")
});
