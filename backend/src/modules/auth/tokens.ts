import { createHash, randomBytes } from "node:crypto";
import { SignJWT, jwtVerify } from "jose";
import { env } from "@/shared/env";
import { AppError } from "@/shared/errors";

export type Role = "FIELD_OFFICER" | "EVALUATOR";

export type Principal = { userId: string; role: Role };

export async function signAccessToken(userId: string, role: Role): Promise<string> {
  return new SignJWT({ role })
    .setProtectedHeader({ alg: "HS256" })
    .setSubject(userId)
    .setIssuedAt()
    .setExpirationTime(`${env.accessTokenTtlSeconds}s`)
    .sign(env.jwtSecret);
}

export async function verifyAccessToken(token: string): Promise<Principal> {
  const payload = await jwtVerify(token, env.jwtSecret, { algorithms: ["HS256"] }).then(
    (result) => result.payload,
    () => null,
  );
  if (payload && typeof payload.sub === "string" && (payload.role === "FIELD_OFFICER" || payload.role === "EVALUATOR")) {
    return { userId: payload.sub, role: payload.role };
  }
  throw new AppError("UNAUTHORIZED", "Invalid or expired access token");
}

export function newRefreshToken(): string {
  return randomBytes(32).toString("base64url");
}

export function hashToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}
