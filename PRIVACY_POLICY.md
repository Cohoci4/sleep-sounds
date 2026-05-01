# SleepSounds — Privacy Policy

_Last updated: replace with the date you publish this policy._

This template is provided so you can publish a public privacy policy URL
(required by Google Play for any subscription app that uses the
internet). Replace every **TODO** placeholder before linking it from
your store listing.

---

## 1. Who we are

SleepSounds (the "App") is operated by **TODO: legal entity name and
contact address**. Questions about this policy can be sent to
**TODO: privacy@yourdomain.example**.

## 2. Data we collect

We collect only the minimum data needed to run the App. We do **not**
sell personal data and we do **not** show advertising.

| Category | Data | Why we collect it | Where it lives |
|---|---|---|---|
| Account identifier | Anonymous Firebase UID, optionally Google or email sign-in | Sync favourites, AI history and subscription status across your devices | Firebase Authentication, Cloud Firestore |
| Subscription state | Google Play purchase token, product id, expiry, grace/account-hold flags | Unlock Sleep Premium features and respect Google Play policies | Cloud Firestore, sent to Google Play Android Publisher API for verification |
| AI prompts | Text you type into "Create dream sound" | Generate the requested cover and audio | Cloud Functions, third-party AI providers (see §4) |
| AI outputs | Generated cover JPGs and audio MP3s | Display them in your AI history and let you replay them offline | Firebase Storage |
| Diagnostic data | Crash reports, Timber logs scrubbed of personal data | Find and fix bugs | Firebase Crashlytics |

We do **not** collect microphone audio, contacts, location, or
device identifiers beyond the anonymous Firebase UID.

## 3. Microphone (optional voice prompt)

If you tap the optional "Use my voice as a prompt?" toggle on the
Generate screen, the App records a short audio clip and uploads it to
**TODO: chosen Speech-to-Text provider, e.g. OpenAI Whisper** for
transcription. The clip is discarded after transcription and never
shared with other users. This feature is opt-in and disabled by
default.

## 4. Third parties

We pass the following data to third-party processors. Each is bound by
their respective privacy policy.

* **Google Firebase** (Authentication, Firestore, Storage, Cloud
  Functions, Crashlytics): https://firebase.google.com/support/privacy
* **Google Play Billing / Android Publisher API**: subscription
  verification.
* **TODO: AI image provider** (Stability AI, OpenAI DALL-E 3,
  Replicate, …): receives your text prompt to generate the cover.
* **TODO: AI audio provider** (Replicate MusicGen, Mubert,
  ElevenLabs, …): receives your text prompt to generate the audio.

## 5. Data retention and deletion

* Anonymous Firebase UIDs and their associated favourites, history and
  subscription metadata are kept until you delete your account.
* AI cover/audio files are kept until you delete the corresponding
  generation from "Profile → AI history" or until your account is
  deleted.
* Crash reports are kept for 90 days by Firebase Crashlytics defaults.

To delete your account and all associated data, send an email to
**TODO: privacy@yourdomain.example** with the subject "Delete my
SleepSounds account" or use the in-app "Delete account" action in
Profile → Settings (TODO: implement this control before publishing).

## 6. Children

The App is not directed at children under 13 and we do not knowingly
collect data from them.

## 7. International transfers

Firebase services and AI providers process data in the United States
and other regions. By using the App you consent to your data being
processed in those regions.

## 8. Changes

We will post material changes here and update the "Last updated" date.
Continued use of the App after a change constitutes acceptance.

## 9. Contact

**TODO: privacy@yourdomain.example**
**TODO: postal address required by GDPR / CCPA**
