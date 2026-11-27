import { z } from "zod";
import { categorySchema } from "@/modules/reports/schemas";

export const evaluationSchema = z
  .object({
    result: z.enum(["VALID", "INVALID"]),
    notes: z.string().trim().max(1000).nullish(),
  })
  .refine((evaluation) => evaluation.result === "VALID" || !!evaluation.notes, {
    path: ["notes"],
    message: "Notes are required for an INVALID result",
  });

export type EvaluationInput = z.infer<typeof evaluationSchema>;

export const queueQuerySchema = z.object({ category: categorySchema.optional() });
