import { describe, expect, it } from "vitest";
import { GET as queue } from "@/app/api/v1/evaluations/queue/route";
import { GET as myReports } from "@/app/api/v1/me/reports/route";
import { POST as evaluate } from "@/app/api/v1/reports/[id]/evaluation/route";
import { createUser, finalizedReport, jsonRequest, params, reportBody, signIn, type Session } from "./helpers";

function evaluateAs(session: Session, reportId: string, body: unknown) {
  return evaluate(
    jsonRequest("POST", `/api/v1/reports/${reportId}/evaluation`, body, session.accessToken),
    params({ id: reportId }),
  );
}

function queuePage(session: Session, query = "") {
  return queue(jsonRequest("GET", `/api/v1/evaluations/queue${query}`, undefined, session.accessToken), undefined);
}

function reportIn(session: Session, category: string) {
  return finalizedReport(session, reportBody(session.user.id, { category }));
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

describe("evaluation queue", () => {
  it("returns only reports in the requested category", async () => {
    await createUser("rina", "FIELD_OFFICER");
    await createUser("andi", "EVALUATOR");
    const rina = await signIn("rina");
    const andi = await signIn("andi");
    const mangrove = await reportIn(rina, "MANGROVE");
    await reportIn(rina, "BIODIVERSITY");

    const response = await queuePage(andi, "?category=MANGROVE");
    expect(response.status).toBe(200);
    const page = await response.json();
    expect(page.items.map((item: { id: string }) => item.id)).toEqual([mangrove]);
    expect(page.items[0].category).toBe("MANGROVE");
    expect(page.nextCursor).toBeNull();
    expect((await (await queuePage(andi)).json()).items).toHaveLength(2);
  });

  it("pages through a filtered queue without gaps or duplicates", async () => {
    await createUser("rina", "FIELD_OFFICER");
    await createUser("andi", "EVALUATOR");
    const rina = await signIn("rina");
    const andi = await signIn("andi");
    const mangrove: string[] = [];
    for (let index = 0; index < 24; index++) {
      if (index % 8 === 0) await reportIn(rina, "OTHER");
      else mangrove.push(await reportIn(rina, "MANGROVE"));
    }
    expect(mangrove).toHaveLength(21);

    const first = await (await queuePage(andi, "?category=MANGROVE")).json();
    expect(first.items).toHaveLength(20);
    expect(first.nextCursor).toEqual(expect.any(String));
    const second = await (
      await queuePage(andi, `?category=MANGROVE&cursor=${encodeURIComponent(first.nextCursor)}`)
    ).json();
    expect(second.nextCursor).toBeNull();

    const items = [...first.items, ...second.items];
    const ids = items.map((item: { id: string }) => item.id);
    expect(items.every((item: { category: string }) => item.category === "MANGROVE")).toBe(true);
    expect(new Set(ids).size).toBe(ids.length);
    expect([...ids].sort()).toEqual([...mangrove].sort());
  });

  it.each(["INVALID", "mangrove", ""])("rejects category=%j with 400", async (category) => {
    await createUser("andi", "EVALUATOR");
    const andi = await signIn("andi");
    const response = await queuePage(andi, `?category=${encodeURIComponent(category)}`);
    expect(response.status).toBe(400);
    const body = await response.json();
    expect(body.code).toBe("BAD_REQUEST");
    expect(body.details[0].field).toBe("category");
  });
});
