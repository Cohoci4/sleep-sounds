import * as admin from "firebase-admin";
import { HttpsError } from "firebase-functions/v2/https";
import type { Request, Response } from "express";
import { GoogleAuth } from "google-auth-library";

interface VerifyRequest {
  userId: string;
  purchaseToken: string;
  productId: string;
}

interface VerifyResponse {
  tier: string;
  expiresAtEpochMs: number | null;
  isAutoRenewing: boolean;
  inGracePeriod: boolean;
  onAccountHold: boolean;
}

const PRODUCT_TIERS: Record<string, string> = {
  sleep_premium_monthly: "PREMIUM_MONTHLY",
  sleep_premium_yearly: "PREMIUM_YEARLY",
};

export async function checkSubscriptionHandler(
  req: Request,
  res: Response
): Promise<void> {
  const body = req.body as Partial<VerifyRequest>;
  const userId = (body.userId ?? "").trim();
  const token = (body.purchaseToken ?? "").trim();
  const productId = (body.productId ?? "").trim();
  if (!userId || !token || !productId) {
    throw new HttpsError("invalid-argument", "userId, purchaseToken and productId required");
  }
  if (!(productId in PRODUCT_TIERS)) {
    throw new HttpsError("invalid-argument", "unknown productId");
  }

  const status = await fetchPlayStatus(productId, token);
  const tier = status.expired ? "FREE" : PRODUCT_TIERS[productId];

  const result: VerifyResponse = {
    tier,
    expiresAtEpochMs: status.expiryTimeMillis,
    isAutoRenewing: status.autoRenewing,
    inGracePeriod: status.inGracePeriod,
    onAccountHold: status.onAccountHold,
  };

  await admin.firestore().collection("users").doc(userId).set(
    {
      subscription: {
        ...result,
        productId,
        purchaseToken: token,
        verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
    },
    { merge: true }
  );

  res.status(200).json(result);
}

interface PlayStatus {
  expired: boolean;
  expiryTimeMillis: number | null;
  autoRenewing: boolean;
  inGracePeriod: boolean;
  onAccountHold: boolean;
}

async function fetchPlayStatus(productId: string, purchaseToken: string): Promise<PlayStatus> {
  const credentialsJson = process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON ?? "";
  const packageName = process.env.PLAY_PACKAGE_NAME ?? "com.sleepsounds.app";
  if (!credentialsJson) {
    throw new HttpsError("failed-precondition", "Play service account not configured");
  }
  const credentials = JSON.parse(credentialsJson);
  const auth = new GoogleAuth({
    credentials,
    scopes: ["https://www.googleapis.com/auth/androidpublisher"],
  });
  const client = await auth.getClient();
  const url =
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/` +
    `${encodeURIComponent(packageName)}/purchases/subscriptionsv2/tokens/` +
    `${encodeURIComponent(purchaseToken)}`;
  const res = await client.request<Record<string, unknown>>({ url, method: "GET" });
  const data = res.data;
  const lineItems = (data?.lineItems as Array<Record<string, unknown>> | undefined) ?? [];
  const matching = lineItems.find((it) => it.productId === productId) ?? lineItems[0];
  const expiryTimeIso = (matching?.expiryTime as string | undefined) ?? null;
  const expiryTimeMillis = expiryTimeIso ? Date.parse(expiryTimeIso) : null;
  const subscriptionState = (data?.subscriptionState as string | undefined) ?? "SUBSCRIPTION_STATE_UNSPECIFIED";
  return {
    expired:
      subscriptionState === "SUBSCRIPTION_STATE_EXPIRED" ||
      subscriptionState === "SUBSCRIPTION_STATE_CANCELED",
    expiryTimeMillis,
    autoRenewing: subscriptionState === "SUBSCRIPTION_STATE_ACTIVE",
    inGracePeriod: subscriptionState === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
    onAccountHold: subscriptionState === "SUBSCRIPTION_STATE_ON_HOLD",
  };
}
