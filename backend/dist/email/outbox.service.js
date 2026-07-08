import { createHash } from "node:crypto";
import { getEmailDeliveryErrorCode, getEmailDeliveryErrorMessage, isRetryableEmailDeliveryError, sendFinanceCounterpartyEmail } from "./email.service.js";
import { markFinanceCounterpartyRecordsShared } from "../modules/finance/repository.js";
const FINANCE_EMAIL_KIND = "finance_counterparty";
const MAX_DRAIN_BATCH_SIZE = 6;
const DRAIN_KICK_COOLDOWN_MS = 15_000;
const OUTBOX_POLL_INTERVAL_MS = 30_000;
const MAX_DRAIN_ROUNDS_PER_RUN = 4;
const STALE_LOCK_MINUTES = 5;
let activeDrainPromise = null;
let lastDrainKickAt = 0;
let rerunDrainAfterActiveRun = false;
export async function sendFinanceEmailWithQueueFallback(db, options) {
    try {
        await sendFinanceCounterpartyEmail(options.input);
        const emailSharedAt = new Date().toISOString();
        if (options.recordIds.length > 0) {
            await markFinanceCounterpartyRecordsShared(db, options.userId, options.recordIds, emailSharedAt);
        }
        return {
            emailed: true,
            emailStatus: "sent",
            emailSharedAt,
            emailQueuedAt: null,
            emailErrorCode: null
        };
    }
    catch (error) {
        const errorCode = getEmailDeliveryErrorCode(error);
        if (resolveFinanceEmailFailureDisposition(error) === "fail") {
            options.logger.warn({
                userId: options.userId,
                deliveryKey: options.deliveryKey,
                recipientEmail: options.input.recipientEmail,
                recordCount: options.recordIds.length,
                errorCode
            }, "finance_email_delivery_failed_without_retry");
            return {
                emailed: false,
                emailStatus: "failed",
                emailSharedAt: null,
                emailQueuedAt: null,
                emailErrorCode: errorCode
            };
        }
        const queuedAt = await queueFinanceEmailOutboxJob(db, {
            userId: options.userId,
            deliveryKey: options.deliveryKey,
            input: options.input,
            recordIds: options.recordIds
        });
        options.logger.warn({
            userId: options.userId,
            deliveryKey: options.deliveryKey,
            recipientEmail: options.input.recipientEmail,
            recordCount: options.recordIds.length,
            errorCode
        }, "finance_email_queued_for_retry");
        kickFinanceEmailOutboxDrain(db, options.logger, { force: true });
        return {
            emailed: false,
            emailStatus: "queued",
            emailSharedAt: null,
            emailQueuedAt: queuedAt,
            emailErrorCode: errorCode
        };
    }
}
export function kickFinanceEmailOutboxDrain(db, logger, options = {}) {
    const now = Date.now();
    const force = options.force === true;
    const limit = Math.max(1, options.limit ?? MAX_DRAIN_BATCH_SIZE);
    if (activeDrainPromise != null) {
        if (force) {
            rerunDrainAfterActiveRun = true;
        }
        return;
    }
    if (!force && now - lastDrainKickAt < DRAIN_KICK_COOLDOWN_MS) {
        return;
    }
    lastDrainKickAt = now;
    activeDrainPromise = drainFinanceEmailOutboxJobs(db, logger, limit)
        .catch((error) => {
        logger.error({ err: error }, "finance_email_outbox_drain_failed");
    })
        .finally(() => {
        activeDrainPromise = null;
        if (rerunDrainAfterActiveRun) {
            rerunDrainAfterActiveRun = false;
            queueMicrotask(() => {
                kickFinanceEmailOutboxDrain(db, logger, { force: true, limit });
            });
        }
    });
}
export function startFinanceEmailOutboxScheduler(db, logger, options = {}) {
    const intervalMs = Math.max(5_000, options.intervalMs ?? OUTBOX_POLL_INTERVAL_MS);
    const timer = setInterval(() => {
        kickFinanceEmailOutboxDrain(db, logger, {
            force: true,
            limit: options.limit ?? MAX_DRAIN_BATCH_SIZE
        });
    }, intervalMs);
    timer.unref?.();
    return () => {
        clearInterval(timer);
    };
}
export function buildFinanceEmailDeliveryKey(kind, parts) {
    const normalized = parts.map((part) => part.trim()).join("|");
    const digest = createHash("sha256").update(`${kind}|${normalized}`).digest("hex").slice(0, 24);
    return `finance_${kind}_${digest}`;
}
export function computeFinanceEmailRetryDelayMs(attempt) {
    const schedule = [45_000, 2 * 60_000, 5 * 60_000, 10 * 60_000, 20 * 60_000, 45 * 60_000, 90 * 60_000];
    return schedule[Math.min(Math.max(attempt - 1, 0), schedule.length - 1)] ?? schedule[schedule.length - 1] ?? 90 * 60_000;
}
export function resolveFinanceEmailFailureDisposition(error) {
    return isRetryableEmailDeliveryError(error) ? "queue" : "fail";
}
export function shouldContinueFinanceEmailDrain(claimedCount, limit, rerunRequested) {
    return claimedCount >= Math.max(limit, 1) || rerunRequested;
}
async function queueFinanceEmailOutboxJob(db, input) {
    const queuedAt = new Date().toISOString();
    await db.unsafe(`
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
    `, [
        input.userId,
        FINANCE_EMAIL_KIND,
        input.deliveryKey,
        input.input.recipientEmail,
        JSON.stringify({
            input: input.input,
            recordIds: input.recordIds
        }),
        input.recordIds,
        queuedAt
    ]);
    return queuedAt;
}
async function drainFinanceEmailOutboxJobs(db, logger, limit) {
    for (let round = 0; round < MAX_DRAIN_ROUNDS_PER_RUN; round += 1) {
        const jobs = await claimDueFinanceEmailOutboxJobs(db, limit);
        if (jobs.length === 0) {
            return;
        }
        for (const job of jobs) {
            await processFinanceEmailOutboxJob(db, logger, job);
        }
        const rerunRequested = rerunDrainAfterActiveRun;
        rerunDrainAfterActiveRun = false;
        if (!shouldContinueFinanceEmailDrain(jobs.length, limit, rerunRequested)) {
            return;
        }
    }
}
async function claimDueFinanceEmailOutboxJobs(db, limit) {
    return db.begin(async (tx) => {
        return tx.unsafe(`
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
      `, [FINANCE_EMAIL_KIND, limit]);
    });
}
async function processFinanceEmailOutboxJob(db, logger, job) {
    const attemptedAt = new Date().toISOString();
    try {
        const payload = parseFinanceEmailOutboxPayload(job.payload);
        await sendFinanceCounterpartyEmail(payload.input);
        const sentAt = new Date().toISOString();
        await db.begin(async (tx) => {
            if (payload.recordIds.length > 0) {
                await markFinanceCounterpartyRecordsShared(tx, job.user_id, payload.recordIds, sentAt);
            }
            await tx `
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
    }
    catch (error) {
        const retryable = isRetryableEmailDeliveryError(error);
        const nextAttempts = job.attempts + 1;
        const exhausted = nextAttempts >= job.max_attempts;
        const nextStatus = retryable && !exhausted ? "failed" : "dead";
        const nextAttemptAt = new Date(Date.now() + computeFinanceEmailRetryDelayMs(nextAttempts)).toISOString();
        const errorCode = getEmailDeliveryErrorCode(error);
        const errorMessage = getEmailDeliveryErrorMessage(error);
        await db `
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
        logger.warn({
            err: error,
            jobId: job.id,
            deliveryKey: job.delivery_key,
            attempts: nextAttempts,
            status: nextStatus
        }, "finance_email_outbox_job_failed");
    }
}
function parseFinanceEmailOutboxPayload(value) {
    const normalizedValue = typeof value === "string"
        ? JSON.parse(value)
        : value;
    if (typeof normalizedValue !== "object" || normalizedValue == null) {
        throw new Error("Invalid finance email outbox payload.");
    }
    const candidate = normalizedValue;
    if (typeof candidate.input !== "object" ||
        candidate.input == null ||
        !Array.isArray(candidate.recordIds)) {
        throw new Error("Invalid finance email outbox payload.");
    }
    return {
        input: candidate.input,
        recordIds: candidate.recordIds.filter((value) => typeof value === "string" && value.trim().length > 0)
    };
}
