import "dotenv/config";
import { z } from "zod";

const optionalUrlEnv = z
  .string()
  .trim()
  .optional()
  .transform((value) => value && value.length > 0 ? value : undefined)
  .pipe(z.string().url().optional());

const optionalSecretEnv = z
  .string()
  .trim()
  .optional()
  .transform((value) => value && value.length > 0 ? value : undefined);

const envSchema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  HOST: z.string().min(1).default("0.0.0.0"),
  PORT: z.coerce.number().int().positive().default(10000),
  LOG_LEVEL: z
    .enum(["fatal", "error", "warn", "info", "debug", "trace", "silent"])
    .default("info"),
  CORS_ORIGIN: z.string().min(1).default("*"),
  DATABASE_URL: z.string().url(),
  SUPABASE_URL: optionalUrlEnv,
  SUPABASE_JWT_AUDIENCE: z.string().min(1).default("authenticated"),
  AUTH_JWT_SECRET: z.string().min(32),
  AUTH_JWT_ISSUER: z.string().min(1).default("aeon-backend"),
  AUTH_JWT_AUDIENCE: z.string().min(1).default("authenticated"),
  ACCESS_TOKEN_TTL_SECONDS: z.coerce.number().int().positive().optional(),
  AUTH_ACCESS_TOKEN_TTL_MINUTES: z.coerce.number().int().positive().default(15),
  AUTH_REFRESH_TOKEN_TTL_DAYS: z.coerce.number().int().positive().default(30),
  AUTH_SIGNUP_PROOF_TTL_MINUTES: z.coerce.number().int().positive().default(15),
  AUTH_REAUTH_TTL_MINUTES: z.coerce.number().int().positive().default(10),
  AUTH_EMAIL_OTP_TTL_MINUTES: z.coerce.number().int().positive().default(10),
  AUTH_EMAIL_OTP_RESEND_SECONDS: z.coerce.number().int().positive().default(60),
  AUTH_EMAIL_OTP_MAX_ATTEMPTS: z.coerce.number().int().positive().default(5),
  AUTH_PASSWORD_MAX_FAILED_ATTEMPTS: z.coerce.number().int().positive().default(5),
  AUTH_PASSWORD_LOCKOUT_MINUTES: z.coerce.number().int().positive().default(15),
  AUTH_TOKEN_HASH_PEPPER: z.string().min(16),
  PASSWORD_PEPPER: optionalSecretEnv,
  OTP_PEPPER: optionalSecretEnv,
  REFRESH_TOKEN_PEPPER: optionalSecretEnv,
  AUTH_ALLOWED_MOBILE_REDIRECT_URIS: z.string().min(1).default("aeon://auth/callback"),
  AUTH_EMAIL_FROM: z.string().min(1),
  EMAIL_FROM: optionalSecretEnv,
  RESEND_API_KEY: z.string().min(1),
  GOOGLE_OAUTH_CLIENT_ID: optionalSecretEnv,
  GOOGLE_OAUTH_CLIENT_SECRET: optionalSecretEnv,
  GOOGLE_OAUTH_REDIRECT_URI: optionalUrlEnv,
  GOOGLE_WEB_CLIENT_ID: optionalSecretEnv,
  GOOGLE_ANDROID_CLIENT_ID: optionalSecretEnv,
  SMTP_HOST: optionalSecretEnv,
  SMTP_PORT: z.coerce.number().int().positive().optional(),
  SMTP_USER: optionalSecretEnv,
  SMTP_PASS: optionalSecretEnv,
  DEFAULT_TIMEZONE: z.string().min(1).default("Asia/Calcutta"),
  DEFAULT_CURRENCY: z.string().length(3).default("INR"),
  DB_POOL_MAX: z.coerce.number().int().positive().default(10),
  RATE_LIMIT_MAX: z.coerce.number().int().positive().default(300),
  RATE_LIMIT_WINDOW_MS: z.coerce.number().int().positive().default(60000)
});

const parsed = envSchema.safeParse(process.env);

if (!parsed.success) {
  throw new Error(`Invalid backend environment: ${parsed.error.message}`);
}

