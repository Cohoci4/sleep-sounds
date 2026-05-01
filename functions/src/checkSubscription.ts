import * as admin from "firebase-admin";
import { HttpsError } from "firebase-functions/v2/https";
import type { Request, Response } from "express";
import { GoogleAuth } from "google-auth-library";
import { authenticate } from "./auth";
import {
  PRODUCT_TIERS,
  PlayStatus,
  VerifyResponse,
  buildVerifyResponse,
  mapPlayStatus,
} from "./playStatus";

interface VerifyRequest {
  purchaseToken: string;
  productId: string;
}

export async function checkSubscriptionHandler(
  req: Request,
  res: Response
): Promise<void> {
  // Caller identity comes only from the verified Firebase Auth ID token;
  // never from the body. Otherwise an attacker could write subscription
  // records to arbitrary user documents.
  const userId = await authenticate(req);
  const body = req.body as Partial<VerifyRequest>;
  const token = (body.purchaseToken ?? "").trim();
  const productId = (body.productId ?? "").trim();
  if (!token || !productId) {
    throw new HttpsError("invalid-argument", "purchaseToken and productId required");
  }
  if (!(productId in PRODUCT_TIERS)) {
    throw new HttpsError("invalid-argument", "unknown productId");
  }

  const status = await fetchPlayStatus(productId, token);
  const result: VerifyResponse = buildVerifyResponse(productId, status, Date.now());

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
  return mapPlayStatus(productId, {
    subscriptionState: data?.subscriptionState as string | undefined,
    lineItems: (data?.lineItems as Array<{ productId?: string; expiryTime?: string }> | undefined) ?? [],
  });
}
