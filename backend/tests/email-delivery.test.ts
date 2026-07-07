import { describe, expect, it } from "vitest";
import { AppError } from "../src/lib/errors.js";
import { computeFinanceEmailRetryDelayMs } from "../src/email/outbox.service.js";
import { isRetryableEmailDeliveryError } from "../src/email/email.service.js";

describe("email delivery retry policy", () => {
  it("retries transient provider failures", () => {
    const error = new AppError(
      502,
      "email_delivery_failed",
      "temporary failure",
      {
        provider: "resend",
        status: 503,
        retryable: true,
        message: "Service unavailable"
      }
    );

    expect(isRetryableEmailDeliveryError(error)).toBe(true);
  });

  it("does not retry permanent provider failures", () => {
    const error = new AppError(
      502,
      "email_delivery_failed",
      "invalid recipient",
      {
        provider: "resend",
        status: 422,
        retryable: false,
        message: "Invalid email"
      }
    );

    expect(isRetryableEmailDeliveryError(error)).toBe(false);
  });

  it("uses increasing finance outbox backoff windows", () => {
    expect(computeFinanceEmailRetryDelayMs(1)).toBe(45_000);
    expect(computeFinanceEmailRetryDelayMs(2)).toBeGreaterThan(computeFinanceEmailRetryDelayMs(1));
    expect(computeFinanceEmailRetryDelayMs(6)).toBeGreaterThan(computeFinanceEmailRetryDelayMs(3));
  });
});
