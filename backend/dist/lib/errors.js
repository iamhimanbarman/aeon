export class AppError extends Error {
    statusCode;
    code;
    details;
    constructor(statusCode, code, message, details) {
        super(message);
        this.name = "AppError";
        this.statusCode = statusCode;
        this.code = code;
        this.details = details;
    }
}
export function badRequest(message, details) {
    return new AppError(400, "bad_request", message, details);
}
export function unauthorized(message = "Authentication is required.") {
    return new AppError(401, "unauthorized", message);
}
export function forbidden(message = "You do not have access to this resource.") {
    return new AppError(403, "forbidden", message);
}
export function notFound(message) {
    return new AppError(404, "not_found", message);
}
export function conflict(message, details) {
    return new AppError(409, "conflict", message, details);
}
