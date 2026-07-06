import { env } from "../../config/env.js";
import { parseWithSchema } from "../../lib/validation.js";
import { hashWithContext } from "../../security/crypto.js";
import { normalAuthRateLimit, strictAuthRateLimit, tokenAuthRateLimit } from "../../security/rate-limit.js";
import { authGoogleExchangeSchema, authGoogleIdTokenSchema, authGoogleStartQuerySchema, authOtpRequestSchema, authOtpVerifySchema, authPasswordForgotStartSchema, authPasswordForgotVerifySchema, authPasswordResetSchema, authPasswordSignInSchema, authPasswordSignupSchema, authReauthStartSchema, authReauthVerifySchema, authRefreshSchema, authSessionParamsSchema, authSignOutSchema, authSignupCompleteSchema } from "./schemas.js";
import { buildGoogleStartUrl, completeGoogleCallback, completeSignup, exchangeGoogleCodeForSession, getAuthProfile, getAuthProviderStatus, listUserSessions, refreshAuthSession, requestSignupOtp, resetPassword, revokeUserSession, signInWithGoogleIdToken, signInWithPassword, signOutAllSessions, signOutFromSession, signUpWithPassword, startOtp, startPasswordReset, startReauth, verifyOtp, verifyPasswordResetOtp, verifyReauth, verifySignupOtp } from "./service.js";
export async function registerAuthRoutes(app) {
    app.get("/providers", async () => {
        return getAuthProviderStatus();
    });
    app.post("/google", { config: normalAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authGoogleIdTokenSchema, request.body, "Invalid Google sign-in payload.");
        return signInWithGoogleIdToken(app.db, body.idToken, resolveContext(request));
    });
    app.get("/google/start", { config: normalAuthRateLimit }, async (request) => {
        const query = parseWithSchema(authGoogleStartQuerySchema, request.query, "Invalid Google sign-in payload.");
        return buildGoogleStartUrl(app.db, query.mobileRedirectUri);
    });
    app.get("/google/callback", async (request, reply) => {
        const query = request.query;
        if (query.error) {
            return reply.redirect(`aeon://auth/callback?error=${encodeURIComponent(query.error)}`);
        }
        if (!query.state || !query.code) {
            return reply.redirect("aeon://auth/callback?error=invalid_google_callback");
        }
        const result = await completeGoogleCallback(app.db, {
            state: query.state,
            code: query.code
        });
        return reply.redirect(result.redirectUrl);
    });
    app.post("/google/exchange", { config: tokenAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authGoogleExchangeSchema, request.body, "Invalid Google sign-in payload.");
        return exchangeGoogleCodeForSession(app.db, body.exchangeCode, resolveContext(request));
    });
    app.post("/otp/start", { config: strictAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authOtpRequestSchema, request.body, "Invalid OTP payload.");
        return startOtp(app.db, body, resolveContext(request));
    });
    app.post("/otp/verify", { config: strictAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authOtpVerifySchema, request.body, "Invalid verification payload.");
        return verifyOtp(app.db, body, resolveContext(request));
    });
    app.post("/signup/request-otp", { config: strictAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authOtpRequestSchema, request.body, "Invalid email payload.");
        return requestSignupOtp(app.db, body.email, resolveContext(request));
    });
    app.post("/signup/verify-otp", { config: strictAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authOtpVerifySchema, request.body, "Invalid verification payload.");
        return verifySignupOtp(app.db, body.email, body.code, resolveContext(request));
    });
    app.post("/signup/complete", { config: normalAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authSignupCompleteSchema, request.body, "Invalid signup payload.");
        return completeSignup(app.db, body, resolveContext(request));
    });
    app.post("/password/signup", { config: strictAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authPasswordSignupSchema, request.body, "Invalid signup payload.");
        return signUpWithPassword(app.db, body, resolveContext(request));
    });
    app.post("/password/login", { config: strictAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authPasswordSignInSchema, request.body, "Invalid sign-in payload.");
        return signInWithPassword(app.db, body.email, body.password, resolveContext(request));
    });
    app.post("/signin/password", { config: strictAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authPasswordSignInSchema, request.body, "Invalid sign-in payload.");
        return signInWithPassword(app.db, body.email, body.password, resolveContext(request));
    });
    app.post("/password/forgot/start", { config: strictAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authPasswordForgotStartSchema, request.body, "Invalid password reset payload.");
        return startPasswordReset(app.db, body.email, resolveContext(request));
    });
    app.post("/password/forgot/verify", { config: strictAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authPasswordForgotVerifySchema, request.body, "Invalid password reset payload.");
        return verifyPasswordResetOtp(app.db, body.email, body.code, resolveContext(request));
    });
    app.post("/password/reset", { config: strictAuthRateLimit }, async (request, reply) => {
        const body = parseWithSchema(authPasswordResetSchema, request.body, "Invalid password reset payload.");
        await resetPassword(app.db, body.resetToken, body.password, resolveContext(request));
        return reply.status(204).send();
    });
    app.post("/token/refresh", { config: tokenAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authRefreshSchema, request.body, "Invalid session refresh payload.");
        return refreshAuthSession(app.db, body.refreshToken, resolveContext(request));
    });
    app.post("/session/refresh", { config: tokenAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authRefreshSchema, request.body, "Invalid session refresh payload.");
        return refreshAuthSession(app.db, body.refreshToken, resolveContext(request));
    });
    app.post("/logout", async (request, reply) => {
        const body = parseWithSchema(authSignOutSchema, request.body, "Invalid sign-out payload.");
        await signOutFromSession(app.db, body.refreshToken, resolveContext(request));
        return reply.status(204).send();
    });
    app.post("/signout", async (request, reply) => {
        const body = parseWithSchema(authSignOutSchema, request.body, "Invalid sign-out payload.");
        await signOutFromSession(app.db, body.refreshToken, resolveContext(request));
        return reply.status(204).send();
    });
    app.post("/logout-all", { preHandler: app.authenticate }, async (request, reply) => {
        await signOutAllSessions(app.db, request.authUser.userId, resolveContext(request));
        return reply.status(204).send();
    });
    app.post("/reauth/start", { preHandler: app.authenticate, config: normalAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authReauthStartSchema, request.body, "Invalid re-auth payload.");
        return startReauth(app.db, request.authUser.userId, body.purpose, resolveContext(request));
    });
    app.post("/reauth/verify", { preHandler: app.authenticate, config: normalAuthRateLimit }, async (request) => {
        const body = parseWithSchema(authReauthVerifySchema, request.body, "Invalid re-auth payload.");
        return verifyReauth(app.db, request.authUser.userId, body, resolveContext(request));
    });
    app.get("/me", { preHandler: app.authenticate }, async (request) => {
        return {
            user: await getAuthProfile(app.db, request.authUser.userId),
            providers: getAuthProviderStatus()
        };
    });
    app.get("/sessions", { preHandler: app.authenticate }, async (request) => {
        return listUserSessions(app.db, request.authUser.userId);
    });
    app.delete("/sessions/:id", { preHandler: app.authenticate }, async (request, reply) => {
        const params = parseWithSchema(authSessionParamsSchema, request.params, "Invalid session payload.");
        await revokeUserSession(app.db, request.authUser.userId, params.id, resolveContext(request));
        return reply.status(204).send();
    });
}
function resolveContext(request) {
    const userAgent = typeof request.headers["user-agent"] === "string"
        ? request.headers["user-agent"]
        : undefined;
    const deviceId = typeof request.headers["x-aeon-device-id"] === "string"
        ? request.headers["x-aeon-device-id"]
        : undefined;
    const deviceName = typeof request.headers["x-aeon-device-name"] === "string"
        ? request.headers["x-aeon-device-name"]
        : undefined;
    return {
        userAgent,
        deviceId,
        deviceName,
        ipHash: hashWithContext(request.ip, env.AUTH_TOKEN_HASH_PEPPER, "ip"),
        userAgentHash: hashWithContext(userAgent, env.AUTH_TOKEN_HASH_PEPPER, "user_agent")
    };
}
