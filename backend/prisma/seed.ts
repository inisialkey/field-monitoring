import "dotenv/config";
import bcrypt from "bcryptjs";
import { prisma } from "../src/shared/db";

const users = [
  { username: "rina", displayName: "Rina Wulandari", role: "FIELD_OFFICER" },
  { username: "dedi", displayName: "Dedi Kurniawan", role: "FIELD_OFFICER" },
  { username: "andi", displayName: "Andi Prasetyo", role: "EVALUATOR" },
  { username: "maya", displayName: "Maya Lestari", role: "EVALUATOR" },
] as const;

const passwordHash = await bcrypt.hash(process.env.SEED_PASSWORD ?? "demo12345", 10);

for (const user of users) {
  await prisma.user.upsert({
    where: { username: user.username },
    update: {},
    create: { ...user, passwordHash },
  });
}

await prisma.$disconnect();
