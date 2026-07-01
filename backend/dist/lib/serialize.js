function camelKey(key) {
    return key.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase());
}
export function camelizeRecord(record) {
    const output = {};
    for (const [key, value] of Object.entries(record)) {
        output[camelKey(key)] = value;
    }
    return output;
}
export function camelizeRows(rows) {
    return rows.map(camelizeRecord);
}
