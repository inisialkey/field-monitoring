import { z } from "zod";

const deviceId = z.string().min(1).max(64);
const refreshToken = z.string().min(1).max(200);

export const loginSchema = z.object({
  username: z.string().trim().min(1).max(50),
  password: z.string().min(1).max(200),
  deviceId,
});

export const refreshSchema = z.object({ refreshToken, deviceId });

export const logoutSchema = z.object({ refreshToken });
