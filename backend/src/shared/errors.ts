const statusByCode = {
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  INVALID_CREDENTIALS: 401,
  INVALID_REFRESH_TOKEN: 401,
  FORBIDDEN: 403,
  ACCOUNT_REVOKED: 403,
  OWNER_MISMATCH: 403,
  NOT_FOUND: 404,
  ALREADY_EVALUATED: 409,
  ATTACHMENTS_INCOMPLETE: 409,
  REPORT_FINALIZED: 409,
  FILE_TOO_LARGE: 413,
  UNSUPPORTED_MEDIA_TYPE: 415,
  VALIDATION_ERROR: 422,
} as const;

export type ErrorCode = keyof typeof statusByCode;

export type ErrorDetail = Record<string, string>;

export class AppError extends Error {
  readonly code: ErrorCode;
  readonly details: ErrorDetail[];

  constructor(code: ErrorCode, message: string, details: ErrorDetail[] = []) {
    super(message);
    this.code = code;
    this.details = details;
  }

  get status(): number {
    return statusByCode[this.code];
  }
}

export function validationError(field: string, issue: string): AppError {
  return new AppError("VALIDATION_ERROR", "Invalid input", [{ field, issue }]);
}

export function isUniqueViolation(error: unknown): boolean {
  return typeof error === "object" && error !== null && "code" in error && error.code === "P2002";
}
