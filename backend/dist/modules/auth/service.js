import { createHash, randomBytes, randomInt, scrypt as scryptCallback, timingSafeEqual } from "node:crypto";
import { authAllowedMobileRedirectUris, env } from "../../config/env.js";
import { badRequest, conflict, unauthorized } from "../../lib/errors.js";
import { createAuthSession, createAuthUser, createEmailChallenge, createOAuthExchangeCode, createOAuthState, findAuthUserByEmail, findAuthUserById, findLatestActiveEmailChallenge, findOAuthExchangeCode, findOAuthState, findSessionByRefreshHash, consumeOAuthExchangeCode, consumeOAuthState, incrementEmailChallengeAttempts, markEmailChallengeConsumed, revokeActiveChallengesForEmail, revokeAuthSession, rotateAuthSession, touchAuthUser, updateAuthUser } from "./repository.js";
import { sendSignupOtpEmail } from "./email.js";
import { createRefreshToken, hashOpaqueToken, signAccessToken, signSignupToken, verifySignupToken } from "./tokens.js";
export async function requestSignupOtp(db, email) {
    const existingChallenge = await findLatestActiveEmailChallenge(db, email, "signup");
    const now = Date.now();
    if (existingChallenge != null) {
        const resendAvailableAt = new Date(existingChallenge.resend_available_at).getTime();
        if (resendAvailableAt > now) {
            return {
                resendAfterSeconds: Math.max(1, Math.ceil((resendAvailableAt - now) / 1000)),
                expiresInSeconds: Math.max(1, Math.ceil((new Date(existingChallenge.expires_at).getTime() - now) / 1000))
            };
        }
    }
    const code = randomInt(100000, 1000000).toString();
    const codeSalt = randomBytes(16).toString("hex");
    const codeHash = hashChallengeCode(codeSalt, code);
    const expiresAt = new Date(now + env.AUTH_EMAIL_OTP_TTL_MINUTES * 60_000).toISOString();
    const resendAvailableAt = new Date(now + env.AUTH_EMAIL_OTP_RESEND_SECONDS * 1000).toISOString();
    await revokeActiveChallengesForEmail(db, email, "signup");
    await createEmailChallenge(db, {
        emailNormalized: email,
        purpose: "signup",
        codeHash,
        codeSalt,
        expiresAt,
        resendAvailableAt,
        maxAttempts: env.AUTH_EMAIL_OTP_MAX_ATTEMPTS
    });
    await sendSignupOtpEmail({
        email,
        code,
        expiresInMinutes: env.AUTH_EMAIL_OTP_TTL_MINUTES
    });
    return {
        resendAfterSeconds: env.AUTH_EMAIL_OTP_RESEND_SECONDS,
        expiresInSeconds: env.AUTH_EMAIL_OTP_TTL_MINUTES * 60
    };
}
export async function verifySignupOtp(db, email, code) {
    const challenge = await findLatestActiveEmailChallenge(db, email, "signup");
    if (challenge == null) {
        throw badRequest("The verification code is invalid or expired.");
    }
    const now = Date.now();
    if (challenge.consumed_at != null || new Date(challenge.expires_at).getTime() <= now) {
        throw badRequest("The verification code is invalid or expired.");
    }
    if (challenge.invalid_attempt_count >= challenge.max_attempts) {
        throw badRequest("Too many incorrect codes. Request a new code.");
    }
    if (!constantTimeMatch(challenge.code_hash, hashChallengeCode(challenge.code_salt, code))) {
        await incrementEmailChallengeAttempts(db, challenge.id);
        throw badRequest("The verification code is invalid or expired.");
    }
    await markEmailChallengeConsumed(db, challenge.id);
    const user = await findAuthUserByEmail(db, email);
    if (user?.password_hash) {
        return {
            nextStep: "sign_in"
        };
    }
    return {
        nextStep: "set_password",
        signupToken: await signSignupToken({ email })
    };
}
export async function completeSignup(db, input, context) {
    const signupClaims = await verifySignupToken(input.signupToken);
    const email = signupClaims.email.toLowerCase();
    const now = new Date().toISOString();
    const passwordHash = await hashPassword(input.password);
    const existingUser = await findAuthUserByEmail(db, email);
    if (existingUser?.password_hash) {
        throw conflict("This email already has an Aeon account. Sign in instead.");
    }
    const user = existingUser == null
        ? await createAuthUser(db, {
            email,
            displayName: input.displayName,
            passwordHash,
            authProvider: "password",
            emailVerifiedAt: now,
            timezone: env.DEFAULT_TIMEZONE,
            defaultCurrency: env.DEFAULT_CURRENCY
        })
        : await updateAuthUser(db, existingUser.id, {
            displayName: input.displayName,
            passwordHash,
            authProvider: "password",
            emailVerifiedAt: now
        });
    return issueSession(db, {
        id: String(user.id),
        email,
        displayName: typeof user.displayName === "string" ? user.displayName : undefined,
        provider: String(user.authProvider ?? "password"),
        emailVerifiedAt: typeof user.emailVerifiedAt === "string" ? user.emailVerifiedAt : now
    }, context);
}
export async function signInWithPassword(db, email, password, context) {
    const user = await findAuthUserByEmail(db, email);
    if (user == null ||
        user.password_hash == null ||
        !user.is_active ||
        !(await verifyPassword(password, user.password_hash))) {
        throw unauthorized("Invalid email or password.");
    }
    return issueSession(db, {
        id: user.id,
        email: user.email_normalized ?? email,
        displayName: user.display_name ?? undefined,
        provider: user.auth_provider,
        emailVerifiedAt: user.email_verified_at ?? undefined
    }, context);
}
export async function refreshAuthSession(db, refreshToken, context) {
    const sessionHash = hashOpaqueToken(refreshToken);
    const session = await findSessionByRefreshHash(db, sessionHash);
    if (session == null || session.revoked_at != null || new Date(session.expires_at).getTime() <= Date.now()) {
        throw unauthorized("The session has expired. Sign in again.");
    }
    const user = await findAuthUserById(db, session.user_id);
    if (user == null || !user.is_active) {
        throw unauthorized("The session has expired. Sign in again.");
    }
    const nextRefreshToken = createRefreshToken();
    const nextRefreshHash = hashOpaqueToken(nextRefreshToken);
    const nextRefreshExpiresAt = new Date(Date.now() + env.AUTH_REFRESH_TOKEN_TTL_DAYS * 24 * 60 * 60 * 1000).toISOString();
    await rotateAuthSession(db, session.id, nextRefreshHash, nextRefreshExpiresAt, context.userAgent, context.ipAddress);
    await touchAuthUser(db, user.id);
    const accessToken = await signAccessToken({
        sub: user.id,
        email: user.email_normalized ?? "",
        name: user.display_name ?? undefined,
        provider: user.auth_provider
    });
    return {
        accessToken: accessToken.token,
        refreshToken: nextRefreshToken,
        expiresInSeconds: accessToken.expiresInSeconds,
        user: {
            id: user.id,
            email: user.email_normalized ?? "",
            displayName: user.display_name ?? undefined,
            provider: user.auth_provider,
            emailVerifiedAt: user.email_verified_at ?? undefined
        }
    };
}
export async function signOutFromSession(db, refreshToken) {
    await revokeAuthSession(db, hashOpaqueToken(refreshToken));
}
export async function getAuthProfile(db, userId) {
    const user = await findAuthUserById(db, userId);
    if (user == null || !user.is_active || user.email_normalized == null) {
        throw unauthorized("Authentication is required.");
    }
    return {
        id: user.id,
        email: user.email_normalized,
        displayName: user.display_name ?? undefined,
        provider: user.auth_provider,
        emailVerifiedAt: user.email_verified_at ?? undefined
    };
}
export function getAuthProviderStatus() {
    return {
        gmail: {
            enabled: Boolean(env.GOOGLE_OAUTH_CLIENT_ID &&
                env.GOOGLE_OAUTH_CLIENT_SECRET &&
                env.GOOGLE_OAUTH_REDIRECT_URI)
        }
    };
}
export async function buildGoogleStartUrl(db, mobileRedirectUri) {
    if (!getAuthProviderStatus().gmail.enabled) {
        throw badRequest("Google sign-in is unavailable right now.");
    }
    assertAllowedMobileRedirectUri(mobileRedirectUri);
    const state = randomBytes(32).toString("base64url");
    const stateHash = hashOpaqueToken(state);
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString();
    await createOAuthState(db, {
        stateHash,
        provider: "google",
        mobileRedirectUri,
        expiresAt
    });
    const params = new URLSearchParams({
        client_id: env.GOOGLE_OAUTH_CLIENT_ID,
        redirect_uri: env.GOOGLE_OAUTH_REDIRECT_URI,
        response_type: "code",
        scope: "openid email profile",
        prompt: "select_account",
        access_type: "offline",
        state
    });
    return {
        url: `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`
    };
}
export async function completeGoogleCallback(db, input) {
    if (!getAuthProviderStatus().gmail.enabled) {
        throw badRequest("Google sign-in is unavailable right now.");
    }
    const state = await findOAuthState(db, hashOpaqueToken(input.state));
    if (state == null || state.consumed_at != null || new Date(state.expires_at).getTime() <= Date.now()) {
        throw badRequest("The Google sign-in session is invalid or expired.");
    }
    await consumeOAuthState(db, state.id);
    const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: new URLSearchParams({
            code: input.code,
            client_id: env.GOOGLE_OAUTH_CLIENT_ID,
            client_secret: env.GOOGLE_OAUTH_CLIENT_SECRET,
            redirect_uri: env.GOOGLE_OAUTH_REDIRECT_URI,
            grant_type: "authorization_code"
        })
    });
    if (!tokenResponse.ok) {
        return {
            redirectUrl: buildCallbackErrorUrl(state.mobile_redirect_uri, "google_token_exchange_failed")
        };
    }
    const tokenJson = await tokenResponse.json();
    if (!tokenJson.access_token) {
        return {
            redirectUrl: buildCallbackErrorUrl(state.mobile_redirect_uri, "google_token_exchange_failed")
        };
    }
    const profileResponse = await fetch("https://openidconnect.googleapis.com/v1/userinfo", {
        headers: {
            Authorization: `Bearer ${tokenJson.access_token}`
        }
    });
    if (!profileResponse.ok) {
        return {
            redirectUrl: buildCallbackErrorUrl(state.mobile_redirect_uri, "google_profile_failed")
        };
    }
    const profile = await profileResponse.json();
    if (!profile.email || !profile.email_verified) {
        return {
            redirectUrl: buildCallbackErrorUrl(state.mobile_redirect_uri, "google_email_not_verified")
        };
    }
    const email = profile.email.toLowerCase();
    const user = await findAuthUserByEmail(db, email);
    const ensuredUser = user == null
        ? await createAuthUser(db, {
            email,
            displayName: profile.name,
            authProvider: "google",
            emailVerifiedAt: new Date().toISOString(),
            timezone: env.DEFAULT_TIMEZONE,
            defaultCurrency: env.DEFAULT_CURRENCY
        })
        : await updateAuthUser(db, user.id, {
            displayName: profile.name,
            authProvider: "google",
            emailVerifiedAt: new Date().toISOString()
        });
    const exchangeCode = randomBytes(32).toString("base64url");
    const exchangeHash = hashOpaqueToken(exchangeCode);
    await createOAuthExchangeCode(db, {
        codeHash: exchangeHash,
        provider: "google",
        userId: String(ensuredUser.id),
        expiresAt: new Date(Date.now() + 5 * 60 * 1000).toISOString()
    });
    return {
        redirectUrl: `${state.mobile_redirect_uri}?exchange_code=${encodeURIComponent(exchangeCode)}`
    };
}
export async function exchangeGoogleCodeForSession(db, exchangeCode, context) {
    const exchange = await findOAuthExchangeCode(db, hashOpaqueToken(exchangeCode));
    if (exchange == null || exchange.consumed_at != null || new Date(exchange.expires_at).getTime() <= Date.now()) {
        throw unauthorized("The Google sign-in session is invalid or expired.");
    }
    await consumeOAuthExchangeCode(db, exchange.id);
    const user = await findAuthUserById(db, exchange.user_id);
    if (user == null || !user.is_active || user.email_normalized == null) {
        throw unauthorized("The Google sign-in session is invalid or expired.");
    }
    return issueSession(db, {
        id: user.id,
        email: user.email_normalized,
        displayName: user.display_name ?? undefined,
        provider: user.auth_provider,
        emailVerifiedAt: user.email_verified_at ?? undefined
    }, context);
}
async function issueSession(db, user, context) {
    const refreshToken = createRefreshToken();
    const refreshTokenHash = hashOpaqueToken(refreshToken);
    const refreshExpiresAt = new Date(Date.now() + env.AUTH_REFRESH_TOKEN_TTL_DAYS * 24 * 60 * 60 * 1000).toISOString();
    await createAuthSession(db, {
        userId: user.id,
        refreshTokenHash,
        expiresAt: refreshExpiresAt,
        userAgent: context.userAgent,
        ipAddress: context.ipAddress
    });
    await touchAuthUser(db, user.id);
    const accessToken = await signAccessToken({
        sub: user.id,
        email: user.email,
        name: user.displayName,
        provider: user.provider
    });
    return {
        accessToken: accessToken.token,
        refreshToken,
        expiresInSeconds: accessToken.expiresInSeconds,
        user
    };
}
async function hashPassword(password) {
    const salt = randomBytes(16);
    const derivedKey = await deriveScryptKey(password, salt, 64, 16_384, 8, 1);
    return [
        "scrypt",
        "16384",
        "8",
        "1",
        salt.toString("base64url"),
        derivedKey.toString("base64url")
    ].join("$");
}
async function verifyPassword(password, encoded) {
    const [algorithm, nValue, rValue, pValue, salt, hash] = encoded.split("$");
    if (algorithm !== "scrypt" || !nValue || !rValue || !pValue || !salt || !hash) {
        return false;
    }
    const derivedKey = await deriveScryptKey(password, Buffer.from(salt, "base64url"), Buffer.from(hash, "base64url").byteLength, Number(nValue), Number(rValue), Number(pValue));
    return constantTimeMatch(hash, derivedKey.toString("base64url"));
}
function hashChallengeCode(salt, code) {
    return createHash("sha256")
        .update(`${env.AUTH_TOKEN_HASH_PEPPER}:${salt}:${code}`)
        .digest("hex");
}
function constantTimeMatch(left, right) {
    const leftBuffer = Buffer.from(left);
    const rightBuffer = Buffer.from(right);
    if (leftBuffer.length !== rightBuffer.length) {
        return false;
    }
    return timingSafeEqual(leftBuffer, rightBuffer);
}
function buildCallbackErrorUrl(mobileRedirectUri, errorCode) {
    return `${mobileRedirectUri}?error=${encodeURIComponent(errorCode)}`;
}
function assertAllowedMobileRedirectUri(mobileRedirectUri) {
    if (!authAllowedMobileRedirectUris.includes(mobileRedirectUri)) {
        throw badRequest("Unsupported mobile redirect URI.");
    }
}
async function deriveScryptKey(password, salt, keyLength, n, r, p) {
    return new Promise((resolve, reject) => {
        scryptCallback(password, salt, keyLength, {
            N: n,
            r,
            p
        }, (error, derivedKey) => {
            if (error != null) {
                reject(error);
                return;
            }
            resolve(derivedKey);
        });
    });
}
