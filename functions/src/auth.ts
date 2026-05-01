import * as admin from "firebase-admin";
import { HttpsError } from "firebase-functions/v2/https";
import type { Request } from "express";

/**
 * Extract the bearer token from the `Authorization` header. Pure helper so
 * it can be unit-tested without firebase-admin.
 */
export function extractBearerToken(
  authorizationHeader: string | undefined | null
): string | null {
  if (!authorizationHeader) return null;
  const trimmed = authorizationHeader.trim();
  // RFC 6750 specifies the scheme is case-insensitive.
  const match = /^Bearer\s+(.+)$/i.exec(trimmed);
  if (!match) return null;
  const token = match[1].trim();
  return token.length > 0 ? token : null;
}

/**
 * Verify the Firebase ID token attached to an inbound HTTP request and
 * return the verified user id. Throws an `HttpsError` with code
 * `unauthenticated` for missing or invalid tokens.
 *
 * The handlers must trust the verified `uid` from this function rather than
 * any `userId` field in the request body, otherwise an attacker who knows
 * any user's UID could burn their AI quota or manipulate their subscription.
 */
export async function authenticate(req: Request): Promise<string> {
  const token = extractBearerToken(req.headers.authorization);
  if (!token) {
    throw new HttpsError(
      "unauthenticated",
      "Missing Authorization: Bearer <Firebase ID token> header"
    );
  }
  try {
    const decoded = await admin.auth().verifyIdToken(token);
    return decoded.uid;
  } catch (e) {
    throw new HttpsError("unauthenticated", "Invalid or expired Firebase ID token");
  }
}
