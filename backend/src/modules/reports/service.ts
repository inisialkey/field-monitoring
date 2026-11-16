import type { Prisma } from "../../../generated/prisma/client";
import type { Principal } from "@/modules/auth/tokens";
import { requireRole } from "@/shared/auth";
import { prisma } from "@/shared/db";
import { AppError, isUniqueViolation, validationError } from "@/shared/errors";
import type { CreateReportInput } from "./schemas";
import { DOCUMENT_MAX_BYTES, PHOTO_MAX_BYTES, detectFileType, readStoredFile, sha256, storeFile } from "./storage";

const MAX_PHOTOS = 5;
const MAX_DOCUMENTS = 3;
const FUTURE_TOLERANCE_MS = 10 * 60 * 1000;

export const reportInclude = {
  owner: { select: { displayName: true } },
  attachments: { orderBy: { uploadedAt: "asc" } },
  evaluation: { include: { evaluator: { select: { displayName: true } } } },
} satisfies Prisma.ReportInclude;

type ReportRecord = Prisma.ReportGetPayload<{ include: typeof reportInclude }>;

type AttachmentRecord = { id: string; kind: string; mimeType: string; sizeBytes: number; sha256: string };

function toAttachmentDto(attachment: AttachmentRecord) {
  return {
    id: attachment.id,
    kind: attachment.kind,
    mimeType: attachment.mimeType,
    sizeBytes: attachment.sizeBytes,
    sha256: attachment.sha256,
  };
}

function toLocationDto(report: ReportRecord) {
  if (
    report.latitude === null ||
    report.longitude === null ||
    report.accuracyM === null ||
    report.locationCapturedAt === null
  ) {
    return null;
  }
  return {
    latitude: report.latitude,
    longitude: report.longitude,
    accuracyM: report.accuracyM,
    capturedAt: report.locationCapturedAt.toISOString(),
  };
}

export function toReportDto(report: ReportRecord) {
  return {
    id: report.id,
    ownerUserId: report.ownerId,
    reporterName: report.owner.displayName,
    title: report.title,
    category: report.category,
    description: report.description,
    incidentAt: report.incidentAt.toISOString(),
    incidentOffsetMinutes: report.incidentOffsetMinutes,
    location: toLocationDto(report),
    locationNote: report.locationNote,
    status: report.status,
    createdAt: report.createdAt.toISOString(),
    receivedAt: report.receivedAt.toISOString(),
    finalizedAt: report.finalizedAt?.toISOString() ?? null,
    attachments: report.attachments.map(toAttachmentDto),
    evaluation: report.evaluation
      ? {
          result: report.evaluation.result,
          notes: report.evaluation.notes,
          evaluatorName: report.evaluation.evaluator.displayName,
          createdAt: report.evaluation.createdAt.toISOString(),
        }
      : null,
  };
}

function canView(principal: Principal, report: { ownerId: string; finalizedAt: Date | null }): boolean {
  return principal.role === "EVALUATOR" ? report.finalizedAt !== null : report.ownerId === principal.userId;
}

function reportNotFound(): AppError {
  return new AppError("NOT_FOUND", "Report not found");
}

function ownerMismatch(): AppError {
  return new AppError("OWNER_MISMATCH", "The report owner does not match the signed-in user");
}

function fileTooLarge(): AppError {
  return new AppError("FILE_TOO_LARGE", "The file is too large");
}

function findReport(id: string) {
  return prisma.report.findUnique({ where: { id }, include: reportInclude });
}

function existingReport(principal: Principal, report: ReportRecord) {
  if (report.ownerId !== principal.userId) throw ownerMismatch();
  return { created: false, report: toReportDto(report) };
}

export async function createReport(principal: Principal, id: string, input: CreateReportInput) {
  requireRole(principal, "FIELD_OFFICER");
  if (input.ownerUserId !== principal.userId) throw ownerMismatch();
  const existing = await findReport(id);
  if (existing) return existingReport(principal, existing);
  if (Date.parse(input.incidentAt) > Date.now() + FUTURE_TOLERANCE_MS) {
    throw validationError("incidentAt", "The incident time cannot be in the future");
  }
  try {
    const report = await prisma.report.create({
      data: {
        id,
        ownerId: principal.userId,
        title: input.title,
        category: input.category,
        description: input.description,
        incidentAt: new Date(input.incidentAt),
        incidentOffsetMinutes: input.incidentOffsetMinutes,
        latitude: input.location?.latitude ?? null,
        longitude: input.location?.longitude ?? null,
        accuracyM: input.location?.accuracyM ?? null,
        locationCapturedAt: input.location ? new Date(input.location.capturedAt) : null,
        locationNote: input.locationNote || null,
        createdAt: new Date(input.createdAt),
      },
      include: reportInclude,
    });
    return { created: true, report: toReportDto(report) };
  } catch (error) {
    if (!isUniqueViolation(error)) throw error;
    const raced = await findReport(id);
    if (!raced) throw error;
    return existingReport(principal, raced);
  }
}

