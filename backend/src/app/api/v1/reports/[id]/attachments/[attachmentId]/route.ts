import { uploadAttachment } from "@/modules/reports/service";
import { DOCUMENT_MAX_BYTES } from "@/modules/reports/storage";
import { authenticate } from "@/shared/auth";
import { AppError } from "@/shared/errors";
import { handle, json, parseId } from "@/shared/http";

type Context = { params: Promise<{ id: string; attachmentId: string }> };

const MULTIPART_OVERHEAD_BYTES = 1024 * 1024;

export const PUT = handle(async (request: Request, { params }: Context) => {
  const principal = await authenticate(request);
  const { id, attachmentId } = await params;
  const reportId = parseId(id);
  const fileId = parseId(attachmentId);
  if (Number(request.headers.get("content-length") ?? 0) > DOCUMENT_MAX_BYTES + MULTIPART_OVERHEAD_BYTES) {
    throw new AppError("FILE_TOO_LARGE", "The file is too large");
  }
  let form: FormData;
  try {
    form = await request.formData();
  } catch {
    throw new AppError("BAD_REQUEST", "Body must be multipart form data");
  }
  const file = form.get("file");
  if (!(file instanceof File)) throw new AppError("BAD_REQUEST", "The file field is required");
  const { created, attachment } = await uploadAttachment(principal, reportId, fileId, file);
  return json(attachment, created ? 201 : 200);
});