const rawEnv = parsed.data;
const resolvedAccessTokenTtlSeconds = rawEnv.ACCESS_TOKEN_TTL_SECONDS ?? rawEnv.AUTH_ACCESS_TOKEN_TTL_MINUTES * 60;
const resolvedEmailFrom = rawEnv.EMAIL_FROM ?? rawEnv.AUTH_EMAIL_FROM;
const resolvedGoogleWebClientId = rawEnv.GOOGLE_WEB_CLIENT_ID ?? rawEnv.GOOGLE_OAUTH_CLIENT_ID;
const resolvedGoogleAndroidClientId = rawEnv.GOOGLE_ANDROID_CLIENT_ID ?? rawEnv.GOOGLE_OAUTH_CLIENT_ID;
const resolvedPasswordPepper = rawEnv.PASSWORD_PEPPER ?? rawEnv.AUTH_TOKEN_HASH_PEPPER;
const resolvedOtpPepper = rawEnv.OTP_PEPPER ?? rawEnv.AUTH_TOKEN_HASH_PEPPER;
const resolvedRefreshTokenPepper = rawEnv.REFRESH_TOKEN_PEPPER ?? rawEnv.AUTH_TOKEN_HASH_PEPPER;

export const env = {
  ...rawEnv,
  ACCESS_TOKEN_TTL_SECONDS: resolvedAccessTokenTtlSeconds,
  EMAIL_FROM: resolvedEmailFrom,
  GOOGLE_WEB_CLIENT_ID: resolvedGoogleWebClientId,
  GOOGLE_ANDROID_CLIENT_ID: resolvedGoogleAndroidClientId,
  PASSWORD_PEPPER: resolvedPasswordPepper,
  OTP_PEPPER: resolvedOtpPepper,
  REFRESH_TOKEN_PEPPER: resolvedRefreshTokenPepper
};
export const authAllowedMobileRedirectUris = env.AUTH_ALLOWED_MOBILE_REDIRECT_URIS
  .split(",")
  .map((value) => value.trim())
  .filter((value) => value.length > 0);

if (env.NODE_ENV === "production" && env.CORS_ORIGIN.trim() === "*") {
  throw new Error("Invalid backend environment: CORS_ORIGIN cannot be '*' in production.");
}

if (env.NODE_ENV === "production") {
  const missingSecrets = [
    rawEnv.PASSWORD_PEPPER ? null : "PASSWORD_PEPPER",
    rawEnv.OTP_PEPPER ? null : "OTP_PEPPER",
    rawEnv.REFRESH_TOKEN_PEPPER ? null : "REFRESH_TOKEN_PEPPER"
  ].filter((value): value is string => value != null);

  if (missingSecrets.length > 0) {
    throw new Error(`Invalid backend environment: missing production auth secrets: ${missingSecrets.join(", ")}.`);
  }
}

if (authAllowedMobileRedirectUris.length === 0) {
  throw new Error("Invalid backend environment: AUTH_ALLOWED_MOBILE_REDIRECT_URIS must contain at least one URI.");
}

for (const redirectUri of authAllowedMobileRedirectUris) {
  const parsedUri = new URL(redirectUri);

  if (parsedUri.protocol !== "aeon:" || parsedUri.hostname !== "auth" || parsedUri.pathname !== "/callback") {
    throw new Error(
      "Invalid backend environment: AUTH_ALLOWED_MOBILE_REDIRECT_URIS must only contain aeon://auth/callback style URIs."
    );
  }
}

if (
  env.NODE_ENV === "production" &&
  env.GOOGLE_OAUTH_REDIRECT_URI != null &&
  !env.GOOGLE_OAUTH_REDIRECT_URI.startsWith("https://")
) {
  throw new Error("Invalid backend environment: GOOGLE_OAUTH_REDIRECT_URI must use HTTPS in production.");
}

const googleOAuthConfigured = Boolean(
  env.GOOGLE_OAUTH_CLIENT_ID &&
    env.GOOGLE_OAUTH_CLIENT_SECRET &&
    env.GOOGLE_OAUTH_REDIRECT_URI
);
const googleOAuthPartiallyConfigured = Boolean(
  env.GOOGLE_OAUTH_CLIENT_ID ||
    env.GOOGLE_OAUTH_CLIENT_SECRET ||
    env.GOOGLE_OAUTH_REDIRECT_URI
) && !googleOAuthConfigured;

if (googleOAuthPartiallyConfigured) {
  throw new Error(
    "Invalid backend environment: Google OAuth requires GOOGLE_OAUTH_CLIENT_ID, GOOGLE_OAUTH_CLIENT_SECRET, and GOOGLE_OAUTH_REDIRECT_URI together."
  );
}

if (env.NODE_ENV === "production" && !googleOAuthConfigured) {
  throw new Error(
    "Invalid backend environment: Google OAuth must be configured in production."
  );
}

export type AppEnv = typeof env;
