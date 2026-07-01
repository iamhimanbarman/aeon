import { badRequest } from "./errors.js";
export function parseWithSchema(schema, value, message) {
    const result = schema.safeParse(value);
    if (!result.success) {
        throw badRequest(message, flattenZodError(result.error));
    }
    return result.data;
}
export function flattenZodError(error) {
    return {
        issues: error.issues.map((issue) => ({
            path: issue.path.join("."),
            message: issue.message,
            code: issue.code
        }))
    };
}
