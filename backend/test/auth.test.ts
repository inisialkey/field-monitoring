import { describe, expect, it } from "vitest";
import { POST as login } from "@/app/api/v1/auth/login/route";
import { POST as logout } from "@/app/api/v1/auth/logout/route";
import { POST as refresh } from "@/app/api/v1/auth/refresh/route";
import { GET as myReports } from "@/app/api/v1/me/reports/route";
import { prisma } from "@/shared/db";
import { PASSWORD, createUser, jsonRequest, signIn } from "./helpers";

function refreshWith(refreshToken: string, deviceId = "device-1") {
  return refresh(jsonRequest("POST", "/api/v1/auth/refresh", { refreshToken, deviceId }), undefined);
}

function loginWith(password: string) {
  return login(jsonRequest("POST", "/api/v1/auth/login", { username: "rina", password, deviceId: "device-1" }), undefined);
}

describe("authentication", () => {
  it("signs in with the right password and rejects a wrong one", async () => {
    await createUser("rina", "FIELD_OFFICER");
    const session = await signIn("rina");
    expect(session.accessToken).toBeTruthy();
    const response = await loginWith("wrong-password");
    expect(response.status).toBe(401);
    expect((await response.json()).code).toBe("INVALID_CREDENTIALS");
  });

  it("rotates the refresh token and rejects one that was already used", async () => {
    await createUser("rina", "FIELD_OFFICER");
    const session = await signIn("rina");
    const first = await refreshWith(session.refreshToken);
    expect(first.status).toBe(200);
    const rotated = await first.json();
    expect(rotated.refreshToken).not.toBe(session.refreshToken);
    expect((await refreshWith(session.refreshToken)).status).toBe(401);
    expect((await refreshWith(rotated.refreshToken)).status).toBe(200);
  });

  it("lets only one of two concurrent refreshes with the same token succeed", async () => {
    await createUser("rina", "FIELD_OFFICER");
    const session = await signIn("rina");
    const responses = await Promise.all([refreshWith(session.refreshToken), refreshWith(session.refreshToken)]);
    expect(responses.map((response) => response.status).sort()).toEqual([200, 401]);
  });

  it("rejects a refresh token presented by another device", async () => {
    await createUser("rina", "FIELD_OFFICER");
    const session = await signIn("rina", "device-1");
    expect((await refreshWith(session.refreshToken, "device-2")).status).toBe(401);
  });

  it("blocks a revoked account from refreshing and signing in", async () => {
    const user = await createUser("rina", "FIELD_OFFICER");
    const session = await signIn("rina");
    await prisma.user.update({ where: { id: user.id }, data: { status: "REVOKED" } });
    expect((await refreshWith(session.refreshToken)).status).toBe(401);
    expect((await loginWith(PASSWORD)).status).toBe(403);
  });

  it("revokes the refresh token on logout", async () => {
    await createUser("rina", "FIELD_OFFICER");
    const session = await signIn("rina");
    const response = await logout(
      jsonRequest("POST", "/api/v1/auth/logout", { refreshToken: session.refreshToken }),
      undefined,
    );
    expect(response.status).toBe(204);
    expect((await refreshWith(session.refreshToken)).status).toBe(401);
  });

  it("rejects requests without a valid access token", async () => {
    expect((await myReports(jsonRequest("GET", "/api/v1/me/reports"), undefined)).status).toBe(401);
    expect((await myReports(jsonRequest("GET", "/api/v1/me/reports", undefined, "not-a-token"), undefined)).status).toBe(401);
  });
});
