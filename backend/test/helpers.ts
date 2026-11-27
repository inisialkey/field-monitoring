import { randomUUID } from "node:crypto";
import bcrypt from "bcryptjs";
import { expect } from "vitest";
import { POST as loginRoute } from "@/app/api/v1/auth/login/route";
import { PUT as putAttachmentRoute } from "@/app/api/v1/reports/[id]/attachments/[attachmentId]/route";
import { POST as finalizeRoute } from "@/app/api/v1/reports/[id]/finalize/route";
import { PUT as putReportRoute } from "@/app/api/v1/reports/[id]/route";
import { prisma } from "@/shared/db";

export const PASSWORD = "secret-password";

const passwordHash = bcrypt.hashSync(PASSWORD, 4);

export type Session = { accessToken: string; refreshToken: string; user: { id: string } };

export function createUser(username: string, role: "FIELD_OFFICER" | "EVALUATOR") {
  return prisma.user.create({ data: { username, displayName: `User ${username}`, role, passwordHash } });
}

export function jsonRequest(method: string, path: string, body?: unknown, token?: string): Request {
  const headers = new Headers({ "content-type": "application/json" });
  if (token) headers.set("authorization", `Bearer ${token}`);
  return new Request(`http://localhost${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

export function fileRequest(path: string, bytes: Uint8Array<ArrayBuffer>, token: string): Request {
  const form = new FormData();
  form.append("file", new Blob([bytes]), "upload.bin");
  return new Request(`http://localhost${path}`, {
    method: "PUT",
    headers: { authorization: `Bearer ${token}` },
    body: form,
  });
}

export function params<T extends Record<string, string>>(values: T) {
  return { params: Promise.resolve(values) };
}

export async function signIn(username: string, deviceId = "device-1"): Promise<Session> {
  const response = await loginRoute(
    jsonRequest("POST", "/api/v1/auth/login", { username, password: PASSWORD, deviceId }),
    undefined,
  );
  expect(response.status).toBe(200);
  return (await response.json()) as Session;
}

export function jpeg(size = 2048): Uint8Array<ArrayBuffer> {
  const bytes = new Uint8Array(size);
  bytes.set([0xff, 0xd8, 0xff, 0xe0]);
  return bytes;
}

export function reportBody(ownerUserId: string, overrides: Record<string, unknown> = {}) {
  return {
    ownerUserId,
    title: "Penanaman mangrove blok B",
    category: "MANGROVE",
    description: "Sebanyak 500 bibit tertanam, 20 bibit mati terbawa arus.",
    incidentAt: new Date(Date.now() - 60_000).toISOString(),
    incidentOffsetMinutes: 420,
    location: {
      latitude: -6.26152,
      longitude: 107.15283,
      accuracyM: 8,
      capturedAt: new Date().toISOString(),
      name: "Pantai Bahagia, Muara Gembong, Bekasi, Jawa Barat",
    },
    locationNote: null,
    createdAt: new Date().toISOString(),
    ...overrides,
  };
}

export function putReport(session: Session, reportId: string, body: unknown = reportBody(session.user.id)) {
  return putReportRoute(
    jsonRequest("PUT", `/api/v1/reports/${reportId}`, body, session.accessToken),
    params({ id: reportId }),
  );
}

export function putPhoto(session: Session, reportId: string, attachmentId: string, bytes = jpeg()) {
  return putAttachmentRoute(
    fileRequest(`/api/v1/reports/${reportId}/attachments/${attachmentId}`, bytes, session.accessToken),
    params({ id: reportId, attachmentId }),
  );
}

export function finalize(session: Session, reportId: string, attachmentIds: string[]) {
  return finalizeRoute(
    jsonRequest("POST", `/api/v1/reports/${reportId}/finalize`, { attachmentIds }, session.accessToken),
    params({ id: reportId }),
  );
}

export async function finalizedReport(session: Session, body: unknown = reportBody(session.user.id)): Promise<string> {
  const reportId = randomUUID();
  const photoId = randomUUID();
  expect((await putReport(session, reportId, body)).status).toBe(201);
  expect((await putPhoto(session, reportId, photoId)).status).toBe(201);
  expect((await finalize(session, reportId, [photoId])).status).toBe(200);
  return reportId;
}
