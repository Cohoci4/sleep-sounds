/**
 * Cloud Functions that proxy AI image/audio providers and verify Play
 * subscriptions for SleepSounds. All API keys live in environment
 * variables (or `firebase functions:secrets`) so they are never shipped
 * with the client APK.
 */

import * as admin from "firebase-admin";
import { onRequest, HttpsError } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2";
import { generateSoundHandler } from "./generateSound";
import { checkSubscriptionHandler } from "./checkSubscription";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

setGlobalOptions({ region: "us-central1", maxInstances: 20 });

export const generateSound = onRequest(
  {
    secrets: ["STABILITY_API_KEY", "REPLICATE_API_TOKEN", "OPENAI_API_KEY"],
    timeoutSeconds: 540,
    memory: "1GiB",
  },
  async (req, res) => {
    try {
      await generateSoundHandler(req, res);
    } catch (err: unknown) {
      handleError(err, res);
    }
  }
);

export const checkSubscription = onRequest(
  {
    secrets: ["GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"],
    timeoutSeconds: 60,
  },
  async (req, res) => {
    try {
      await checkSubscriptionHandler(req, res);
    } catch (err: unknown) {
      handleError(err, res);
    }
  }
);

function handleError(err: unknown, res: import("express").Response) {
  if (err instanceof HttpsError) {
    res.status(httpStatusForCode(err.code)).json({ error: err.code, message: err.message });
    return;
  }
  const message = err instanceof Error ? err.message : String(err);
  res.status(500).json({ error: "internal", message });
}

function httpStatusForCode(code: string): number {
  switch (code) {
    case "invalid-argument":
      return 400;
    case "permission-denied":
      return 403;
    case "unauthenticated":
      return 401;
    case "resource-exhausted":
      return 429;
    case "not-found":
      return 404;
    default:
      return 500;
  }
}
