import { randomUUID } from "node:crypto";
import { readdirSync, readFileSync } from "node:fs";
import path from "node:path";
import { compileErrors, dereference, validate } from "@readme/openapi-parser";
import { Ajv2020 } from "ajv/dist/2020.js";
import addFormats from "ajv-formats";
import { beforeAll, describe, expect, it } from "vitest";
import { GET as attachmentContent } from "@/app/api/v1/attachments/[id]/content/route";
import { POST as loginRoute } from "@/app/api/v1/auth/login/route";
import { POST as logoutRoute } from "@/app/api/v1/auth/logout/route";
import { POST as refreshRoute } from "@/app/api/v1/auth/refresh/route";
import { GET as queueRoute } from "@/app/api/v1/evaluations/queue/route";
import { GET as myReportsRoute } from "@/app/api/v1/me/reports/route";
import { POST as evaluateRoute } from "@/app/api/v1/reports/[id]/evaluation/route";
import { GET as getReportRoute } from "@/app/api/v1/reports/[id]/route";
import { PASSWORD, createUser, finalize, jsonRequest, params, putPhoto, putReport, reportBody, signIn } from "./helpers";

type MediaType = { schema?: object };
type Operation = { responses: Record<string, { content?: Record<string, MediaType> }> };
type Spec = { paths: Record<string, Record<string, Operation>> };

const specFile = path.resolve(import.meta.dirname, "../openapi.yaml");
const routesDir = path.resolve(import.meta.dirname, "../src/app/api/v1");
const methods = ["get", "put", "post", "patch", "delete"];

let spec: Spec;
const ajv = new Ajv2020({ strict: false, allErrors: true });
addFormats.default(ajv);

beforeAll(async () => {
  spec = (await dereference(specFile)) as unknown as Spec;
});

function documentedResponse(method: string, route: string, status: number) {
  const operation = spec.paths[route]?.[method];
  expect(operation, `${method.toUpperCase()} ${route} is documented`).toBeDefined();
  const response = operation?.responses[String(status)];
  expect(response, `${method.toUpperCase()} ${route} documents status ${status}`).toBeDefined();
  return response;
}

async function expectDocumented(method: string, route: string, response: Response) {
  const documented = documentedResponse(method, route, response.status);
  const schema = documented?.content?.["application/json"]?.schema;
  if (!schema) {
    expect(await response.text()).toBe("");
    return undefined;
  }
  const body = await response.json();
  const check = ajv.compile(schema);
  expect(check(body), `${method.toUpperCase()} ${route} ${response.status}: ${ajv.errorsText(check.errors)}`).toBe(true);
  return body;
}

function implementedOperations(): string[] {
  return readdirSync(routesDir, { recursive: true, encoding: "utf8" })
    .filter((file) => path.basename(file) === "route.ts")
    .flatMap((file) => {
      const route = `/${path.dirname(file).split(path.sep).map((part) => part.replace(/^\[(.+)\]$/, "{$1}")).join("/")}`;
      const source = readFileSync(path.join(routesDir, file), "utf8");
      return [...source.matchAll(/export const (GET|PUT|POST|PATCH|DELETE)\b/g)].map((match) => `${match[1]} ${route}`);
    })
    .sort();
}

function documentedOperations(): string[] {
  return Object.entries(spec.paths)
    .flatMap(([route, item]) => methods.filter((method) => method in item).map((method) => `${method.toUpperCase()} ${route}`))
    .sort();
}

