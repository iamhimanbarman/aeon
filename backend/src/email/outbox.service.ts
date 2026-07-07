import { createHash } from "node:crypto";
import type { FastifyBaseLogger } from "fastify";
import type { Sql } from "postgres";
import {
  getEmailDeliveryErrorCode,
  getEmailDeliveryErrorMessage,
  isRetryableEmailDeliveryError,
  sendFinanceCounterpartyEmail,
  type CounterpartyShareEmailInput
} from "./email.service.js";
import { markFinanceCounterpartyRecordsShared } from "../modules/finance/repository.js";

type FinanceEmailOutboxPayload = {
  input: CounterpartyShareEmailInput;
  recordIds: string[];
};

type EmailOutboxStatus = "pending" | "processing" | "sent" | "failed" | "dead";

type EmailOutboxRow = {
  id: string;
  user_id: string;
  email_kind: string;
  delivery_key: string;
  recipient_email: string;
  payload: FinanceEmailOutboxPayload;
  related_record_ids: string[];
  status: EmailOutboxStatus;
  attempts: number;
  max_attempts: number;
  next_attempt_at: string;
  last_attempt_at: string | null;
  sent_at: string | null;
  last_error_code: string | null;
  last_error_message: string | null;
  locked_at: string | null;
  created_at: string;
  updated_at: string;
};

type QueueFinanceEmailInput = {
  userId: string;
  deliveryKey: string;
  input: CounterpartyShareEmailInput;
  recordIds: string[];
};

type SendFinanceEmailWithQueueFallbackInput = QueueFinanceEmailInput & {
  logger: FastifyBaseLogger;
};

type FinanceEmailSendResult = {
  emailed: boolean;
  emailStatus: "sent" | "queued";
  emailSharedAt: string | null;
  emailQueuedAt: string | null;
  emailErrorCode: string | null;
};

const FINANCE_EMAIL_KIND = "finance_counterparty";
const MAX_DRAIN_BATCH_SIZE = 6;
const DRAIN_KICK_COOLDOWN_MS = 15_000;
const STALE_LOCK_MINUTES = 5;

let activeDrainPromise: Promise<void> | null = null;
let lastDrainKickAt = 0;

export async function sendFinanceEmailWithQueueFallback(
  db: Sql<Record<string, unknown>>,
  options: SendFinanceEmailWithQueueFallbackInput
): Promise<FinanceEmailSendResult> {
  try {
    await sendFinanceCounterpartyEmail(options.input);
    const emailSharedAt = new Date().toISOString();

    if (options.recordIds.length > 0) {
      await markFinanceCounterpartyRecordsShared(
        db,
        options.userId,
        options.recordIds,
        emailSharedAt
      );
    }

    return {
      emailed: true,
      emailStatus: "sent",
      emailSharedAt,
      emailQueuedAt: null,
      emailErrorCode: null
    };
  } catch (error) {
    const queuedAt = await queueFinanceEmailOutboxJob(db, {
      userId: options.userId,
      deliveryKey: options.deliveryKey,
      input: options.input,
      recordIds: options.recordIds
    });

    options.logger.warn(
      {
        userId: options.userId,
        deliveryKey: options.deliveryKey,
        recipientEmail: options.input.recipientEmail,
        recordCount: options.recordIds.length,
        errorCode: getEmailDeliveryErrorCode(error)
      },
      "finance_email_queued_for_retry"
    );

    kickFinanceEmailOutboxDrain(db, options.logger, { force: true });

    return {
      emailed: false,
      emailStatus: "queued",
      emailSharedAt: null,
      emailQueuedAt: queuedAt,
      emailErrorCode: getEmailDeliveryErrorCode(error)
    };
  }
}

export function kickFinanceEmailOutboxDrain(
  db: Sql<Record<string, unknown>>,
  logger: FastifyBaseLogger,
  options: { force?: boolean; limit?: number } = {}
): void {
  const now = Date.now();
  const force = options.force === true;

  if (activeDrainPromise != null) {
    return;
  }

  if (!force && now - lastDrainKickAt < DRAIN_KICK_COOLDOWN_MS) {
    return;
  }

  lastDrainKickAt = now;
  activeDrainPromise = drainFinanceEmailOutboxJobs(db, logger, options.limit ?? MAX_DRAIN_BATCH_SIZE)
    .catch((error) => {
      logger.error({ err: error }, "finance_email_outbox_drain_failed");
    })
    .finally(() => {
      activeDrainPromise = null;
    });
}

export function buildFinanceEmailDeliveryKey(
  kind: "record" | "settlement" | "manual",
  parts: string[]
): string {
  const normalized = parts.map((part) => part.trim()).join("|");
  const digest = createHash("sha256").update(`${kind}|${normalized}`).digest("hex").slice(0, 24);
  return `finance_${kind}_${digest}`;
}

export function computeFinanceEmailRetryDelayMs(attempt: number): number {
  const schedule = [45_000, 2 * 60_000, 5 * 60_000, 10 * 60_000, 20 * 60_000, 45 * 60_000, 90 * 60_000];
  return schedule[Math.min(Math.max(attempt - 1, 0), schedule.length - 1)] ?? schedule[schedule.length - 1] ?? 90 * 60_000;
}

