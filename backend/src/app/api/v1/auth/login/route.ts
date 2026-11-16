import { loginSchema } from "@/modules/auth/schemas";
import { login } from "@/modules/auth/service";
import { handle, json, readJson } from "@/shared/http";

export const POST = handle(async (request: Request) => {
  const input = await readJson(request, loginSchema);
  return json(await login(input.username, input.password, input.deviceId));
});
