/**
 * Pure helpers for converting a Google Play subscriptionsv2 response into the
 * shape we send back to the Android client. Kept free of firebase-admin and
 * google-auth-library imports so they can be unit tested.
 */

export const PRODUCT_TIERS: Record<string, string> = {
  sleep_premium_monthly: "PREMIUM_MONTHLY",
  sleep_premium_yearly: "PREMIUM_YEARLY",
};

export interface PlayStatus {
  /** Subscription has been revoked: EXPIRED, PAUSED, or ON_HOLD. */
  expired: boolean;
  /** Epoch ms when the current paid period ends. */
  expiryTimeMillis: number | null;
  autoRenewing: boolean;
  inGracePeriod: boolean;
  onAccountHold: boolean;
}

export interface VerifyResponse {
  tier: string;
  expiresAtEpochMs: number | null;
  isAutoRenewing: boolean;
  inGracePeriod: boolean;
  onAccountHold: boolean;
}

interface PlayApiPayload {
  subscriptionState?: string;
  lineItems?: Array<{ productId?: string; expiryTime?: string }>;
}

/**
 * Map a raw Play API response into our flat status struct.
 *
 * Per the Play Billing contract:
 * - EXPIRED, PAUSED, ON_HOLD revoke access immediately.
 * - CANCELED keeps access until `expiryTime`.
 * - IN_GRACE_PERIOD keeps access while the user updates payment.
 */
export function mapPlayStatus(
  productId: string,
  payload: PlayApiPayload
): PlayStatus {
  const lineItems = payload.lineItems ?? [];
  const matching = lineItems.find((it) => it.productId === productId) ?? lineItems[0];
  const expiryTimeIso = matching?.expiryTime ?? null;
  const expiryTimeMillis = expiryTimeIso ? Date.parse(expiryTimeIso) : null;
  const subscriptionState = payload.subscriptionState ?? "SUBSCRIPTION_STATE_UNSPECIFIED";

  const expired =
    subscriptionState === "SUBSCRIPTION_STATE_EXPIRED" ||
    subscriptionState === "SUBSCRIPTION_STATE_PAUSED" ||
    subscriptionState === "SUBSCRIPTION_STATE_ON_HOLD";

  return {
    expired,
    expiryTimeMillis,
    autoRenewing: subscriptionState === "SUBSCRIPTION_STATE_ACTIVE",
    inGracePeriod: subscriptionState === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
    onAccountHold: subscriptionState === "SUBSCRIPTION_STATE_ON_HOLD",
  };
}

/**
 * Decide which tier the user gets based on the mapped status, the productId
 * they purchased, and the current wall-clock time. Returns the verification
 * payload we send back to the app.
 */
export function buildVerifyResponse(
  productId: string,
  status: PlayStatus,
  nowMillis: number
): VerifyResponse {
  const accessActive = !status.expired && (status.expiryTimeMillis ?? 0) > nowMillis;
  const tier = accessActive ? PRODUCT_TIERS[productId] ?? "FREE" : "FREE";
  return {
    tier,
    expiresAtEpochMs: status.expiryTimeMillis,
    isAutoRenewing: status.autoRenewing,
    inGracePeriod: status.inGracePeriod,
    onAccountHold: status.onAccountHold,
  };
}
