import { randomUUID } from "node:crypto";

export function buildPrefixedId(prefix: string): string {
  return `${prefix}_${randomUUID().replaceAll("-", "")}`;
}

export function buildStableId(prefix: string, ...parts: string[]): string {
  return `${prefix}_${parts.join("_").replace(/[^a-zA-Z0-9_]/g, "_")}`;
}
