import {
  MAX_PROMPT_LENGTH,
  sanitizeTitle,
  validatePrompt,
} from "../src/promptUtils";

describe("validatePrompt", () => {
  it("rejects undefined and empty input", () => {
    expect(validatePrompt(undefined)).toEqual({ ok: false, reason: "empty" });
    expect(validatePrompt("")).toEqual({ ok: false, reason: "empty" });
    expect(validatePrompt("   \n  ")).toEqual({ ok: false, reason: "empty" });
  });

  it("rejects prompts longer than the documented limit", () => {
    const tooLong = "a".repeat(MAX_PROMPT_LENGTH + 1);
    expect(validatePrompt(tooLong)).toEqual({ ok: false, reason: "too_long" });
  });

  it("accepts prompts at exactly the documented limit", () => {
    const max = "b".repeat(MAX_PROMPT_LENGTH);
    expect(validatePrompt(max)).toEqual({ ok: true, prompt: max });
  });

  it("trims surrounding whitespace", () => {
    expect(validatePrompt("  hello world  \n")).toEqual({
      ok: true,
      prompt: "hello world",
    });
  });
});

describe("sanitizeTitle", () => {
  it("prefixes short prompts with 'Dream: '", () => {
    expect(sanitizeTitle("rain in the forest")).toBe("Dream: rain in the forest");
  });

  it("collapses runs of whitespace into single spaces", () => {
    expect(sanitizeTitle("rain   in\n the\tforest")).toBe(
      "Dream: rain in the forest"
    );
  });

  it("truncates long prompts and keeps the title under 70 chars", () => {
    const long = "a very long ambient atmosphere ".repeat(10);
    const title = sanitizeTitle(long);
    expect(title.startsWith("Dream: ")).toBe(true);
    expect(title.length).toBeLessThanOrEqual("Dream: ".length + 60);
    expect(title.endsWith("…")).toBe(true);
  });
});
