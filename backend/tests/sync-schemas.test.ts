import { describe, expect, it } from "vitest";
import {
  syncPullQuerySchema,
  syncPushSchema
} from "../src/modules/sync/schemas.js";

describe("sync schemas", () => {
  it("accepts a valid idempotent sync push batch", () => {
    const parsed = syncPushSchema.parse({
      clientId: "android-device-1",
      changes: [
        {
          idempotencyKey: "idem_123456789",
          entityType: "finance_transactions",
          entityId: "txn_local_1",
          operation: "create",
          payload: {
            title: "Lunch",
            amount: "120.00"
          },
          baseRevision: null
        }
      ]
    });

    expect(parsed.changes[0]?.entityType).toBe("finance_transactions");
  });

  it("rejects unknown entity types", () => {
    expect(() => syncPushSchema.parse({
      clientId: "android-device-1",
      changes: [
        {
          idempotencyKey: "idem_123456789",
          entityType: "other_users_private_table",
          entityId: "x",
          operation: "create",
          payload: {}
        }
      ]
    })).toThrow();
  });

  it("limits push batches to 100 changes", () => {
    expect(() => syncPushSchema.parse({
      clientId: "android-device-1",
      changes: Array.from({ length: 101 }, (_, index) => ({
        idempotencyKey: `idem_${index}_123456789`,
        entityType: "tasks",
        entityId: `task_${index}`,
        operation: "update",
        payload: {}
      }))
    })).toThrow();
  });

  it("parses filtered pull queries", () => {
    const parsed = syncPullQuerySchema.parse({
      cursor: "10",
      limit: "25",
      entityTypes: "tasks, finance_transactions"
    });

    expect(parsed).toEqual({
      cursor: 10,
      limit: 25,
      entityTypes: ["tasks", "finance_transactions"]
    });
  });
});
