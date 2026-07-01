import type { FastifyInstance } from "fastify";
import { parseMonthKey } from "../../lib/dates.js";
import { parseWithSchema } from "../../lib/validation.js";
import {
  archiveFinanceAccount,
  createOrUpdateFinanceAccount,
  createOrUpdateFinanceCategory,
  createOrUpdateFinanceTransaction,
  deleteFinanceCategory,
  deleteFinanceTransaction,
  getFinanceOverview,
  getFinanceTransaction,
  listFinanceAccounts,
  listFinanceBudgets,
  listFinanceCategories,
  listFinanceTransactions,
  setFinanceBudgetsForMonth
} from "./repository.js";
import {
  financeAccountInputSchema,
  financeBudgetQuerySchema,
  financeCategoryInputSchema,
  financeSetMonthBudgetSchema,
  financeTransactionInputSchema,
  financeTransactionQuerySchema
} from "./schemas.js";

export async function registerFinanceRoutes(app: FastifyInstance): Promise<void> {
  app.get("/categories", { preHandler: app.authenticate }, async (request) => {
    return listFinanceCategories(app.db, request.authUser!.userId);
  });

  app.post("/categories", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(financeCategoryInputSchema, request.body, "Invalid finance category payload.");
    return createOrUpdateFinanceCategory(app.db, request.authUser!.userId, body);
  });

  app.delete("/categories/:categoryId", { preHandler: app.authenticate }, async (request, reply) => {
    await deleteFinanceCategory(app.db, request.authUser!.userId, (request.params as { categoryId: string }).categoryId);
    return reply.status(204).send();
  });

  app.get("/accounts", { preHandler: app.authenticate }, async (request) => {
    return listFinanceAccounts(app.db, request.authUser!.userId);
  });

  app.post("/accounts", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(financeAccountInputSchema, request.body, "Invalid finance account payload.");
    return createOrUpdateFinanceAccount(app.db, request.authUser!.userId, body);
  });

  app.delete("/accounts/:accountId", { preHandler: app.authenticate }, async (request, reply) => {
    await archiveFinanceAccount(app.db, request.authUser!.userId, (request.params as { accountId: string }).accountId);
    return reply.status(204).send();
  });

  app.get("/transactions", { preHandler: app.authenticate }, async (request) => {
    const query = parseWithSchema(financeTransactionQuerySchema, request.query, "Invalid finance transaction query.");
    return listFinanceTransactions(app.db, request.authUser!.userId, query);
  });

  app.get("/transactions/:transactionId", { preHandler: app.authenticate }, async (request) => {
    return getFinanceTransaction(app.db, request.authUser!.userId, (request.params as { transactionId: string }).transactionId);
  });

  app.post("/transactions", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(financeTransactionInputSchema, request.body, "Invalid finance transaction payload.");
    return createOrUpdateFinanceTransaction(app.db, request.authUser!.userId, body);
  });

  app.delete("/transactions/:transactionId", { preHandler: app.authenticate }, async (request, reply) => {
    await deleteFinanceTransaction(app.db, request.authUser!.userId, (request.params as { transactionId: string }).transactionId);
    return reply.status(204).send();
  });

  app.get("/budgets", { preHandler: app.authenticate }, async (request) => {
    const query = parseWithSchema(financeBudgetQuerySchema, request.query, "Invalid finance budget query.");
    return listFinanceBudgets(app.db, request.authUser!.userId, query);
  });

  app.post("/budgets/month", { preHandler: app.authenticate }, async (request) => {
    const body = parseWithSchema(financeSetMonthBudgetSchema, request.body, "Invalid budget payload.");
    return setFinanceBudgetsForMonth(app.db, request.authUser!.userId, body);
  });

  app.get("/overview/:month", { preHandler: app.authenticate }, async (request) => {
    const month = parseMonthKey((request.params as { month: string }).month).monthKey;
    return getFinanceOverview(app.db, request.authUser!.userId, month);
  });
}
