import { logoutSchema } from "@/modules/auth/schemas";
import { logout } from "@/modules/auth/service";
import { handle, noContent, readJson } from "@/shared/http";

export const POST = handle(async (request: Request) => {
  const input = await readJson(request, logoutSchema);
  await logout(input.refreshToken);
  return noContent();
});
