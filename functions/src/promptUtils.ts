/**
 * Pure helpers used by `generateSound`. Kept out of the handler module so
 * they can be unit tested without bringing in firebase-admin / axios.
 */

export const MAX_PROMPT_LENGTH = 600;

export type PromptValidation =
  | { ok: true; prompt: string }
  | { ok: false; reason: "empty" | "too_long" };

export function validatePrompt(raw: string | undefined | null): PromptValidation {
  const trimmed = (raw ?? "").trim();
  if (trimmed.length === 0) return { ok: false, reason: "empty" };
  if (trimmed.length > MAX_PROMPT_LENGTH) return { ok: false, reason: "too_long" };
  return { ok: true, prompt: trimmed };
}

const TITLE_PREFIX = "Dream: ";
const MAX_TITLE_BODY = 60;

export function sanitizeTitle(prompt: string): string {
  const trimmed = prompt.replace(/\s+/g, " ").trim();
  if (trimmed.length <= MAX_TITLE_BODY) return `${TITLE_PREFIX}${trimmed}`;
  return `${TITLE_PREFIX}${trimmed.slice(0, MAX_TITLE_BODY - 3)}…`;
}
