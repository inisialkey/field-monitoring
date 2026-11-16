import { getAttachmentContent } from "@/modules/reports/service";
import { authenticate } from "@/shared/auth";
import { handle, parseId } from "@/shared/http";

type Context = { params: Promise<{ id: string }> };

export const GET = handle(async (request: Request, { params }: Context) => {
  const principal = await authenticate(request);
  const { id } = await params;
  const { bytes, mimeType } = await getAttachmentContent(principal, parseId(id));
  return new Response(new Uint8Array(bytes), {
    headers: {
      "content-type": mimeType,
      "content-length": String(bytes.byteLength),
      "cache-control": "private, no-store",
    },
  });
});
