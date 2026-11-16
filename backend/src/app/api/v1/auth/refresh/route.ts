import { refreshSchema } from "@/modules/auth/schemas";
import { refresh } from "@/modules/auth/service";
import { handle, json, readJson } from "@/shared/http";

export const POST = handle(async (request: Request) => {
  const input = await readJson(request, refreshSchema);
  return json(await refresh(input.refreshToken, input.deviceId));
});
