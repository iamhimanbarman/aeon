import { sendFinanceCounterpartyEmail } from "../../email/email.service.js";
import { parseMonthKey } from "../../lib/dates.js";
import { parseWithSchema } from "../../lib/validation.js";
import { archiveFinanceAccount, createFinanceCounterpartyRecord, createOrUpdateFinanceAccount, createOrUpdateFinanceCategory, createOrUpdateFinanceTransaction, deleteFinanceCategory, deleteFinanceTransaction, getFinanceCounterpartyForEmail, getFinanceOverview, getFinanceTransaction, getFinanceLedgerOwnerProfile, listFinanceAccounts, listFinanceBudgets, listFinanceCategories, listFinanceCounterpartyRecordsByIdsForEmail, listOpenFinanceCounterpartyRecordsForEmail, listFinanceTransactionMonths, listFinanceTransactions, markFinanceCounterpartyRecordShared, markFinanceCounterpartyRecordsShared, shouldSendFinanceCounterpartyEmail, upsertFinanceCounterparty, setFinanceBudgetsForMonth } from "./repository.js";
import { financeAccountInputSchema, financeBudgetQuerySchema, financeCounterpartyInputSchema, financeCounterpartyManualEmailInputSchema, financeCounterpartyRecordInputSchema, financeCounterpartyShareInputSchema, financeCategoryInputSchema, financeSetMonthBudgetSchema, financeTransactionInputSchema, financeTransactionMonthsQuerySchema, financeTransactionQuerySchema } from "./schemas.js";
export async function registerFinanceRoutes(app) {
    app.get("/categories", { preHandler: app.authenticate }, async (request) => {
        return listFinanceCategories(app.db, request.authUser.userId);
    });
    app.post("/categories", { preHandler: app.authenticate }, async (request) => {
        const body = parseWithSchema(financeCategoryInputSchema, request.body, "Invalid finance category payload.");
        return createOrUpdateFinanceCategory(app.db, request.authUser.userId, body);
    });
    app.delete("/categories/:categoryId", { preHandler: app.authenticate }, async (request, reply) => {
        await deleteFinanceCategory(app.db, request.authUser.userId, request.params.categoryId);
        return reply.status(204).send();
    });
    app.get("/accounts", { preHandler: app.authenticate }, async (request) => {
        return listFinanceAccounts(app.db, request.authUser.userId);
    });
    app.post("/accounts", { preHandler: app.authenticate }, async (request) => {
        const body = parseWithSchema(financeAccountInputSchema, request.body, "Invalid finance account payload.");
        return createOrUpdateFinanceAccount(app.db, request.authUser.userId, body);
    });
    app.delete("/accounts/:accountId", { preHandler: app.authenticate }, async (request, reply) => {
        await archiveFinanceAccount(app.db, request.authUser.userId, request.params.accountId);
        return reply.status(204).send();
    });
    app.get("/transactions", { preHandler: app.authenticate }, async (request) => {
        const query = parseWithSchema(financeTransactionQuerySchema, request.query, "Invalid finance transaction query.");
        return listFinanceTransactions(app.db, request.authUser.userId, query);
    });
    app.get("/transaction-months", { preHandler: app.authenticate }, async (request) => {
        const query = parseWithSchema(financeTransactionMonthsQuerySchema, request.query, "Invalid finance transaction month query.");
        return listFinanceTransactionMonths(app.db, request.authUser.userId, query);
    });
    app.get("/transactions/:transactionId", { preHandler: app.authenticate }, async (request) => {
        return getFinanceTransaction(app.db, request.authUser.userId, request.params.transactionId);
    });
    app.post("/transactions", { preHandler: app.authenticate }, async (request) => {
        const body = parseWithSchema(financeTransactionInputSchema, request.body, "Invalid finance transaction payload.");
        return createOrUpdateFinanceTransaction(app.db, request.authUser.userId, body);
    });
    app.delete("/transactions/:transactionId", { preHandler: app.authenticate }, async (request, reply) => {
        await deleteFinanceTransaction(app.db, request.authUser.userId, request.params.transactionId);
        return reply.status(204).send();
    });
    app.get("/budgets", { preHandler: app.authenticate }, async (request) => {
        const query = parseWithSchema(financeBudgetQuerySchema, request.query, "Invalid finance budget query.");
        return listFinanceBudgets(app.db, request.authUser.userId, query);
    });
    app.post("/budgets/month", { preHandler: app.authenticate }, async (request) => {
        const body = parseWithSchema(financeSetMonthBudgetSchema, request.body, "Invalid budget payload.");
        return setFinanceBudgetsForMonth(app.db, request.authUser.userId, body);
    });
    app.get("/overview/:month", { preHandler: app.authenticate }, async (request) => {
        const month = parseMonthKey(request.params.month).monthKey;
        return getFinanceOverview(app.db, request.authUser.userId, month);
    });
    app.post("/counterparties", { preHandler: app.authenticate }, async (request) => {
        const body = parseWithSchema(financeCounterpartyInputSchema, request.body, "Invalid finance counterparty payload.");
        return upsertFinanceCounterparty(app.db, request.authUser.userId, body);
    });
    app.post("/counterparty-records", { preHandler: app.authenticate }, async (request) => {
        const body = parseWithSchema(financeCounterpartyRecordInputSchema, request.body, "Invalid finance counterparty record payload.");
        const ownerProfile = await getFinanceLedgerOwnerProfile(app.db, request.authUser.userId);
        const ownerEmail = ownerProfile.email ?? request.authUser?.email;
        const ownerName = resolveFinanceLedgerOwnerName(ownerProfile.displayName, request.authUser?.displayName, ownerEmail);
        const result = await createFinanceCounterpartyRecord(app.db, request.authUser.userId, body);
        const existingEmailSharedAt = toIsoStringOrNull(result.record.emailSharedAt);
        const shouldEmail = shouldSendFinanceCounterpartyEmail(result.counterparty.emailSharePreference, body.direction);
        let emailStatus = existingEmailSharedAt
            ? "already_sent"
            : shouldEmail
                ? "failed"
                : "skipped";
        let emailed = Boolean(existingEmailSharedAt);
        let emailSharedAt = existingEmailSharedAt;
        let emailErrorCode = null;
        if (!emailSharedAt && shouldEmail) {
            try {
                const openRecords = await listOpenFinanceCounterpartyRecordsForEmail(app.db, request.authUser.userId, String(result.counterparty.id));
                await sendFinanceCounterpartyEmail({
                    recipientEmail: body.counterpartyEmail,
                    recipientName: body.counterpartyName,
                    ownerName,
                    ownerEmail,
                    direction: body.direction,
                    purpose: body.purpose,
                    amount: body.amount,
                    currency: body.currency,
                    occurredAt: body.occurredAt,
                    note: body.note,
                    newRecordId: result.recordId,
                    openRecords
                });
                emailed = true;
                emailStatus = "sent";
                emailSharedAt = new Date().toISOString();
                await markFinanceCounterpartyRecordShared(app.db, request.authUser.userId, result.recordId, emailSharedAt);
            }
            catch (error) {
                emailStatus = "failed";
                emailErrorCode = error instanceof Error && "code" in error
                    ? String(error.code ?? "email_delivery_failed")
                    : "email_delivery_failed";
                app.log.warn({
                    err: error,
                    userId: request.authUser.userId,
                    recordId: result.recordId
                }, "finance_counterparty_email_failed");
            }
        }
        return {
            ok: true,
            emailed,
            emailStatus,
            emailSharedAt,
            emailErrorCode,
            counterparty: result.counterparty,
            record: {
                ...result.record,
                emailSharedAt
            }
        };
    });
    app.post("/counterparty-record-emails", { preHandler: app.authenticate }, async (request) => {
        const body = parseWithSchema(financeCounterpartyManualEmailInputSchema, request.body, "Invalid finance counterparty email payload.");
        const userId = request.authUser.userId;
        const ownerProfile = await getFinanceLedgerOwnerProfile(app.db, userId);
        const ownerEmail = ownerProfile.email ?? request.authUser?.email;
        const ownerName = resolveFinanceLedgerOwnerName(ownerProfile.displayName, request.authUser?.displayName, ownerEmail);
        const counterparty = await getFinanceCounterpartyForEmail(app.db, userId, body.counterpartyId);
        const records = await listFinanceCounterpartyRecordsByIdsForEmail(app.db, userId, body);
        const currency = records[0]?.currency ?? "INR";
        const owedToOwner = records
            .filter((record) => record.direction === "owed_to_me")
            .reduce((total, record) => total + Number(record.amount), 0);
        const ownerOwes = records
            .filter((record) => record.direction === "i_owe")
            .reduce((total, record) => total + Number(record.amount), 0);
        const net = owedToOwner - ownerOwes;
        const emailSharedAt = new Date().toISOString();
        try {
            await sendFinanceCounterpartyEmail({
                recipientEmail: counterparty.email,
                recipientName: counterparty.name,
                ownerName,
                ownerEmail,
                mode: "manual_summary",
                direction: net >= 0 ? "owed_to_me" : "i_owe",
                purpose: `${records.length} selected ledger record${records.length === 1 ? "" : "s"}`,
                amount: Math.abs(net).toFixed(2),
                currency,
                occurredAt: emailSharedAt,
                letterMessage: body.message,
                statementRecords: records
            });
            await markFinanceCounterpartyRecordsShared(app.db, userId, body.recordIds, emailSharedAt);
            return {
                ok: true,
                emailed: true,
                emailStatus: "sent",
                emailSharedAt,
                recordIds: body.recordIds,
                recipientEmail: counterparty.email
            };
        }
        catch (error) {
            app.log.warn({
                err: error,
                userId,
                counterpartyId: body.counterpartyId,
                recordCount: body.recordIds.length
            }, "finance_counterparty_manual_email_failed");
            throw error;
        }
    });
    app.post("/counterparty-share", { preHandler: app.authenticate }, async (request) => {
        const body = parseWithSchema(financeCounterpartyShareInputSchema, request.body, "Invalid finance account-share payload.");
        const ownerProfile = await getFinanceLedgerOwnerProfile(app.db, request.authUser.userId);
        const ownerEmail = ownerProfile.email ?? request.authUser?.email;
        const ownerName = resolveFinanceLedgerOwnerName(ownerProfile.displayName, request.authUser?.displayName, ownerEmail);
        if (!shouldSendFinanceCounterpartyEmail(body.emailSharePreference, body.direction)) {
            return {
                ok: true,
                emailed: false,
                emailStatus: "skipped",
                recipientEmail: body.counterpartyEmail
            };
        }
        await sendFinanceCounterpartyEmail({
            recipientEmail: body.counterpartyEmail,
            recipientName: body.counterpartyName,
            ownerName,
            ownerEmail,
            direction: body.direction,
            purpose: body.purpose,
            amount: body.amount,
            currency: body.currency,
            occurredAt: body.occurredAt,
            note: body.note
        });
        return {
            ok: true,
            emailed: true,
            recipientEmail: body.counterpartyEmail
        };
    });
}
function toIsoStringOrNull(value) {
    if (value instanceof Date && !Number.isNaN(value.getTime())) {
        return value.toISOString();
    }
    if (typeof value === "string" && value.trim().length > 0) {
        const parsed = new Date(value);
        return Number.isNaN(parsed.getTime()) ? value : parsed.toISOString();
    }
    return null;
}
function resolveFinanceLedgerOwnerName(databaseDisplayName, tokenDisplayName, email) {
    const name = databaseDisplayName?.trim() || tokenDisplayName?.trim();
    if (name) {
        return name;
    }
    const emailName = email
        ?.split("@")[0]
        ?.replace(/[._-]+/g, " ")
        .replace(/\s+/g, " ")
        .trim();
    return emailName || "Aeon member";
}
