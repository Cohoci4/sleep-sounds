import * as admin from "firebase-admin";
import { HttpsError } from "firebase-functions/v2/https";
import type { Request, Response } from "express";
import axios from "axios";
import { randomUUID } from "crypto";
import { authenticate } from "./auth";
import { sanitizeTitle, validatePrompt } from "./promptUtils";

const MONTHLY_QUOTA = 20;

interface GenerateRequest {
  prompt: string;
}

interface GenerateResponse {
  id: string;
  audioUrl: string;
  coverUrl: string;
  title: string;
  durationSeconds: number;
}

export async function generateSoundHandler(
  req: Request,
  res: Response
): Promise<void> {
  // The verified Firebase Auth uid is the only source of truth for the
  // caller's identity; the request body never carries `userId`. This
  // prevents an attacker who knows another user's UID from burning their
  // monthly AI quota or creating generations under their account.
  const userId = await authenticate(req);
  const body = req.body as Partial<GenerateRequest>;
  const validated = validatePrompt(body.prompt);
  if (!validated.ok) {
    throw new HttpsError("invalid-argument", "prompt must be 1..600 chars");
  }
  const prompt = validated.prompt;

  await assertSubscription(userId);
  // Atomically check + reserve a quota slot before doing any expensive AI
  // work. If two requests race only one will succeed.
  await reserveQuotaSlot(userId);

  const generationId = randomUUID();
  let result: GenerateResponse;
  try {
    const cover = await generateCover(prompt, generationId);
    const audio = await generateAudio(prompt, generationId);
    const title = sanitizeTitle(prompt);

    result = {
      id: generationId,
      audioUrl: audio.publicUrl,
      coverUrl: cover.publicUrl,
      title,
      durationSeconds: audio.durationSeconds,
    };

    await admin
      .firestore()
      .collection("users")
      .doc(userId)
      .collection("generations")
      .doc(generationId)
      .set({
        ...result,
        prompt,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
  } catch (err) {
    // Generation failed: refund the slot so the user is not penalised for our
    // failure. Best-effort; rethrow original error.
    await refundQuotaSlot(userId).catch(() => undefined);
    throw err;
  }

  res.status(200).json(result);
}

async function assertSubscription(userId: string): Promise<void> {
  const snap = await admin.firestore().collection("users").doc(userId).get();
  const tier = (snap.data()?.subscription?.tier ?? "FREE") as string;
  if (tier === "FREE") {
    throw new HttpsError(
      "permission-denied",
      "Sleep Premium required for AI generation"
    );
  }
}

/**
 * Atomically reads the current monthly usage counter and increments it if the
 * user is still under the quota. Throws `resource-exhausted` otherwise. Using
 * a Firestore transaction prevents the time-of-check / time-of-use race where
 * two concurrent requests could both pass an independent read-then-write.
 */
async function reserveQuotaSlot(userId: string): Promise<void> {
  const monthKey = new Date().toISOString().slice(0, 7); // YYYY-MM
  const ref = admin
    .firestore()
    .collection("users")
    .doc(userId)
    .collection("quotas")
    .doc(monthKey);

  await admin.firestore().runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const used = (snap.data()?.used as number | undefined) ?? 0;
    if (used >= MONTHLY_QUOTA) {
      throw new HttpsError("resource-exhausted", "quota_exceeded");
    }
    tx.set(
      ref,
      {
        used: used + 1,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
  });
}

/**
 * Decrement the quota counter when a previously-reserved generation could not
 * complete. Floors at zero so we never go negative.
 */
async function refundQuotaSlot(userId: string): Promise<void> {
  const monthKey = new Date().toISOString().slice(0, 7);
  const ref = admin
    .firestore()
    .collection("users")
    .doc(userId)
    .collection("quotas")
    .doc(monthKey);
  await admin.firestore().runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const used = (snap.data()?.used as number | undefined) ?? 0;
    if (used <= 0) return;
    tx.set(
      ref,
      {
        used: used - 1,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
  });
}

interface UploadedAsset {
  publicUrl: string;
}

async function generateCover(
  prompt: string,
  generationId: string
): Promise<UploadedAsset> {
  const apiKey = process.env.STABILITY_API_KEY ?? "";
  const stylized =
    "relaxing cinematic concept art, pastel tones, dreamy ambient lighting, " +
    "soft volumetric fog, painterly, no text, square composition. Subject: " +
    prompt;
  if (apiKey.length === 0) {
    return generateCoverWithDalle(prompt, generationId);
  }
  const response = await axios.post(
    "https://api.stability.ai/v2beta/stable-image/generate/sd3",
    {
      prompt: stylized,
      output_format: "jpeg",
      aspect_ratio: "1:1",
      model: "sd3-medium",
    },
    {
      headers: {
        Authorization: `Bearer ${apiKey}`,
        Accept: "image/*",
      },
      responseType: "arraybuffer",
      timeout: 120_000,
    }
  );
  const buffer: Buffer = Buffer.from(response.data as ArrayBuffer);
  return uploadBuffer(`generations/${generationId}/cover.jpg`, buffer, "image/jpeg");
}

async function generateCoverWithDalle(
  prompt: string,
  generationId: string
): Promise<UploadedAsset> {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    throw new HttpsError("failed-precondition", "no image generator configured");
  }
  const response = await axios.post(
    "https://api.openai.com/v1/images/generations",
    {
      model: "dall-e-3",
      prompt: `Pastel relaxing concept art for a sleep sound app. ${prompt}`,
      size: "1024x1024",
      n: 1,
      response_format: "b64_json",
    },
    {
      headers: { Authorization: `Bearer ${apiKey}` },
      timeout: 120_000,
    }
  );
  const b64 = (response.data?.data?.[0]?.b64_json ?? "") as string;
  const buffer = Buffer.from(b64, "base64");
  return uploadBuffer(`generations/${generationId}/cover.jpg`, buffer, "image/jpeg");
}

interface AudioAsset extends UploadedAsset {
  durationSeconds: number;
}

async function generateAudio(
  prompt: string,
  generationId: string
): Promise<AudioAsset> {
  const replicateToken = process.env.REPLICATE_API_TOKEN;
  if (!replicateToken) {
    throw new HttpsError("failed-precondition", "no audio generator configured");
  }
  // We use facebookresearch/musicgen via Replicate. Returns an MP3 URL.
  const start = await axios.post(
    "https://api.replicate.com/v1/predictions",
    {
      version: "671ac645ce5e552cc63a54a2bbff63fcf798043055d2dac5fc9e36a837eedcfb",
      input: {
        prompt: `Ambient relaxation soundscape, gentle, suitable for sleep. ${prompt}`,
        duration: 60,
        output_format: "mp3",
      },
    },
    {
      headers: {
        Authorization: `Token ${replicateToken}`,
        "Content-Type": "application/json",
      },
      timeout: 120_000,
    }
  );

  const predictionId = (start.data?.id ?? "") as string;
  if (!predictionId) {
    throw new HttpsError("internal", "audio generator did not return prediction id");
  }
  const audioUrl = await waitForReplicate(predictionId, replicateToken);

  // Download the audio and upload to Firebase Storage so that downloads
  // happen against our CDN with predictable lifetime.
  const audioResponse = await axios.get<ArrayBuffer>(audioUrl, {
    responseType: "arraybuffer",
    timeout: 120_000,
  });
  const buffer = Buffer.from(audioResponse.data);
  const uploaded = await uploadBuffer(
    `generations/${generationId}/audio.mp3`,
    buffer,
    "audio/mpeg"
  );
  return { ...uploaded, durationSeconds: 60 };
}

async function waitForReplicate(predictionId: string, token: string): Promise<string> {
  const url = `https://api.replicate.com/v1/predictions/${predictionId}`;
  const start = Date.now();
  while (Date.now() - start < 480_000) {
    const res = await axios.get(url, {
      headers: { Authorization: `Token ${token}` },
      timeout: 60_000,
    });
    const status = res.data?.status as string;
    if (status === "succeeded") {
      const output = res.data?.output;
      if (typeof output === "string") return output;
      if (Array.isArray(output) && output.length > 0) return output[0] as string;
      throw new HttpsError("internal", "replicate output missing");
    }
    if (status === "failed" || status === "canceled") {
      throw new HttpsError("internal", `replicate failed: ${res.data?.error ?? status}`);
    }
    await new Promise((r) => setTimeout(r, 2500));
  }
  throw new HttpsError("deadline-exceeded", "audio generation timed out");
}

async function uploadBuffer(
  path: string,
  buffer: Buffer,
  contentType: string
): Promise<UploadedAsset> {
  const bucket = admin.storage().bucket();
  const file = bucket.file(path);
  await file.save(buffer, {
    contentType,
    public: true,
    metadata: { cacheControl: "public, max-age=31536000" },
  });
  return { publicUrl: `https://storage.googleapis.com/${bucket.name}/${path}` };
}


