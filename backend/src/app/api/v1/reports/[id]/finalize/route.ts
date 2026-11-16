import { finalizeSchema } from "@/modules/reports/schemas";
import { finalizeReport } from "@/modules/reports/service";
import { authenticate } from "@/shared/auth";
import { handle, json, parseId, readJson } from "@/shared/http";

type Context = { params: Promise<{ id: string }> };

export const POST = handle(async (request: Request, { params }: Context) => {
  const principal = await authenticate(request);
  const { id } = await params;
  const reportId = parseId(id);
  const input = await readJson(request, finalizeSchema);
  return json(await finalizeReport(principal, reportId, input.attachmentIds));
});
