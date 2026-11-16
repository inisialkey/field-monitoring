import { listQueue } from "@/modules/evaluations/service";
import { authenticate } from "@/shared/auth";
import { handle, json } from "@/shared/http";

export const GET = handle(async (request: Request) => {
  const principal = await authenticate(request);
  const cursor = new URL(request.url).searchParams.get("cursor");
  return json(await listQueue(principal, cursor));
});
