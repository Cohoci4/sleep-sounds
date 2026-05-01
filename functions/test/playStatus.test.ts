import {
  PRODUCT_TIERS,
  buildVerifyResponse,
  mapPlayStatus,
} from "../src/playStatus";

describe("mapPlayStatus", () => {
  const productId = "sleep_premium_monthly";

  it("returns expired=true for SUBSCRIPTION_STATE_EXPIRED", () => {
    const status = mapPlayStatus(productId, {
      subscriptionState: "SUBSCRIPTION_STATE_EXPIRED",
      lineItems: [{ productId, expiryTime: "2024-01-01T00:00:00Z" }],
    });
    expect(status.expired).toBe(true);
    expect(status.autoRenewing).toBe(false);
  });

  it("treats CANCELED as not-expired so the user keeps premium until expiryTime", () => {
    const status = mapPlayStatus(productId, {
      subscriptionState: "SUBSCRIPTION_STATE_CANCELED",
      lineItems: [{ productId, expiryTime: "2099-01-01T00:00:00Z" }],
    });
    expect(status.expired).toBe(false);
  });

  it("treats PAUSED and ON_HOLD as expired (access revoked)", () => {
    expect(
      mapPlayStatus(productId, {
        subscriptionState: "SUBSCRIPTION_STATE_PAUSED",
        lineItems: [],
      }).expired
    ).toBe(true);
    expect(
      mapPlayStatus(productId, {
        subscriptionState: "SUBSCRIPTION_STATE_ON_HOLD",
        lineItems: [],
      }).expired
    ).toBe(true);
  });

  it("flags inGracePeriod and onAccountHold without revoking", () => {
    const grace = mapPlayStatus(productId, {
      subscriptionState: "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
      lineItems: [{ productId, expiryTime: "2099-01-01T00:00:00Z" }],
    });
    expect(grace.inGracePeriod).toBe(true);
    expect(grace.expired).toBe(false);

    const hold = mapPlayStatus(productId, {
      subscriptionState: "SUBSCRIPTION_STATE_ON_HOLD",
      lineItems: [{ productId, expiryTime: "2099-01-01T00:00:00Z" }],
    });
    expect(hold.onAccountHold).toBe(true);
    expect(hold.expired).toBe(true); // ON_HOLD revokes access
  });

  it("picks the matching line item by productId", () => {
    const status = mapPlayStatus(productId, {
      subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
      lineItems: [
        { productId: "sleep_premium_yearly", expiryTime: "2030-01-01T00:00:00Z" },
        { productId, expiryTime: "2026-06-01T00:00:00Z" },
      ],
    });
    expect(status.expiryTimeMillis).toBe(Date.parse("2026-06-01T00:00:00Z"));
  });

  it("falls back to the first line item when productId does not match", () => {
    const status = mapPlayStatus(productId, {
      subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
      lineItems: [{ productId: "other", expiryTime: "2030-01-01T00:00:00Z" }],
    });
    expect(status.expiryTimeMillis).toBe(Date.parse("2030-01-01T00:00:00Z"));
  });

  it("returns null expiry when no line items are present", () => {
    const status = mapPlayStatus(productId, {
      subscriptionState: "SUBSCRIPTION_STATE_UNSPECIFIED",
      lineItems: [],
    });
    expect(status.expiryTimeMillis).toBeNull();
  });
});

describe("buildVerifyResponse", () => {
  const productId = "sleep_premium_monthly";
  const FIXED_NOW = Date.parse("2026-01-15T00:00:00Z");

  it("grants the matching tier when state is ACTIVE and expiry is in the future", () => {
    const result = buildVerifyResponse(
      productId,
      mapPlayStatus(productId, {
        subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
        lineItems: [{ productId, expiryTime: "2026-02-15T00:00:00Z" }],
      }),
      FIXED_NOW
    );
    expect(result.tier).toBe("PREMIUM_MONTHLY");
    expect(result.isAutoRenewing).toBe(true);
  });

  it("grants the matching tier when state is CANCELED but expiry is still future", () => {
    const result = buildVerifyResponse(
      productId,
      mapPlayStatus(productId, {
        subscriptionState: "SUBSCRIPTION_STATE_CANCELED",
        lineItems: [{ productId, expiryTime: "2026-02-15T00:00:00Z" }],
      }),
      FIXED_NOW
    );
    expect(result.tier).toBe("PREMIUM_MONTHLY");
    expect(result.isAutoRenewing).toBe(false);
  });

  it("downgrades to FREE once expiry is past, even if state is still CANCELED", () => {
    const result = buildVerifyResponse(
      productId,
      mapPlayStatus(productId, {
        subscriptionState: "SUBSCRIPTION_STATE_CANCELED",
        lineItems: [{ productId, expiryTime: "2025-12-01T00:00:00Z" }],
      }),
      FIXED_NOW
    );
    expect(result.tier).toBe("FREE");
  });

  it("keeps premium while IN_GRACE_PERIOD even if expiryTime is in the past", () => {
    // During a grace period Play sets `expiryTime` to the start of the grace
    // window (already in the past). We must still grant access.
    const result = buildVerifyResponse(
      productId,
      mapPlayStatus(productId, {
        subscriptionState: "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
        lineItems: [{ productId, expiryTime: "2025-12-01T00:00:00Z" }],
      }),
      FIXED_NOW
    );
    expect(result.tier).toBe("PREMIUM_MONTHLY");
    expect(result.inGracePeriod).toBe(true);
  });

  it("downgrades to FREE for ON_HOLD even with future expiry", () => {
    const result = buildVerifyResponse(
      productId,
      mapPlayStatus(productId, {
        subscriptionState: "SUBSCRIPTION_STATE_ON_HOLD",
        lineItems: [{ productId, expiryTime: "2030-01-01T00:00:00Z" }],
      }),
      FIXED_NOW
    );
    expect(result.tier).toBe("FREE");
    expect(result.onAccountHold).toBe(true);
  });

  it("returns FREE for unknown product ids", () => {
    const result = buildVerifyResponse(
      "unknown_product",
      mapPlayStatus("unknown_product", {
        subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
        lineItems: [{ productId: "unknown_product", expiryTime: "2030-01-01T00:00:00Z" }],
      }),
      FIXED_NOW
    );
    expect(result.tier).toBe("FREE");
  });

  it("PRODUCT_TIERS exposes both monthly and yearly mappings", () => {
    expect(PRODUCT_TIERS.sleep_premium_monthly).toBe("PREMIUM_MONTHLY");
    expect(PRODUCT_TIERS.sleep_premium_yearly).toBe("PREMIUM_YEARLY");
  });
});
