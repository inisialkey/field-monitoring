CREATE SCHEMA IF NOT EXISTS "public";

CREATE TYPE "Role" AS ENUM ('FIELD_OFFICER', 'EVALUATOR');

CREATE TYPE "UserStatus" AS ENUM ('ACTIVE', 'REVOKED');

CREATE TYPE "ReportStatus" AS ENUM ('SUBMITTED', 'EVALUATED');

CREATE TYPE "Category" AS ENUM ('ROAD', 'BRIDGE', 'DRAINAGE', 'STREET_LIGHT', 'TRAFFIC_SIGN', 'PUBLIC_FACILITY', 'OTHER');

CREATE TYPE "AttachmentKind" AS ENUM ('PHOTO', 'DOCUMENT');

CREATE TYPE "EvaluationResult" AS ENUM ('VALID', 'INVALID');

CREATE TABLE "users" (
    "id" UUID NOT NULL,
    "username" TEXT NOT NULL,
    "display_name" TEXT NOT NULL,
    "role" "Role" NOT NULL,
    "status" "UserStatus" NOT NULL DEFAULT 'ACTIVE',
    "password_hash" TEXT NOT NULL,
    "created_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "users_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "refresh_tokens" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "device_id" TEXT NOT NULL,
    "token_hash" TEXT NOT NULL,
    "expires_at" TIMESTAMPTZ(3) NOT NULL,
    "revoked_at" TIMESTAMPTZ(3),
    "created_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "refresh_tokens_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "reports" (
    "id" UUID NOT NULL,
    "owner_id" UUID NOT NULL,
    "title" VARCHAR(100) NOT NULL,
    "category" "Category" NOT NULL,
    "description" VARCHAR(2000) NOT NULL,
    "incident_at" TIMESTAMPTZ(3) NOT NULL,
    "incident_offset_minutes" INTEGER NOT NULL,
    "latitude" DOUBLE PRECISION,
    "longitude" DOUBLE PRECISION,
    "accuracy_m" DOUBLE PRECISION,
    "location_captured_at" TIMESTAMPTZ(3),
    "location_note" VARCHAR(200),
    "status" "ReportStatus" NOT NULL DEFAULT 'SUBMITTED',
    "created_at" TIMESTAMPTZ(3) NOT NULL,
    "received_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "finalized_at" TIMESTAMPTZ(3),

    CONSTRAINT "reports_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "attachments" (
    "id" UUID NOT NULL,
    "report_id" UUID NOT NULL,
    "kind" "AttachmentKind" NOT NULL,
    "mime_type" TEXT NOT NULL,
    "size_bytes" INTEGER NOT NULL,
    "sha256" CHAR(64) NOT NULL,
    "storage_key" TEXT NOT NULL,
    "uploaded_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "attachments_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "evaluations" (
    "id" UUID NOT NULL,
    "report_id" UUID NOT NULL,
    "evaluator_id" UUID NOT NULL,
    "result" "EvaluationResult" NOT NULL,
    "notes" VARCHAR(1000),
    "created_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "evaluations_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "users_username_key" ON "users"("username");

CREATE UNIQUE INDEX "refresh_tokens_token_hash_key" ON "refresh_tokens"("token_hash");

CREATE INDEX "refresh_tokens_user_id_device_id_idx" ON "refresh_tokens"("user_id", "device_id");

CREATE INDEX "reports_owner_id_created_at_idx" ON "reports"("owner_id", "created_at");

CREATE INDEX "reports_status_finalized_at_idx" ON "reports"("status", "finalized_at");

CREATE INDEX "attachments_report_id_idx" ON "attachments"("report_id");

CREATE UNIQUE INDEX "evaluations_report_id_key" ON "evaluations"("report_id");

ALTER TABLE "refresh_tokens" ADD CONSTRAINT "refresh_tokens_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "reports" ADD CONSTRAINT "reports_owner_id_fkey" FOREIGN KEY ("owner_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "attachments" ADD CONSTRAINT "attachments_report_id_fkey" FOREIGN KEY ("report_id") REFERENCES "reports"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "evaluations" ADD CONSTRAINT "evaluations_report_id_fkey" FOREIGN KEY ("report_id") REFERENCES "reports"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "evaluations" ADD CONSTRAINT "evaluations_evaluator_id_fkey" FOREIGN KEY ("evaluator_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "reports" ADD CONSTRAINT "reports_location_complete" CHECK (
  (latitude IS NULL AND longitude IS NULL AND accuracy_m IS NULL AND location_captured_at IS NULL)
  OR (latitude IS NOT NULL AND longitude IS NOT NULL AND accuracy_m IS NOT NULL AND location_captured_at IS NOT NULL)
);

ALTER TABLE "reports" ADD CONSTRAINT "reports_location_note_required" CHECK (
  latitude IS NOT NULL OR location_note IS NOT NULL
);
