import path from "node:path";
import { fileURLToPath } from "node:url";
import postgres from "postgres";
import { env } from "../config/env.js";
import { runMigrations } from "./migration-runner.js";

const hasSslMode = env.DATABASE_URL.includes("sslmode=");
const connectionString = hasSslMode ? env.DATABASE_URL : `${env.DATABASE_URL}?sslmode=require`;

async function main() {
  const db = postgres(connectionString, {
    max: 1,
    prepare: false
  });

  const currentDir = path.dirname(fileURLToPath(import.meta.url));
  const migrationsDir = path.resolve(currentDir, "../../db/migrations");

  try {
    const executed = await runMigrations(db, migrationsDir);

    if (executed.length === 0) {
      console.log("No new migrations.");
      return;
    }

    console.log(`Applied migrations: ${executed.join(", ")}`);
  } finally {
    await db.end({ timeout: 5 });
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
