import { randomUUID } from "node:crypto";
export function buildPrefixedId(prefix) {
    return `${prefix}_${randomUUID().replaceAll("-", "")}`;
}
export function buildStableId(prefix, ...parts) {
    return `${prefix}_${parts.join("_").replace(/[^a-zA-Z0-9_]/g, "_")}`;
}