describe("OpenAPI contract", () => {
  it("is a valid OpenAPI document", async () => {
    const result = await validate(specFile);
    expect(result.valid, result.valid ? "" : compileErrors(result)).toBe(true);
  });

  it("documents exactly the implemented endpoints", () => {
    expect(documentedOperations()).toEqual(implementedOperations());
  });

  it("matches the responses of the report and evaluation flow", async () => {
    await createUser("rina", "FIELD_OFFICER");
    await createUser("andi", "EVALUATOR");

    const login = await loginRoute(
      jsonRequest("POST", "/api/v1/auth/login", { username: "rina", password: PASSWORD, deviceId: "device-1" }),
      undefined,
    );
    const rina = await expectDocumented("post", "/auth/login", login);
    const wrongPassword = await loginRoute(
      jsonRequest("POST", "/api/v1/auth/login", { username: "rina", password: "wrong", deviceId: "device-1" }),
      undefined,
    );
    await expectDocumented("post", "/auth/login", wrongPassword);

    const reportId = randomUUID();
    const photoId = randomUUID();
    await expectDocumented("put", "/reports/{id}", await putReport(rina, reportId));
    await expectDocumented("put", "/reports/{id}", await putReport(rina, reportId));
    await expectDocumented("put", "/reports/{id}", await putReport(rina, randomUUID(), reportBody(rina.user.id, { title: "" })));
    await expectDocumented("put", "/reports/{id}/attachments/{attachmentId}", await putPhoto(rina, reportId, photoId));
    await expectDocumented("post", "/reports/{id}/finalize", await finalize(rina, reportId, [photoId, randomUUID()]));
    await expectDocumented("post", "/reports/{id}/finalize", await finalize(rina, reportId, [photoId]));

    const ownReports = await myReportsRoute(jsonRequest("GET", "/api/v1/me/reports", undefined, rina.accessToken), undefined);
    await expectDocumented("get", "/me/reports", ownReports);
    await expectDocumented("get", "/me/reports", await myReportsRoute(jsonRequest("GET", "/api/v1/me/reports"), undefined));

    const andi = await signIn("andi");
    const queue = await queueRoute(jsonRequest("GET", "/api/v1/evaluations/queue", undefined, andi.accessToken), undefined);
    await expectDocumented("get", "/evaluations/queue", queue);
    const forbiddenQueue = await queueRoute(jsonRequest("GET", "/api/v1/evaluations/queue", undefined, rina.accessToken), undefined);
    await expectDocumented("get", "/evaluations/queue", forbiddenQueue);

    const detail = await getReportRoute(
      jsonRequest("GET", `/api/v1/reports/${reportId}`, undefined, andi.accessToken),
      params({ id: reportId }),
    );
    await expectDocumented("get", "/reports/{id}", detail);

    const evaluate = (body: unknown) =>
      evaluateRoute(
        jsonRequest("POST", `/api/v1/reports/${reportId}/evaluation`, body, andi.accessToken),
        params({ id: reportId }),
      );
    await expectDocumented("post", "/reports/{id}/evaluation", await evaluate({ result: "INVALID" }));
    await expectDocumented("post", "/reports/{id}/evaluation", await evaluate({ result: "VALID", notes: null }));
    await expectDocumented("post", "/reports/{id}/evaluation", await evaluate({ result: "VALID", notes: null }));

    const photo = await attachmentContent(
      jsonRequest("GET", `/api/v1/attachments/${photoId}/content`, undefined, rina.accessToken),
      params({ id: photoId }),
    );
    const download = documentedResponse("get", "/attachments/{id}/content", photo.status);
    expect(Object.keys(download?.content ?? {})).toContain(photo.headers.get("content-type"));

    const refreshed = await refreshRoute(
      jsonRequest("POST", "/api/v1/auth/refresh", { refreshToken: rina.refreshToken, deviceId: "device-1" }),
      undefined,
    );
    const session = await expectDocumented("post", "/auth/refresh", refreshed);
    const reused = await refreshRoute(
      jsonRequest("POST", "/api/v1/auth/refresh", { refreshToken: rina.refreshToken, deviceId: "device-1" }),
      undefined,
    );
    await expectDocumented("post", "/auth/refresh", reused);
    const logout = await logoutRoute(
      jsonRequest("POST", "/api/v1/auth/logout", { refreshToken: session.refreshToken }),
      undefined,
    );
    await expectDocumented("post", "/auth/logout", logout);
  });
});
