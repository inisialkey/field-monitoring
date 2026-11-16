import { createHash } from "node:crypto";
import { mkdir, readFile, rename, writeFile } from "node:fs/promises";
import path from "node:path";
import { env } from "@/shared/env";

export const PHOTO_MAX_BYTES = 2 * 1024 * 1024;
export const DOCUMENT_MAX_BYTES = 10 * 1024 * 1024;

type FileType = { kind: "PHOTO" | "DOCUMENT"; mimeType: string };

export function detectFileType(bytes: Uint8Array): FileType | null {
  if (bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) return { kind: "PHOTO", mimeType: "image/jpeg" };
  if (bytes[0] === 0x25 && bytes[1] === 0x50 && bytes[2] === 0x44 && bytes[3] === 0x46) {
    return { kind: "DOCUMENT", mimeType: "application/pdf" };
  }
  return null;
}

export function sha256(bytes: Uint8Array): string {
  return createHash("sha256").update(bytes).digest("hex");
}

export async function storeFile(reportId: string, attachmentId: string, bytes: Uint8Array): Promise<string> {
  const directory = path.join(env.uploadDir, reportId);
  await mkdir(directory, { recursive: true });
  const target = path.join(directory, attachmentId);
  const temporary = `${target}.${process.pid}.${Date.now()}.tmp`;
  await writeFile(temporary, bytes);
  await rename(temporary, target);
  return `${reportId}/${attachmentId}`;
}

export function readStoredFile(storageKey: string): Promise<Buffer> {
  return readFile(path.join(env.uploadDir, storageKey));
}
