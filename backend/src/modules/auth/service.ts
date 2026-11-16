import bcrypt from "bcryptjs";
import { prisma } from "@/shared/db";
import { env } from "@/shared/env";
import { AppError } from "@/shared/errors";
import { hashToken, newRefreshToken, signAccessToken, type Role } from "./tokens";

type SessionUser = { id: string; username: string; displayName: string; role: Role };

const dummyHash = bcrypt.hashSync("unused-password", 10);

function refreshExpiry(): Date {
  return new Date(Date.now() + env.refreshTokenTtlDays * 24 * 60 * 60 * 1000);
}

async function session(user: SessionUser, refreshToken: string) {
  return {
    accessToken: await signAccessToken(user.id, user.role),
    accessTokenExpiresIn: env.accessTokenTtlSeconds,
    refreshToken,
    user: { id: user.id, username: user.username, displayName: user.displayName, role: user.role },
  };
}

export async function login(username: string, password: string, deviceId: string) {
  const user = await prisma.user.findUnique({ where: { username } });
  const passwordMatches = await bcrypt.compare(password, user?.passwordHash ?? dummyHash);
  if (!user || !passwordMatches) throw new AppError("INVALID_CREDENTIALS", "Wrong username or password");
  if (user.status !== "ACTIVE") throw new AppError("ACCOUNT_REVOKED", "This account has been revoked");
  const refreshToken = newRefreshToken();
  await prisma.refreshToken.create({
    data: { userId: user.id, deviceId, tokenHash: hashToken(refreshToken), expiresAt: refreshExpiry() },
  });
  return session(user, refreshToken);
}

export async function refresh(refreshToken: string, deviceId: string) {
  const invalid = new AppError("INVALID_REFRESH_TOKEN", "Refresh token is not valid");
  const nextToken = newRefreshToken();
  const user = await prisma.$transaction(async (tx) => {
    const stored = await tx.refreshToken.findUnique({
      where: { tokenHash: hashToken(refreshToken) },
      include: { user: true },
    });
    if (
      !stored ||
      stored.revokedAt ||
      stored.expiresAt <= new Date() ||
      stored.deviceId !== deviceId ||
      stored.user.status !== "ACTIVE"
    ) {
      throw invalid;
    }
    const revoked = await tx.refreshToken.updateMany({
      where: { id: stored.id, revokedAt: null },
      data: { revokedAt: new Date() },
    });
    if (revoked.count !== 1) throw invalid;
    await tx.refreshToken.create({
      data: { userId: stored.userId, deviceId, tokenHash: hashToken(nextToken), expiresAt: refreshExpiry() },
    });
    return stored.user;
  });
  return session(user, nextToken);
}

export async function logout(refreshToken: string) {
  await prisma.refreshToken.updateMany({
    where: { tokenHash: hashToken(refreshToken), revokedAt: null },
    data: { revokedAt: new Date() },
  });
}
