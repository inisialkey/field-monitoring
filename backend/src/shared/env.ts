function required(name: string): string {
  const value = process.env[name];
  if (!value) throw new Error(`Missing environment variable ${name}`);
  return value;
}

export const env = {
  get jwtSecret(): Uint8Array {
    return new TextEncoder().encode(required("JWT_SECRET"));
  },
  get accessTokenTtlSeconds(): number {
    return Number(process.env.ACCESS_TOKEN_TTL_SECONDS ?? 300);
  },
  get refreshTokenTtlDays(): number {
    return Number(process.env.REFRESH_TOKEN_TTL_DAYS ?? 30);
  },
  get uploadDir(): string {
    return process.env.UPLOAD_DIR ?? "uploads";
  },
};
