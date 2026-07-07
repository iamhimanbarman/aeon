import { env } from "../config/env.js";
import { AppError } from "../lib/errors.js";
import {
  buildFinanceLedgerStatementPdf,
  type FinanceLedgerStatementRecord
} from "./finance-ledger-pdf.js";

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
  mode?: "new_record" | "manual_summary" | undefined;
  direction: "owed_to_me" | "i_owe";
  purpose: string;
  amount: string;
  currency: string;
  occurredAt: string;
  note?: string | undefined;
  letterMessage?: string | undefined;
  newRecordId?: string | undefined;
  openRecords?: FinanceLedgerStatementRecord[] | undefined;
  statementRecords?: FinanceLedgerStatementRecord[] | undefined;
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

  const attachments = await buildCounterpartyEmailAttachments(input);
  const payload: Record<string, unknown> = {
    from: env.EMAIL_FROM,
    to: [input.recipientEmail],
    subject: input.mode === "manual_summary"
      ? `${input.ownerName} shared an Aeon ledger statement`
      : `${input.ownerName} shared an Aeon ledger update`,
    html: buildCounterpartyEmailHtml(input),
    text: buildCounterpartyEmailText(input)
  };

  if (attachments.length > 0) {
    payload.attachments = attachments;
  }

  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${env.RESEND_API_KEY}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
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

