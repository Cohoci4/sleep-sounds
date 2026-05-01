import { extractBearerToken } from "../src/auth";

describe("extractBearerToken", () => {
  it("returns null for missing or blank headers", () => {
    expect(extractBearerToken(undefined)).toBeNull();
    expect(extractBearerToken(null)).toBeNull();
    expect(extractBearerToken("")).toBeNull();
    expect(extractBearerToken("   ")).toBeNull();
  });

  it("returns null for non-Bearer schemes", () => {
    expect(extractBearerToken("Basic abcdef")).toBeNull();
    expect(extractBearerToken("Token abcdef")).toBeNull();
  });

  it("returns null when the scheme is present but the token is empty", () => {
    expect(extractBearerToken("Bearer ")).toBeNull();
    expect(extractBearerToken("Bearer    ")).toBeNull();
  });

  it("extracts the token and treats the scheme case-insensitively (RFC 6750)", () => {
    expect(extractBearerToken("Bearer abc.def.ghi")).toBe("abc.def.ghi");
    expect(extractBearerToken("bearer abc.def.ghi")).toBe("abc.def.ghi");
    expect(extractBearerToken("BEARER abc.def.ghi")).toBe("abc.def.ghi");
  });

  it("trims surrounding whitespace from header and token", () => {
    expect(extractBearerToken("  Bearer  abc.def.ghi  ")).toBe("abc.def.ghi");
  });
});
