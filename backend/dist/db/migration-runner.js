import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
export async function runMigrations(db, migrationsDir) {
    await db `
    create table if not exists schema_migrations (
      version text primary key,
      applied_at timestamptz not null default now()
    )
  `;
    const appliedRows = await db `
    select version
    from schema_migrations
  `;
    const applied = new Set(appliedRows.map((row) => row.version));
    const entries = (await readdir(migrationsDir, { withFileTypes: true }))
        .filter((entry) => entry.isFile() && entry.name.endsWith(".sql"))
        .map((entry) => entry.name)
        .sort();
    const executed = [];
    for (const entry of entries) {
        if (applied.has(entry)) {
            continue;
        }
        const source = await readFile(path.join(migrationsDir, entry), "utf8");
        await db.begin(async (tx) => {
            await tx.unsafe(source);
            await tx `
        insert into schema_migrations (version)
        values (${entry})
      `;
        });
        executed.push(entry);
    }
    return executed;
}
