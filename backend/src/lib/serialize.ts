function camelKey(key: string): string {
  return key.replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase());
}

export function camelizeRecord<T extends Record<string, unknown>>(record: T): Record<string, unknown> {
  const output: Record<string, unknown> = {};

  for (const [key, value] of Object.entries(record)) {
    output[camelKey(key)] = value;
  }

  return output;
}

export function camelizeRows<T extends Record<string, unknown>>(rows: T[]): Array<Record<string, unknown>> {
  return rows.map(camelizeRecord);
}
