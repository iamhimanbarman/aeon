import { describe, expect, it } from "vitest";

process.env.DATABASE_URL ??= "https://db.example.com";
process.env.AUTH_JWT_SECRET ??= "abcdefghijklmnopqrstuvwxyz123456";
process.env.AUTH_TOKEN_HASH_PEPPER ??= "0123456789abcdef0123456789abcdef";
process.env.AUTH_EMAIL_FROM ??= "Aeon <auth@example.com>";
process.env.RESEND_API_KEY ??= "re_test";

const tokens = await import("../src/modules/auth/tokens.js");

describe("auth token helpers", () => {

  it("signs and verifies first-party access tokens", async () => {
    const result = await tokens.signAccessToken({
      sub: "10c9d1f6-0e03-4f79-b5d4-36816a4f25d8",
      email: "user@example.com",
      name: "Aeon User",
      provider: "password"
    });

    expect(result.expiresInSeconds).toBeGreaterThan(0);

    const payload = await tokens.verifyFirstPartyAccessToken(result.token);

    expect(payload.sub).toBe("10c9d1f6-0e03-4f79-b5d4-36816a4f25d8");
    expect(payload.email).toBe("user@example.com");
    expect(payload.type).toBe("access");
  });

  it("creates deterministic opaque token hashes", () => {
    const left = tokens.hashOpaqueToken("refresh-token");
    const right = tokens.hashOpaqueToken("refresh-token");
    const different = tokens.hashOpaqueToken("other-token");

    expect(left).toBe(right);
    expect(left).not.toBe(different);
  });
});
