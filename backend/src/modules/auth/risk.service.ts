export type RiskDecision = "allow" | "require_reauth" | "require_otp" | "block_temporarily";

export type RiskInput = {
  failedAttempts?: number | null | undefined;
  otpRequestsInWindow?: number | undefined;
  isNewDevice?: boolean | undefined;
  isNewIpHash?: boolean | undefined;
  isUnusualUserAgent?: boolean | undefined;
  refreshTokenReuse?: boolean | undefined;
  daysSinceLastLogin?: number | undefined;
  isAccountLinking?: boolean | undefined;
  isPasswordReset?: boolean | undefined;
};

export function evaluateAuthRisk(input: RiskInput): { score: number; decision: RiskDecision; reasons: string[] } {
  const reasons: string[] = [];
  let score = 0;

  if ((input.failedAttempts ?? 0) >= 5) {
    score += 60;
    reasons.push("failed_attempts");
  }

  if ((input.otpRequestsInWindow ?? 0) >= 5) {
    score += 35;
    reasons.push("otp_velocity");
  }

  if (input.refreshTokenReuse) {
    score += 100;
    reasons.push("refresh_token_reuse");
  }

  if (input.isAccountLinking) {
    score += 30;
    reasons.push("account_linking");
  }

  if (input.isPasswordReset) {
    score += 25;
    reasons.push("password_reset");
  }

  if (input.isNewDevice) {
    score += 20;
    reasons.push("new_device");
  }

  if (input.isNewIpHash) {
    score += 15;
    reasons.push("new_ip_hash");
  }

  if (input.isUnusualUserAgent) {
    score += 15;
    reasons.push("unusual_user_agent");
  }

  if ((input.daysSinceLastLogin ?? 0) >= 60) {
    score += 15;
    reasons.push("long_inactivity");
  }

  const decision: RiskDecision = score >= 90
    ? "block_temporarily"
    : score >= 55
      ? "require_reauth"
      : score >= 30
        ? "require_otp"
        : "allow";

  return { score, decision, reasons };
}
