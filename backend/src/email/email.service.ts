import { env } from "../config/env.js";
import { AppError } from "../lib/errors.js";

type OtpEmailInput = {
  email: string;
  code: string;
  expiresInMinutes: number;
  purpose: "signup" | "login" | "verify_email" | "reset_password" | "change_email" | "reauth";
};

type CounterpartyShareEmailInput = {
  recipientEmail: string;
  recipientName: string;
  ownerName: string;
  ownerEmail?: string | undefined;
  direction: "owed_to_me" | "i_owe";
  purpose: string;
  amount: string;
  currency: string;
  occurredAt: string;
  note?: string | undefined;
};

export async function sendOtpEmail(input: OtpEmailInput): Promise<void> {
  if (env.NODE_ENV !== "production" && env.RESEND_API_KEY.startsWith("re_test")) {
    return;
  }

  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${env.RESEND_API_KEY}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      from: env.EMAIL_FROM,
      to: [input.email],
      subject: resolveSubject(input.purpose),
      html: buildOtpEmailHtml(input),
      text: buildOtpEmailText(input)
    })
  });

  if (!response.ok) {
    const providerError = await parseEmailProviderError(response);
    throw new AppError(
      502,
      "email_delivery_failed",
      "Aeon could not send the verification email right now.",
      providerError
    );
  }
}

export async function sendPasswordResetEmail(input: Omit<OtpEmailInput, "purpose">): Promise<void> {
  return sendOtpEmail({
    ...input,
    purpose: "reset_password"
  });
}

export async function sendFinanceCounterpartyEmail(
  input: CounterpartyShareEmailInput
): Promise<void> {
  if (env.NODE_ENV !== "production" && env.RESEND_API_KEY.startsWith("re_test")) {
    return;
  }

  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${env.RESEND_API_KEY}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      from: env.EMAIL_FROM,
      to: [input.recipientEmail],
      subject: `${input.ownerName} shared a money record on Aeon`,
      html: buildCounterpartyEmailHtml(input),
      text: buildCounterpartyEmailText(input)
    })
  });

  if (!response.ok) {
    const providerError = await parseEmailProviderError(response);
    throw new AppError(
      502,
      "email_delivery_failed",
      "Aeon could not send the account email right now.",
      providerError
    );
  }
}

async function parseEmailProviderError(response: Response): Promise<Record<string, unknown>> {
  const body = await response.text().catch(() => "");

  return {
    provider: "resend",
    status: response.status,
    message: parseProviderMessage(body)
  };
}

function parseProviderMessage(body: string): string {
  if (!body.trim()) {
    return "No provider error body returned.";
  }

  try {
    const parsed = JSON.parse(body) as { message?: unknown; name?: unknown };
    const message = typeof parsed.message === "string" ? parsed.message : undefined;
    const name = typeof parsed.name === "string" ? parsed.name : undefined;
    return [name, message].filter(Boolean).join(": ") || "Provider returned an error.";
  } catch {
    return body.slice(0, 500);
  }
}

function resolveSubject(purpose: OtpEmailInput["purpose"]): string {
  switch (purpose) {
    case "reset_password":
      return "Reset your Aeon password";
    case "reauth":
      return "Confirm your Aeon action";
    default:
      return "Your Aeon verification code";
  }
}

function buildOtpEmailHtml(input: OtpEmailInput): string {
  return `
    <div style="background:#050608;color:#F6F7FB;font-family:Arial,sans-serif;padding:32px;">
      <div style="max-width:520px;margin:0 auto;background:#101218;border:1px solid rgba(255,255,255,0.08);border-radius:28px;padding:32px;">
        <p style="margin:0 0 12px;color:#ABB2C0;font-size:12px;letter-spacing:0.18em;text-transform:uppercase;">Aeon private access</p>
        <h1 style="margin:0 0 12px;font-size:28px;line-height:1.2;">${resolveHeading(input.purpose)}</h1>
        <p style="margin:0 0 24px;color:#ABB2C0;font-size:15px;line-height:1.6;">
          Enter this code in Aeon to continue securely.
        </p>
        <div style="margin:0 0 24px;padding:20px;border-radius:22px;background:linear-gradient(135deg,#1D2140,#111520);border:1px solid rgba(139,108,255,0.30);text-align:center;">
          <div style="font-size:34px;letter-spacing:0.42em;font-weight:700;color:#F6F7FB;">${input.code}</div>
        </div>
        <p style="margin:0 0 10px;color:#ABB2C0;font-size:14px;">This code expires in ${input.expiresInMinutes} minutes.</p>
        <p style="margin:0;color:#747D8E;font-size:13px;line-height:1.5;">
          If you did not request this, you can ignore this email.
        </p>
      </div>
    </div>
  `.trim();
}

function buildOtpEmailText(input: OtpEmailInput): string {
  return [
    resolveHeading(input.purpose),
    "",
    `Code: ${input.code}`,
    `Expires in: ${input.expiresInMinutes} minutes`,
    "",
    "If you did not request this, you can ignore this email."
  ].join("\n");
}