async function buildCounterpartyEmailAttachments(
  input: CounterpartyShareEmailInput
): Promise<Array<{ filename: string; content: string }>> {
  const records = getStatementRecords(input);

  if (records.length === 0) {
    return [];
  }

  const pdf = await buildFinanceLedgerStatementPdf({
    ownerName: input.ownerName,
    ownerEmail: input.ownerEmail,
    recipientName: input.recipientName,
    newRecordId: input.newRecordId,
    records,
    statementTitle: input.mode === "manual_summary" ? "Selected ledger records" : "Open ledger account",
    tableTitle: input.mode === "manual_summary" ? "Selected records" : "Open records",
    letterMessage: input.letterMessage
  });

  return [
    {
      filename: buildStatementFilename(input.ownerName, input.recipientName),
      content: pdf.toString("base64")
    }
  ];
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
  const summary = buildLedgerEmailSummary(input);
  const isManualSummary = input.mode === "manual_summary";
  const amountLabel = isManualSummary ? summary.netAmountLabel : formatCurrency(input.amount, input.currency);
  const directionLabel = isManualSummary
    ? "Selected statement net"
    : input.direction === "owed_to_me"
      ? "Expected from you"
      : "Owed to you";
  const noteBlock = input.note
    ? `
        <div style="margin-top:16px;border-top:1px solid rgba(255,255,255,0.08);padding-top:14px;">
          <div style="font-size:11px;letter-spacing:0.16em;text-transform:uppercase;color:#7B8395;margin-bottom:7px;">Note</div>
          <div style="font-size:14px;line-height:1.6;color:#E9ECF7;">${escapeHtml(input.note)}</div>
        </div>
      `.trim()
    : "";
  const letterBlock = input.letterMessage
    ? `
        <div style="margin-top:18px;padding:18px 20px;border-radius:24px;background:#10131D;border:1px solid rgba(255,255,255,0.08);">
          <div style="font-size:11px;letter-spacing:0.16em;text-transform:uppercase;color:#F5C542;margin-bottom:10px;font-weight:700;">Message from ${escapeHtml(input.ownerName)}</div>
          <div style="font-size:15px;line-height:1.7;color:#E9ECF7;">${escapeHtml(input.letterMessage)}</div>
        </div>
      `.trim()
    : "";
  const ownerMeta = input.ownerEmail
    ? `${escapeHtml(input.ownerName)} | ${escapeHtml(input.ownerEmail)}`
    : escapeHtml(input.ownerName);
  const pdfLine = summary.hasStatement
    ? `
        <div style="margin-top:22px;padding:14px 0;border-top:1px solid rgba(245,197,66,0.28);border-bottom:1px solid rgba(245,197,66,0.20);">
          <div style="font-size:13px;line-height:1.6;color:#F5C542;font-weight:700;">PDF statement attached</div>
          <div style="font-size:13px;line-height:1.6;color:#AEB5C7;">It includes ${isManualSummary ? "the selected records, sender message, and account total." : "previous open records, this new record, and the current total."}</div>
        </div>
      `.trim()
    : "";
  const intro = isManualSummary
    ? `Hi ${escapeHtml(input.recipientName)}, ${escapeHtml(input.ownerName)} shared selected ledger records with you.`
    : `Hi ${escapeHtml(input.recipientName)}, ${escapeHtml(input.ownerName)} added a new ledger record with you.`;
  const primaryLabel = isManualSummary ? "Selected records" : "New record";
  const purposeLine = isManualSummary
    ? `${summary.recordCount} selected record${summary.recordCount === 1 ? "" : "s"} are included in this email and PDF.`
    : `Purpose: ${escapeHtml(input.purpose)}`;

  return `
    <div style="margin:0;background:#07080C;color:#F8FAFC;font-family:Arial,Helvetica,sans-serif;">
      <div style="max-width:620px;margin:0 auto;padding:38px 26px 42px;">
        <div style="display:inline-block;padding:10px 18px;border-radius:999px;background:#2B2414;border:1px solid rgba(245,197,66,0.34);color:#F5C542;font-size:12px;letter-spacing:0.18em;text-transform:uppercase;font-weight:700;">
          Aeon finance record
        </div>
        <h1 style="margin:24px 0 14px;font-size:42px;line-height:1.05;letter-spacing:-0.04em;color:#F8FAFC;">
          ${isManualSummary ? "Shared ledger statement" : "Shared account update"}
        </h1>
        <p style="margin:0 0 28px;color:#B6BDCD;font-size:18px;line-height:1.65;">
          ${intro}
        </p>

        <div style="padding:24px;border-radius:30px;background:#121622;border:1px solid rgba(255,255,255,0.08);">
          <div style="display:inline-block;padding:7px 12px;border-radius:999px;background:#2B2414;border:1px solid rgba(245,197,66,0.28);font-size:11px;letter-spacing:0.16em;text-transform:uppercase;color:#F5C542;font-weight:700;">${primaryLabel}</div>
          <div style="margin-top:16px;font-size:13px;color:#8E96A8;text-transform:uppercase;letter-spacing:0.16em;">${directionLabel}</div>
          <div style="margin-top:8px;font-size:42px;font-weight:800;letter-spacing:-0.04em;color:#F8FAFC;">${amountLabel}</div>
          <div style="margin-top:10px;font-size:15px;color:#B6BDCD;line-height:1.7;">
            ${purposeLine}
          </div>
          <div style="margin-top:4px;font-size:15px;color:#B6BDCD;line-height:1.7;">
            Recorded on: ${escapeHtml(formatOccurredAt(input.occurredAt))}
          </div>
          ${noteBlock}
        </div>

        ${letterBlock}

        <div style="margin-top:18px;padding:22px 24px;border-radius:28px;background:#0F121B;border:1px solid rgba(139,108,255,0.28);">
          <div style="font-size:12px;letter-spacing:0.16em;text-transform:uppercase;color:#8E96A8;margin-bottom:8px;">${isManualSummary ? "Selected total" : "Current open total"}</div>
          <div style="font-size:34px;font-weight:800;letter-spacing:-0.03em;color:#F8FAFC;">${escapeHtml(summary.netAmountLabel)}</div>
          <div style="margin-top:6px;font-size:14px;color:#B6BDCD;line-height:1.6;">${escapeHtml(summary.netText)} across ${summary.recordCount} ${isManualSummary ? "selected" : "open"} record${summary.recordCount === 1 ? "" : "s"}.</div>
        </div>

        ${pdfLine}

        <div style="margin-top:24px;">
          <div style="font-size:11px;letter-spacing:0.16em;text-transform:uppercase;color:#747D8E;margin-bottom:8px;">Shared by</div>
          <div style="font-size:15px;color:#F8FAFC;line-height:1.6;">${ownerMeta}</div>
        </div>

        <p style="margin:24px 0 0;color:#747D8E;font-size:13px;line-height:1.65;">
          This email was sent from Aeon as a private record summary. Confirm the amount directly with the sender before taking action.
        </p>
      </div>
    </div>
  `.trim();
}

