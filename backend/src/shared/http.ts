import { z } from "zod";
import { AppError } from "./errors";

type Handler<C> = (request: Request, context: C) => Promise<Response>;

export function handle<C>(handler: Handler<C>): Handler<C> {
  return async (request, context) => {
    try {
      return await handler(request, context);
    } catch (error) {
      if (error instanceof AppError) {
        return Response.json(
          { code: error.code, message: error.message, details: error.details },
          { status: error.status },
        );
      }
      console.error(error);
      return Response.json({ code: "INTERNAL_ERROR", message: "Unexpected error", details: [] }, { status: 500 });
    }
  };
}

export function json(data: unknown, status = 200): Response {
  return Response.json(data, { status });
}

export function noContent(): Response {
  return new Response(null, { status: 204 });
}

export function parse<T extends z.ZodType>(schema: T, value: unknown): z.infer<T> {
  const result = schema.safeParse(value);
  if (!result.success) {
    throw new AppError(
      "VALIDATION_ERROR",
      "Invalid input",
      result.error.issues.map((issue) => ({ field: issue.path.map(String).join("."), issue: issue.message })),
    );
  }
  return result.data;
}

export async function readJson<T extends z.ZodType>(request: Request, schema: T): Promise<z.infer<T>> {
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    throw new AppError("BAD_REQUEST", "Body must be valid JSON");
  }
  return parse(schema, body);
}

export function parseId(value: string): string {
  const result = z.uuid().safeParse(value);
  if (!result.success) throw new AppError("BAD_REQUEST", "Invalid ID");
  return result.data;
}
