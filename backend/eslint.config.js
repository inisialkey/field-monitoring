import js from "@eslint/js";
import { defineConfig } from "eslint/config";
import tseslint from "typescript-eslint";

export default defineConfig(
  { ignores: ["generated/", ".next/", "next-env.d.ts"] },
  js.configs.recommended,
  tseslint.configs.recommended,
  {
    languageOptions: {
      parserOptions: {
        projectService: { allowDefaultProject: ["eslint.config.js"] },
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      "@typescript-eslint/no-floating-promises": "error",
      "@typescript-eslint/no-misused-promises": "error",
    },
  },
);
