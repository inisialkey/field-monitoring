import { z } from "zod";

const location = z.object({
  latitude: z.number().min(-90).max(90),
  longitude: z.number().min(-180).max(180),
  accuracyM: z.number().min(0),
  capturedAt: z.iso.datetime({ offset: true }),
});

export const createReportSchema = z
  .object({
    ownerUserId: z.uuid(),
    title: z.string().trim().min(1).max(100),
    category: z.enum(["ROAD", "BRIDGE", "DRAINAGE", "STREET_LIGHT", "TRAFFIC_SIGN", "PUBLIC_FACILITY", "OTHER"]),
    description: z.string().trim().min(1).max(2000),
    incidentAt: z.iso.datetime({ offset: true }),
    incidentOffsetMinutes: z.number().int().min(-720).max(840),
    location: location.nullable(),
    locationNote: z.string().trim().max(200).nullable(),
    createdAt: z.iso.datetime({ offset: true }),
  })
  .refine((report) => report.location !== null || !!report.locationNote, {
    path: ["locationNote"],
    message: "A location note is required when there are no coordinates",
  });

export type CreateReportInput = z.infer<typeof createReportSchema>;

export const finalizeSchema = z.object({ attachmentIds: z.array(z.uuid()).min(1).max(8) });
