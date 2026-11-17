import { randomUUID } from "node:crypto";
import { describe, expect, it } from "vitest";
import { GET as attachmentContent } from "@/app/api/v1/attachments/[id]/content/route";
import { GET as queue } from "@/app/api/v1/evaluations/queue/route";
import { GET as getReport } from "@/app/api/v1/reports/[id]/route";
import { prisma } from "@/shared/db";
import {
  createUser,
  finalize,
  finalizedReport,
  jpeg,
  jsonRequest,
  params,
  putPhoto,
  putReport,
  reportBody,
  signIn,
  type Session,
} from "./helpers";

async function queuedIds(session: Session): Promise<string[]> {
  const response = await queue(jsonRequest("GET", "/api/v1/evaluations/queue", undefined, session.accessToken), undefined);
  const body = await response.json();
  return body.items.map((item: { id: string }) => item.id);
}

function readReport(session: Session, reportId: string) {
  return getReport(jsonRequest("GET", `/api/v1/reports/${reportId}`, undefined, session.accessToken), params({ id: reportId }));
}

function readAttachment(session: Session, attachmentId: string) {
  return attachmentContent(
    jsonRequest("GET", `/api/v1/attachments/${attachmentId}/content`, undefined, session.accessToken),
    params({ id: attachmentId }),
  );
}

describe("report submission", () => {
  it("stores one report when the same ID is sent twice", async () => {
    await createUser("rina", "FIELD_OFFICER");
    const session = await signIn("rina");
    const reportId = randomUUID();
    expect((await putReport(session, reportId)).status).toBe(201);
    expect((await putReport(session, reportId)).status).toBe(200);
    expect(await prisma.report.count()).toBe(1);
  });

  it("rejects a report whose owner differs from the signed-in user", async () => {
    await createUser("rina", "FIELD_OFFICER");
    const dedi = await createUser("dedi", "FIELD_OFFICER");
    const session = await signIn("rina");
    const response = await putReport(session, randomUUID(), reportBody(dedi.id));
    expect(response.status).toBe(403);
    expect((await response.json()).code).toBe("OWNER_MISMATCH");
  });

  it("rejects a report ID that already belongs to another user", async () => {
    await createUser("rina", "FIELD_OFFICER");
    await createUser("dedi", "FIELD_OFFICER");
    const rina = await signIn("rina");
    const dedi = await signIn("dedi");
    const reportId = randomUUID();
    await putReport(rina, reportId);
    expect((await putReport(dedi, reportId)).status).toBe(403);
  });

  it("requires a location note when there are no coordinates", async () => {
    await createUser("rina", "FIELD_OFFICER");
    const session = await signIn("rina");
    const rejected = await putReport(
      session,
      randomUUID(),
      reportBody(session.user.id, { location: null, locationNote: null }),
    );
    expect(rejected.status).toBe(422);
    expect((await rejected.json()).details[0].field).toBe("locationNote");
    const accepted = await putReport(
      session,
      randomUUID(),
      reportBody(session.user.id, { location: null, locationNote: "Petak 3, Desa Pantai Bahagia" }),
    );
    expect(accepted.status).toBe(201);
  });

  it("rejects an incident time in the future", async () => {
    await createUser("rina", "FIELD_OFFICER");
    const session = await signIn("rina");
    const incidentAt = new Date(Date.now() + 60 * 60 * 1000).toISOString();
    expect((await putReport(session, randomUUID(), reportBody(session.user.id, { incidentAt }))).status).toBe(422);
  });

  it("does not let an Evaluator create reports", async () => {
    await createUser("andi", "EVALUATOR");
    const session = await signIn("andi");
    expect((await putReport(session, randomUUID())).status).toBe(403);
  });
});

describe("attachments and finalize", () => {
  it("accepts a JPEG once and rejects other file types and oversized photos", async () => {
    await createUser("rina", "FIELD_OFFICER");
    const session = await signIn("rina");
    const reportId = randomUUID();
    await putReport(session, reportId);
    const photoId = randomUUID();
    expect((await putPhoto(session, reportId, photoId)).status).toBe(201);
    expect((await putPhoto(session, reportId, photoId)).status).toBe(200);
    expect((await putPhoto(session, reportId, randomUUID(), new Uint8Array(64))).status).toBe(415);
    expect((await putPhoto(session, reportId, randomUUID(), jpeg(2 * 1024 * 1024 + 1))).status).toBe(413);
  });

  it("returns 409 with the missing attachments and finalizes once they arrive", async () => {
    await createUser("rina", "FIELD_OFFICER");
    const session = await signIn("rina");
    const reportId = randomUUID();
    const photoId = randomUUID();
    await putReport(session, reportId);
    const incomplete = await finalize(session, reportId, [photoId]);
    expect(incomplete.status).toBe(409);
    expect((await incomplete.json()).details).toEqual([{ attachmentId: photoId }]);
    await putPhoto(session, reportId, photoId);
    expect((await finalize(session, reportId, [photoId])).status).toBe(200);
    expect((await finalize(session, reportId, [photoId])).status).toBe(200);
  });

  it("keeps unfinalized reports away from Evaluators", async () => {
    await createUser("rina", "FIELD_OFFICER");
    await createUser("andi", "EVALUATOR");
    const rina = await signIn("rina");
    const andi = await signIn("andi");
    const reportId = randomUUID();
    const photoId = randomUUID();
    await putReport(rina, reportId);
    await putPhoto(rina, reportId, photoId);
    expect(await queuedIds(andi)).toEqual([]);
    expect((await readReport(andi, reportId)).status).toBe(404);
    await finalize(rina, reportId, [photoId]);
    expect(await queuedIds(andi)).toEqual([reportId]);
    expect((await readReport(andi, reportId)).status).toBe(200);
  });

  it("hides another Field Officer's report and photos", async () => {
    await createUser("rina", "FIELD_OFFICER");
    await createUser("dedi", "FIELD_OFFICER");
    const rina = await signIn("rina");
    const dedi = await signIn("dedi");
    const reportId = await finalizedReport(rina);
    const attachment = await prisma.attachment.findFirstOrThrow({ where: { reportId } });
    expect((await readReport(dedi, reportId)).status).toBe(404);
    expect((await readAttachment(dedi, attachment.id)).status).toBe(404);
    const own = await readAttachment(rina, attachment.id);
    expect(own.status).toBe(200);
    expect(own.headers.get("content-type")).toBe("image/jpeg");
  });
});
