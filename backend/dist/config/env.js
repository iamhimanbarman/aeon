import "dotenv/config";
import { z } from "zod";
const envSchema = z.object({
    NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
    HOST: z.string().min(1).default("0.0.0.0"),
    PORT: z.coerce.number().int().positive().default(10000),
    LOG_LEVEL: z
        .enum(["fatal", "error", "warn", "info", "debug", "trace", "silent"])
        .default("info"),
    CORS_ORIGIN: z.string().min(1).default("*"),
    DATABASE_URL: z.string().url(),
    SUPABASE_URL: z.string().url().optional(),
    SUPABASE_JWT_AUDIENCE: z.string().min(1).default("authenticated"),
    AUTH_JWT_SECRET: z.string().min(32),
    AUTH_JWT_ISSUER: z.string().min(1).default("aeon-backend"),
    AUTH_JWT_AUDIENCE: z.string().min(1).default("authenticated"),
    AUTH_ACCESS_TOKEN_TTL_MINUTES: z.coerce.number().int().positive().default(15),
    AUTH_REFRESH_TOKEN_TTL_DAYS: z.coerce.number().int().positive().default(30),
    AUTH_SIGNUP_PROOF_TTL_MINUTES: z.coerce.number().int().positive().default(15),
    AUTH_EMAIL_OTP_TTL_MINUTES: z.coerce.number().int().positive().default(10),
    AUTH_EMAIL_OTP_RESEND_SECONDS: z.coerce.number().int().positive().default(60),
    AUTH_EMAIL_OTP_MAX_ATTEMPTS: z.coerce.number().int().positive().default(5),
    AUTH_TOKEN_HASH_PEPPER: z.string().min(16),
    AUTH_EMAIL_FROM: z.string().min(1),
    RESEND_API_KEY: z.string().min(1),
    GOOGLE_OAUTH_CLIENT_ID: z.string().min(1).optional(),
    GOOGLE_OAUTH_CLIENT_SECRET: z.string().min(1).optional(),
    GOOGLE_OAUTH_REDIRECT_URI: z.string().url().optional(),
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
export const env = parsed.data;
