import { createHmac, randomBytes, timingSafeEqual } from "node:crypto";
export function createOpaqueToken(bytes = 48) {
    return randomBytes(bytes).toString("base64url");
}
export function hmacSha256(value, pepper) {
    return createHmac("sha256", pepper)
        .update(value)
        .digest("hex");
}
export function hashWithContext(value, pepper, context) {
    const normalized = value?.trim();
    if (!normalized) {
        return undefined;
    }
    return hmacSha256(`${context}:${normalized}`, pepper);
}
export function safeEqual(left, right) {
    const leftBuffer = Buffer.from(left);
    const rightBuffer = Buffer.from(right);
    if (leftBuffer.length !== rightBuffer.length) {
        return false;
    }
    return timingSafeEqual(leftBuffer, rightBuffer);
}
