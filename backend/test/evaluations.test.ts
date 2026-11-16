import { describe, expect, it } from "vitest";
import { GET as queue } from "@/app/api/v1/evaluations/queue/route";
import { GET as myReports } from "@/app/api/v1/me/reports/route";
import { POST as evaluate } from "@/app/api/v1/reports/[id]/evaluation/route";
import { createUser, finalizedReport, jsonRequest, params, signIn, type Session } from "./helpers";

function evaluateAs(session: Session, reportId: string, body: unknown) {
  return evaluate(
    jsonRequest("POST", `/api/v1/reports/${reportId}/evaluation`, body, session.accessToken),
    params({ id: reportId }),
  );
}

async function prepare() {
  await createUser("rina", "FIELD_OFFICER");
  await createUser("andi", "EVALUATOR");
  await createUser("maya", "EVALUATOR");
  const rina = await signIn("rina");
  const andi = await signIn("andi");
  const maya = await signIn("maya");
  const reportId = await finalizedReport(rina);
  return { rina, andi, maya, reportId };
}

describe("evaluation", () => {
  it("accepts the first evaluation and rejects a second one", async () => {
    const { andi, maya, reportId } = await prepare();
    expect((await evaluateAs(andi, reportId, { result: "VALID" })).status).toBe(201);
    const second = await evaluateAs(maya, reportId, { result: "INVALID", notes: "Tidak sesuai foto" });
    expect(second.status).toBe(409);
    expect((await second.json()).code).toBe("ALREADY_EVALUATED");
  });

  it("lets only one of two concurrent evaluations succeed", async () => {
    const { andi, maya, reportId } = await prepare();
    const responses = await Promise.all([
      evaluateAs(andi, reportId, { result: "VALID" }),
      evaluateAs(maya, reportId, { result: "INVALID", notes: "Lokasi berbeda" }),
    ]);
    expect(responses.map((response) => response.status).sort()).toEqual([201, 409]);
  });

  it("requires notes for an INVALID result", async () => {
    const { andi, reportId } = await prepare();
    expect((await evaluateAs(andi, reportId, { result: "INVALID" })).status).toBe(422);
  });

  it("does not let a Field Officer evaluate", async () => {
    const { rina, reportId } = await prepare();
    expect((await evaluateAs(rina, reportId, { result: "VALID" })).status).toBe(403);
  });

  it("removes the report from the queue and shows the result to its owner", async () => {
    const { rina, andi, reportId } = await prepare();
    await evaluateAs(andi, reportId, { result: "INVALID", notes: "Foto tidak sesuai lokasi" });
    const pending = await (
      await queue(jsonRequest("GET", "/api/v1/evaluations/queue", undefined, andi.accessToken), undefined)
    ).json();
    expect(pending.items).toEqual([]);
    const own = await (await myReports(jsonRequest("GET", "/api/v1/me/reports", undefined, rina.accessToken), undefined)).json();
    expect(own[0].status).toBe("EVALUATED");
    expect(own[0].evaluation).toMatchObject({ result: "INVALID", notes: "Foto tidak sesuai lokasi" });
  });
});
