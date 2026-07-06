import { z } from "zod";
const emailSchema = z
    .string()
    .trim()
    .email("Enter a valid email address.")
    .transform((value) => value.toLowerCase());
const otpCodeSchema = z
    .string()
    .trim()
    .regex(/^\d{6,8}$/, "Enter the verification code.");
const passwordSchema = z
    .string()
    .min(12, "Password must be at least 12 characters.")
    .max(128, "Password must be 128 characters or fewer.")
    .refine((value) => !COMMON_PASSWORDS.has(value.toLowerCase()), "Choose a less common password.");
const otpPurposeSchema = z.enum([
    "signup",
    "login",
    "verify_email",
    "reset_password",
    "change_email",
    "reauth"
]);
const reauthPurposeSchema = z.enum([
    "change_password",
    "change_email",
    "delete_account",
    "logout_all_devices",
    "export_personal_data",
    "revoke_all_sessions",
    "add_login_method",
    "remove_login_method",
    "link_google",
    "unlink_google"
]);
export const authOtpRequestSchema = z.object({
    email: emailSchema,
    purpose: otpPurposeSchema.optional().default("signup")
});
export const authOtpVerifySchema = z.object({
    email: emailSchema,
    code: otpCodeSchema,
    purpose: otpPurposeSchema.optional().default("signup")
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
export const authPasswordSignupSchema = z.object({
    email: emailSchema,
    displayName: z
        .string()
        .trim()
        .max(80, "Name must be 80 characters or fewer.")
        .optional()
        .transform((value) => value?.trim() || undefined),
    password: passwordSchema
});
export const authPasswordForgotStartSchema = z.object({
    email: emailSchema
});
export const authPasswordForgotVerifySchema = z.object({
    email: emailSchema,
    code: otpCodeSchema
});
export const authPasswordResetSchema = z.object({
    resetToken: z.string().min(1, "Reset token is required."),
    password: passwordSchema
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
        }
        catch {
            return false;
        }
    }, "Unsupported mobile redirect URI.")
});
export const authGoogleExchangeSchema = z.object({
    exchangeCode: z.string().min(1, "Exchange code is required.")
});
export const authGoogleIdTokenSchema = z.object({
    idToken: z.string().min(20, "Google ID token is required.")
});
export const authSessionParamsSchema = z.object({
    id: z.string().min(1, "Session id is required.")
});
export const authReauthStartSchema = z.object({
    purpose: reauthPurposeSchema
});
export const authReauthVerifySchema = z.object({
    purpose: reauthPurposeSchema,
    method: z.enum(["password", "otp", "google"]),
    password: z.string().optional(),
    code: otpCodeSchema.optional(),
    idToken: z.string().optional()
});
const COMMON_PASSWORDS = new Set([
    "password",
    "password123",
    "password1234",
    "123456789012",
    "qwerty123456",
    "admin123456",
    "letmein123456",
    "aeonpassword"
]);
