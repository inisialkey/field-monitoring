import type { Principal } from "@/modules/auth/tokens";
import { reportInclude, toReportDto } from "@/modules/reports/service";
import { requireRole } from "@/shared/auth";
import { prisma } from "@/shared/db";
import { AppError, isUniqueViolation } from "@/shared/errors";
import { parseId } from "@/shared/http";
import type { Category } from "@/modules/reports/schemas";
import type { EvaluationInput } from "./schemas";

const PAGE_SIZE = 20;

function encodeCursor(finalizedAt: Date, id: string): string {
  return Buffer.from(`${finalizedAt.toISOString()}|${id}`).toString("base64url");
}

function decodeCursor(cursor: string) {
  const [time = "", id = ""] = Buffer.from(cursor, "base64url").toString().split("|");
  const finalizedAt = new Date(time);
  if (Number.isNaN(finalizedAt.getTime())) throw new AppError("BAD_REQUEST", "Invalid cursor");
  return { finalizedAt, id: parseId(id) };
}

function alreadyEvaluated(): AppError {
  return new AppError("ALREADY_EVALUATED", "This report has already been evaluated");
}

export async function listQueue(principal: Principal, cursor: string | null, category?: Category) {
  requireRole(principal, "EVALUATOR");
  const after = cursor ? decodeCursor(cursor) : null;
  const reports = await prisma.report.findMany({
    where: {
      status: "SUBMITTED",
      finalizedAt: { not: null },
      ...(category ? { category } : {}),
      ...(after
        ? { OR: [{ finalizedAt: { gt: after.finalizedAt } }, { finalizedAt: after.finalizedAt, id: { gt: after.id } }] }
        : {}),
    },
    orderBy: [{ finalizedAt: "asc" }, { id: "asc" }],
    take: PAGE_SIZE + 1,
    include: reportInclude,
  });
  const page = reports.slice(0, PAGE_SIZE);
  const last = page.at(-1);
  const nextCursor = reports.length > PAGE_SIZE && last?.finalizedAt ? encodeCursor(last.finalizedAt, last.id) : null;
  return { items: page.map(toReportDto), nextCursor };
}

export async function createEvaluation(principal: Principal, reportId: string, input: EvaluationInput) {
  requireRole(principal, "EVALUATOR");
  try {
    const evaluation = await prisma.$transaction(async (tx) => {
      const report = await tx.report.findUnique({ where: { id: reportId }, select: { finalizedAt: true, status: true } });
      if (!report || report.finalizedAt === null) throw new AppError("NOT_FOUND", "Report not found");
      if (report.status === "EVALUATED") throw alreadyEvaluated();
      const created = await tx.evaluation.create({
        data: { reportId, evaluatorId: principal.userId, result: input.result, notes: input.notes || null },
        include: { evaluator: { select: { displayName: true } } },
      });
      await tx.report.update({ where: { id: reportId }, data: { status: "EVALUATED" } });
      return created;
    });
    return {
      result: evaluation.result,
      notes: evaluation.notes,
      evaluatorName: evaluation.evaluator.displayName,
      createdAt: evaluation.createdAt.toISOString(),
    };
  } catch (error) {
    if (isUniqueViolation(error)) throw alreadyEvaluated();
    throw error;
  }
}
