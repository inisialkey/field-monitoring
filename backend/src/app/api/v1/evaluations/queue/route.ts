import { queueQuerySchema } from "@/modules/evaluations/schemas";
import { listQueue } from "@/modules/evaluations/service";
import { authenticate } from "@/shared/auth";
import { handle, json, parse } from "@/shared/http";

export const GET = handle(async (request: Request) => {
  const principal = await authenticate(request);
  const searchParams = new URL(request.url).searchParams;
  const query = parse(queueQuerySchema, { category: searchParams.get("category") ?? undefined }, "BAD_REQUEST");
  return json(await listQueue(principal, searchParams.get("cursor"), query.category));
});
