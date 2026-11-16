import { verifyAccessToken, type Principal, type Role } from "@/modules/auth/tokens";
import { AppError } from "./errors";

export async function authenticate(request: Request): Promise<Principal> {
  const [scheme, token] = (request.headers.get("authorization") ?? "").split(" ");
  if (scheme !== "Bearer" || !token) throw new AppError("UNAUTHORIZED", "Missing access token");
  return verifyAccessToken(token);
}

export function requireRole(principal: Principal, role: Role): void {
  if (principal.role !== role) throw new AppError("FORBIDDEN", "This action is not allowed for your role");
}