export async function uploadAttachment(principal: Principal, reportId: string, attachmentId: string, file: File) {
  requireRole(principal, "FIELD_OFFICER");
  const report = await prisma.report.findUnique({ where: { id: reportId }, include: { attachments: true } });
  if (!report || report.ownerId !== principal.userId) throw reportNotFound();
  const existing = report.attachments.find((attachment) => attachment.id === attachmentId);
  if (existing) return { created: false, attachment: toAttachmentDto(existing) };
  if (report.finalizedAt) throw new AppError("REPORT_FINALIZED", "The report is already finalized");
  if (file.size > DOCUMENT_MAX_BYTES) throw fileTooLarge();
  const bytes = new Uint8Array(await file.arrayBuffer());
  const type = detectFileType(bytes);
  if (!type) throw new AppError("UNSUPPORTED_MEDIA_TYPE", "Only JPEG photos and PDF documents are accepted");
  if (bytes.byteLength > (type.kind === "PHOTO" ? PHOTO_MAX_BYTES : DOCUMENT_MAX_BYTES)) throw fileTooLarge();
  const limit = type.kind === "PHOTO" ? MAX_PHOTOS : MAX_DOCUMENTS;
  if (report.attachments.filter((attachment) => attachment.kind === type.kind).length >= limit) {
    throw validationError("file", `A report can have at most ${limit} ${type.kind === "PHOTO" ? "photos" : "documents"}`);
  }
  const storageKey = await storeFile(reportId, attachmentId, bytes);
  try {
    const attachment = await prisma.attachment.create({
      data: {
        id: attachmentId,
        reportId,
        kind: type.kind,
        mimeType: type.mimeType,
        sizeBytes: bytes.byteLength,
        sha256: sha256(bytes),
        storageKey,
      },
    });
    return { created: true, attachment: toAttachmentDto(attachment) };
  } catch (error) {
    if (!isUniqueViolation(error)) throw error;
    const raced = await prisma.attachment.findUnique({ where: { id: attachmentId } });
    if (!raced || raced.reportId !== reportId) throw new AppError("BAD_REQUEST", "The attachment ID is already in use");
    return { created: false, attachment: toAttachmentDto(raced) };
  }
}

export async function finalizeReport(principal: Principal, reportId: string, attachmentIds: string[]) {
  requireRole(principal, "FIELD_OFFICER");
  return prisma.$transaction(async (tx) => {
    const report = await tx.report.findUnique({ where: { id: reportId }, include: reportInclude });
    if (!report || report.ownerId !== principal.userId) throw reportNotFound();
    if (report.finalizedAt) return toReportDto(report);
    const stored = new Map(report.attachments.map((attachment) => [attachment.id, attachment]));
    const missing = attachmentIds.filter((id) => !stored.has(id));
    if (missing.length > 0) {
      throw new AppError(
        "ATTACHMENTS_INCOMPLETE",
        "Some attachments have not been uploaded",
        missing.map((attachmentId) => ({ attachmentId })),
      );
    }
    if (!attachmentIds.some((id) => stored.get(id)?.kind === "PHOTO")) {
      throw validationError("attachmentIds", "A report needs at least one photo");
    }
    const finalized = await tx.report.update({
      where: { id: reportId },
      data: { finalizedAt: new Date() },
      include: reportInclude,
    });
    return toReportDto(finalized);
  });
}

export async function listOwnReports(principal: Principal) {
  requireRole(principal, "FIELD_OFFICER");
  const reports = await prisma.report.findMany({
    where: { ownerId: principal.userId },
    orderBy: { createdAt: "desc" },
    include: reportInclude,
  });
  return reports.map(toReportDto);
}

export async function getReport(principal: Principal, id: string) {
  const report = await findReport(id);
  if (!report || !canView(principal, report)) throw reportNotFound();
  return toReportDto(report);
}

export async function getAttachmentContent(principal: Principal, id: string) {
  const attachment = await prisma.attachment.findUnique({
    where: { id },
    include: { report: { select: { ownerId: true, finalizedAt: true } } },
  });
  if (!attachment || !canView(principal, attachment.report)) throw new AppError("NOT_FOUND", "Attachment not found");
  return { bytes: await readStoredFile(attachment.storageKey), mimeType: attachment.mimeType };
}