function buildCounterpartyEmailText(input: CounterpartyShareEmailInput): string {
  const summary = buildLedgerEmailSummary(input);
  const isManualSummary = input.mode === "manual_summary";
  const directionLabel = isManualSummary
    ? "Selected statement net"
    : input.direction === "owed_to_me"
      ? "Expected from you"
      : "Owed to you";

  return [
    "Aeon finance record",
    "",
    `Hi ${input.recipientName},`,
    isManualSummary
      ? `${input.ownerName} shared selected ledger records with you.`
      : `${input.ownerName} added a new ledger record with you.`,
    "",
    isManualSummary ? "Selected records" : "New record",
    `${directionLabel}: ${isManualSummary ? summary.netAmountLabel : formatCurrency(input.amount, input.currency)}`,
    isManualSummary ? `Selected records: ${summary.recordCount}` : `Purpose: ${input.purpose}`,
    `Recorded on: ${formatOccurredAt(input.occurredAt)}`,
    input.note ? `Note: ${input.note}` : null,
    input.letterMessage ? `Message: ${input.letterMessage}` : null,
    "",
    isManualSummary ? "Selected total" : "Current open total",
    `${summary.netText}: ${summary.netAmountLabel}`,
    `${isManualSummary ? "Selected" : "Open"} records: ${summary.recordCount}`,
    summary.hasStatement ? "A PDF statement is attached with the ledger records and total." : null,
    "",
    `Shared by: ${input.ownerName}${input.ownerEmail ? ` (${input.ownerEmail})` : ""}`,
    "",
    "Confirm the amount directly with the sender before taking action."
  ].filter((line): line is string => typeof line === "string").join("\n");
}

function buildLedgerEmailSummary(input: CounterpartyShareEmailInput): {
  netText: string;
  netAmountLabel: string;
  recordCount: number;
  hasStatement: boolean;
} {
  const fallbackRecord: FinanceLedgerStatementRecord = {
    id: input.newRecordId ?? "new_record",
    direction: input.direction,
    purpose: input.purpose,
    note: input.note ?? null,
    amount: input.amount,
    currency: input.currency,
    status: "open",
    occurredAt: input.occurredAt,
    createdAt: input.occurredAt
  };
  const records = getStatementRecords(input);
  const summaryRecords = records.length ? records : [fallbackRecord];
  const currency = summaryRecords[0]?.currency ?? input.currency;
  const owedToOwner = summaryRecords
    .filter((record) => record.direction === "owed_to_me")
    .reduce((total, record) => total + Number(record.amount), 0);
  const ownerOwes = summaryRecords
    .filter((record) => record.direction === "i_owe")
    .reduce((total, record) => total + Number(record.amount), 0);
  const netRecipientOwes = owedToOwner - ownerOwes;
  const base = {
    recordCount: summaryRecords.length,
    hasStatement: records.length > 0
  };

  if (netRecipientOwes > 0) {
    return {
      ...base,
      netText: `You owe ${input.ownerName}`,
      netAmountLabel: formatCurrency(netRecipientOwes, currency)
    };
  }

  if (netRecipientOwes < 0) {
    return {
      ...base,
      netText: `${input.ownerName} owes you`,
      netAmountLabel: formatCurrency(Math.abs(netRecipientOwes), currency)
    };
  }

  return {
    ...base,
    netText: "Open balance is settled",
    netAmountLabel: formatCurrency(0, currency)
  };
}

function getStatementRecords(input: CounterpartyShareEmailInput): FinanceLedgerStatementRecord[] {
  return input.statementRecords ?? input.openRecords ?? [];
}

function buildStatementFilename(ownerName: string, recipientName: string): string {
  return `aeon-ledger-${toFilenamePart(ownerName)}-${toFilenamePart(recipientName)}.pdf`;
}

function toFilenamePart(value: string): string {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 40)
    || "account";
}

function formatCurrency(amount: string | number, currency: string): string {
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
