import { describe, expect, it } from "vitest";
import { AppError } from "../src/lib/errors.js";
import {
  computeFinanceEmailRetryDelayMs,
  resolveFinanceEmailFailureDisposition,
  shouldContinueFinanceEmailDrain
} from "../src/email/outbox.service.js";
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

  it("queues only retryable finance email failures", () => {
    const transientError = new AppError(
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
    const permanentError = new AppError(
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

    expect(resolveFinanceEmailFailureDisposition(transientError)).toBe("queue");
    expect(resolveFinanceEmailFailureDisposition(permanentError)).toBe("fail");
  });

  it("continues the outbox drain when a full batch or rerun is requested", () => {
    expect(shouldContinueFinanceEmailDrain(6, 6, false)).toBe(true);
    expect(shouldContinueFinanceEmailDrain(2, 6, true)).toBe(true);
    expect(shouldContinueFinanceEmailDrain(2, 6, false)).toBe(false);
  });
});
