import { listOwnReports } from "@/modules/reports/service";
import { authenticate } from "@/shared/auth";
import { handle, json } from "@/shared/http";

export const GET = handle(async (request: Request) => {
  const principal = await authenticate(request);
  return json(await listOwnReports(principal));
});
