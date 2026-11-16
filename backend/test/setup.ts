import { afterAll, beforeEach } from "vitest";
import { prisma } from "@/shared/db";

beforeEach(async () => {
  await prisma.$executeRawUnsafe('TRUNCATE "evaluations", "attachments", "reports", "refresh_tokens", "users" CASCADE');
});

afterAll(async () => {
  await prisma.$disconnect();
});