function resolveHeading(purpose: OtpEmailInput["purpose"]): string {
  switch (purpose) {
    case "reset_password":
      return "Reset your password";
    case "reauth":
      return "Confirm this action";
    default:
      return "Verify your email";
  }
}

function buildCounterpartyEmailHtml(input: CounterpartyShareEmailInput): string {
  const amountLabel = formatCurrency(input.amount, input.currency);
  const directionLabel = input.direction === "owed_to_me"
    ? "Amount expected from you"
    : "Amount they owe you";
  const directionDetail = input.direction === "owed_to_me"
    ? `${input.ownerName} recorded that this amount is expected from you.`
    : `${input.ownerName} recorded that they owe you this amount.`;
  const noteBlock = input.note
    ? `
        <div style="margin-top:14px;padding:14px 16px;border-radius:18px;background:rgba(255,255,255,0.03);border:1px solid rgba(255,255,255,0.06);">
          <div style="font-size:12px;letter-spacing:0.12em;text-transform:uppercase;color:#8D95A6;margin-bottom:8px;">Note</div>
          <div style="font-size:14px;line-height:1.6;color:#E8EBF5;">${escapeHtml(input.note)}</div>
        </div>
      `.trim()
    : "";
  const ownerMeta = input.ownerEmail
    ? `${escapeHtml(input.ownerName)} • ${escapeHtml(input.ownerEmail)}`
    : escapeHtml(input.ownerName);

  return `
    <div style="background:#050608;color:#F6F7FB;font-family:Arial,sans-serif;padding:32px;">
      <div style="max-width:560px;margin:0 auto;background:#101218;border:1px solid rgba(255,255,255,0.08);border-radius:30px;padding:32px;">
        <div style="display:inline-block;padding:8px 14px;border-radius:999px;background:rgba(245,197,66,0.14);border:1px solid rgba(245,197,66,0.24);color:#F5C542;font-size:12px;letter-spacing:0.14em;text-transform:uppercase;">
          Aeon finance record
        </div>
        <h1 style="margin:18px 0 10px;font-size:28px;line-height:1.2;">Shared account summary</h1>
        <p style="margin:0 0 24px;color:#ABB2C0;font-size:15px;line-height:1.6;">
          Hi ${escapeHtml(input.recipientName)}, ${escapeHtml(directionDetail)}
        </p>
        <div style="padding:22px;border-radius:24px;background:linear-gradient(135deg,#171B2C,#12141C);border:1px solid rgba(255,255,255,0.08);">
          <div style="font-size:13px;color:#8D95A6;text-transform:uppercase;letter-spacing:0.14em;">${directionLabel}</div>
          <div style="margin-top:10px;font-size:34px;font-weight:700;color:#F6F7FB;">${amountLabel}</div>
          <div style="margin-top:8px;font-size:14px;color:#ABB2C0;line-height:1.6;">
            Purpose: ${escapeHtml(input.purpose)}
          </div>
          <div style="margin-top:6px;font-size:14px;color:#ABB2C0;line-height:1.6;">
            Recorded on: ${escapeHtml(formatOccurredAt(input.occurredAt))}
          </div>
          ${noteBlock}
        </div>
        <div style="margin-top:22px;padding:18px 20px;border-radius:22px;background:rgba(255,255,255,0.03);border:1px solid rgba(255,255,255,0.06);">
          <div style="font-size:12px;letter-spacing:0.12em;text-transform:uppercase;color:#8D95A6;margin-bottom:8px;">Shared by</div>
          <div style="font-size:15px;color:#F6F7FB;line-height:1.5;">${ownerMeta}</div>
        </div>
        <p style="margin:22px 0 0;color:#747D8E;font-size:13px;line-height:1.6;">
          This email was sent from Aeon as a private record summary. Confirm the amount directly with the sender before taking action.
        </p>
      </div>
    </div>
  `.trim();
}

function buildCounterpartyEmailText(input: CounterpartyShareEmailInput): string {
  const directionLabel = input.direction === "owed_to_me"
    ? `${input.ownerName} recorded that this amount is expected from you.`
    : `${input.ownerName} recorded that they owe you this amount.`;

  return [
    "Aeon finance record",
    "",
    `Hi ${input.recipientName},`,
    directionLabel,
    `Amount: ${formatCurrency(input.amount, input.currency)}`,
    `Purpose: ${input.purpose}`,
    `Recorded on: ${formatOccurredAt(input.occurredAt)}`,
    input.note ? `Note: ${input.note}` : null,
    "",
    `Shared by: ${input.ownerName}${input.ownerEmail ? ` (${input.ownerEmail})` : ""}`,
    "",
    "Confirm the amount directly with the sender before taking action."
  ].filter((line): line is string => typeof line === "string").join("\n");
}

function formatCurrency(amount: string, currency: string): string {
  try {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency,
      maximumFractionDigits: 2
    }).format(Number(amount));
  } catch {
    return `${amount} ${currency}`;
  }
}

function formatOccurredAt(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit"
  }).format(date);
}

function escapeHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#39;");
}