async function queueFinanceEmailOutboxJob(
  db: Sql<Record<string, unknown>>,
  input: QueueFinanceEmailInput
): Promise<string> {
  const queuedAt = new Date().toISOString();

  await db.unsafe(
    `
      insert into email_outbox (
        user_id,
        email_kind,
        delivery_key,
        recipient_email,
        payload,
        related_record_ids,
        status,
        attempts,
        max_attempts,
        next_attempt_at,
        created_at,
        updated_at
      )
      values (
        $1::uuid,
        $2,
        $3,
        $4,
        $5::jsonb,
        $6::text[],
        'pending',
        0,
        8,
        $7::timestamptz,
        $7::timestamptz,
        $7::timestamptz
      )
      on conflict (user_id, delivery_key) do update
        set recipient_email = excluded.recipient_email,
            payload = excluded.payload,
            related_record_ids = excluded.related_record_ids,
            status = case when email_outbox.status = 'sent' then email_outbox.status else 'pending' end,
            next_attempt_at = case when email_outbox.status = 'sent' then email_outbox.next_attempt_at else excluded.next_attempt_at end,
            locked_at = null,
            updated_at = excluded.updated_at
    `,
    [
      input.userId,
      FINANCE_EMAIL_KIND,
      input.deliveryKey,
      input.input.recipientEmail,
      JSON.stringify({
        input: input.input,
        recordIds: input.recordIds
      } satisfies FinanceEmailOutboxPayload),
      input.recordIds,
      queuedAt
    ]
  );

  return queuedAt;
}

async function drainFinanceEmailOutboxJobs(
  db: Sql<Record<string, unknown>>,
  logger: FastifyBaseLogger,
  limit: number
): Promise<void> {
  const jobs = await claimDueFinanceEmailOutboxJobs(db, limit);

  for (const job of jobs) {
    await processFinanceEmailOutboxJob(db, logger, job);
  }
}

async function claimDueFinanceEmailOutboxJobs(
  db: Sql<Record<string, unknown>>,
  limit: number
): Promise<EmailOutboxRow[]> {
  return db.begin(async (tx) => {
    return tx.unsafe<EmailOutboxRow[]>(
      `
        with candidate as (
          select id
          from email_outbox
          where email_kind = $1
            and status in ('pending', 'failed')
            and attempts < max_attempts
            and next_attempt_at <= now()
            and (
              locked_at is null
              or locked_at < now() - interval '${STALE_LOCK_MINUTES} minutes'
            )
          order by next_attempt_at asc, created_at asc
          limit $2
          for update skip locked
        )
        update email_outbox as outbox
        set status = 'processing',
            locked_at = now(),
            updated_at = now()
        from candidate
        where outbox.id = candidate.id
        returning outbox.*
      `,
      [FINANCE_EMAIL_KIND, limit]
    );
  });
}

async function processFinanceEmailOutboxJob(
  db: Sql<Record<string, unknown>>,
  logger: FastifyBaseLogger,
  job: EmailOutboxRow
): Promise<void> {
  const attemptedAt = new Date().toISOString();

  try {
    const payload = parseFinanceEmailOutboxPayload(job.payload);
    await sendFinanceCounterpartyEmail(payload.input);

    const sentAt = new Date().toISOString();
    await db.begin(async (tx) => {
      if (payload.recordIds.length > 0) {
        await markFinanceCounterpartyRecordsShared(
          tx as unknown as Sql<Record<string, unknown>>,
          job.user_id,
          payload.recordIds,
          sentAt
        );
      }

      await tx`
        update email_outbox
        set status = 'sent',
            attempts = attempts + 1,
            last_attempt_at = ${attemptedAt}::timestamptz,
            sent_at = ${sentAt}::timestamptz,
            last_error_code = null,
            last_error_message = null,
            locked_at = null,
            updated_at = ${sentAt}::timestamptz
        where id = ${job.id}::uuid
      `;
    });
  } catch (error) {
    const retryable = isRetryableEmailDeliveryError(error);
    const nextAttempts = job.attempts + 1;
    const exhausted = nextAttempts >= job.max_attempts;
    const nextStatus: EmailOutboxStatus = retryable && !exhausted ? "failed" : "dead";
    const nextAttemptAt = new Date(
      Date.now() + computeFinanceEmailRetryDelayMs(nextAttempts)
    ).toISOString();
    const errorCode = getEmailDeliveryErrorCode(error);
    const errorMessage = getEmailDeliveryErrorMessage(error);

    await db`
      update email_outbox
      set status = ${nextStatus},
          attempts = ${nextAttempts},
          last_attempt_at = ${attemptedAt}::timestamptz,
          next_attempt_at = ${nextStatus === "dead" ? attemptedAt : nextAttemptAt}::timestamptz,
          last_error_code = ${errorCode},
          last_error_message = ${errorMessage.slice(0, 500)},
          locked_at = null,
          updated_at = now()
      where id = ${job.id}::uuid
    `;

    logger.warn(
      {
        err: error,
        jobId: job.id,
        deliveryKey: job.delivery_key,
        attempts: nextAttempts,
        status: nextStatus
      },
      "finance_email_outbox_job_failed"
    );
  }
}

function parseFinanceEmailOutboxPayload(value: unknown): FinanceEmailOutboxPayload {
  const normalizedValue = typeof value === "string"
    ? JSON.parse(value) as unknown
    : value;

  if (typeof normalizedValue !== "object" || normalizedValue == null) {
    throw new Error("Invalid finance email outbox payload.");
  }

  const candidate = normalizedValue as Partial<FinanceEmailOutboxPayload>;

  if (
    typeof candidate.input !== "object" ||
    candidate.input == null ||
    !Array.isArray(candidate.recordIds)
  ) {
    throw new Error("Invalid finance email outbox payload.");
  }

  return {
    input: candidate.input as CounterpartyShareEmailInput,
    recordIds: candidate.recordIds.filter((value): value is string => typeof value === "string" && value.trim().length > 0)
  };
}
