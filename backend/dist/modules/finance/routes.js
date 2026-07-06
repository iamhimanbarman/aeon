import { sendFinanceCounterpartyEmail } from "../../email/email.service.js";
import { parseMonthKey } from "../../lib/dates.js";
import { parseWithSchema } from "../../lib/validation.js";
import { archiveFinanceAccount, createFinanceCounterpartyRecord, createOrUpdateFinanceAccount, createOrUpdateFinanceCategory, createOrUpdateFinanceTransaction, deleteFinanceCategory, deleteFinanceTransaction, getFinanceOverview, getFinanceTransaction, listFinanceAccounts, listFinanceBudgets, listFinanceCategories, listFinanceTransactionMonths, listFinanceTransactions, markFinanceCounterpartyRecordShared, upsertFinanceCounterparty, setFinanceBudgetsForMonth } from "./repository.js";
import { financeAccountInputSchema, financeBudgetQuerySchema, financeCounterpartyInputSchema, financeCounterpartyRecordInputSchema, financeCounterpartyShareInputSchema, financeCategoryInputSchema, financeSetMonthBudgetSchema, financeTransactionInputSchema, financeTransactionMonthsQuerySchema, financeTransactionQuerySchema } from "./schemas.js";
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
        const ownerName = request.authUser?.displayName
            ?? request.authUser?.email
            ?? "An Aeon user";
        const result = await createFinanceCounterpartyRecord(app.db, request.authUser.userId, body);
        const existingEmailSharedAt = toIsoStringOrNull(result.record.emailSharedAt);
        let emailStatus = existingEmailSharedAt ? "already_sent" : "failed";
        let emailed = Boolean(existingEmailSharedAt);
        let emailSharedAt = existingEmailSharedAt;
        let emailErrorCode = null;
        if (!emailSharedAt) {
            try {
                await sendFinanceCounterpartyEmail({
                    recipientEmail: body.counterpartyEmail,
                    recipientName: body.counterpartyName,
                    ownerName,
                    ownerEmail: request.authUser?.email,
                    direction: body.direction,
                    purpose: body.purpose,
                    amount: body.amount,
                    currency: body.currency,
                    occurredAt: body.occurredAt,
                    note: body.note
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
    app.post("/counterparty-share", { preHandler: app.authenticate }, async (request) => {
        const body = parseWithSchema(financeCounterpartyShareInputSchema, request.body, "Invalid finance account-share payload.");
        const ownerName = request.authUser?.displayName
            ?? request.authUser?.email
            ?? "An Aeon user";
        await sendFinanceCounterpartyEmail({
            recipientEmail: body.counterpartyEmail,
            recipientName: body.counterpartyName,
            ownerName,
            ownerEmail: request.authUser?.email,
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
