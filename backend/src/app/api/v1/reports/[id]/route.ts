import { createReportSchema } from "@/modules/reports/schemas";
import { createReport, getReport } from "@/modules/reports/service";
import { authenticate } from "@/shared/auth";
import { handle, json, parseId, readJson } from "@/shared/http";

type Context = { params: Promise<{ id: string }> };

export const GET = handle(async (request: Request, { params }: Context) => {
  const principal = await authenticate(request);
  const { id } = await params;
  return json(await getReport(principal, parseId(id)));
});

export const PUT = handle(async (request: Request, { params }: Context) => {
  const principal = await authenticate(request);
  const { id } = await params;
  const reportId = parseId(id);
  const input = await readJson(request, createReportSchema);
  const { created, report } = await createReport(principal, reportId, input);
  return json(report, created ? 201 : 200);
});
