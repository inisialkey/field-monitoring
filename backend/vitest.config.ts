import os from "node:os";
import path from "node:path";
import { defineConfig } from "vitest/config";

const testDatabaseUrl = (process.env.TEST_DATABASE_URL ??=
  "postgresql://field:field@localhost:5432/field_monitoring_test");

export default defineConfig({
  resolve: {
    alias: { "@": path.resolve(import.meta.dirname, "src") },
  },
  test: {
    environment: "node",
    globalSetup: ["./test/global-setup.ts"],
    setupFiles: ["./test/setup.ts"],
    fileParallelism: false,
    env: {
      DATABASE_URL: testDatabaseUrl,
      JWT_SECRET: "test-secret-that-is-long-enough-for-hs256",
      ACCESS_TOKEN_TTL_SECONDS: "300",
      REFRESH_TOKEN_TTL_DAYS: "30",
      UPLOAD_DIR: path.join(os.tmpdir(), "field-monitoring-test-uploads"),
    },
  },
});
