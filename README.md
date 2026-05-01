# SleepSounds

Production-ready Android app for relaxing sleep sounds with offline-first
playback, multi-track mixing, sleep timer, and AI-powered "dream sound"
generation backed by Firebase Cloud Functions.

* Native Android, **Kotlin** + **Jetpack Compose** + **Material 3**
* `compileSdk 35`, `targetSdk 35`, `minSdk 26`
* Clean architecture: `data` / `domain` / `presentation`
* Hilt DI, Room, Retrofit, Media3 ExoPlayer, Coil
* Google Play Billing 7+, Firebase Auth / Firestore / Storage / Functions
* Cloud Functions in TypeScript that proxy Stability AI / DALL-E
  (image) and Replicate / MusicGen (audio)
* Signed AAB builds out of the box (debug-signed when no keystore is
  configured) with R8 / ProGuard rules

---

## Repository layout

```
sleep-sounds/
├── app/                       Android application module
│   ├── src/main/java/com/sleepsounds/app/
│   │   ├── audio/             ExoPlayer mixer + MediaSession service
│   │   ├── data/              Room, Retrofit, Firebase, Billing impls
│   │   ├── di/                Hilt modules
│   │   ├── domain/            Interfaces, models, use cases
│   │   └── presentation/      Compose screens, theme, navigation
│   ├── src/main/assets/       Bundled MP3 + JPG covers (replace placeholders)
│   ├── src/main/res/          Strings / colors / icons / themes / splash
│   ├── src/test/              JUnit 5 + Turbine unit tests
│   └── src/androidTest/       Compose UI tests scaffolding
├── functions/                 Firebase Cloud Functions (TypeScript)
├── firebase.json              Functions / Firestore / Storage targets
├── firestore.rules            Catalog + user-private rules
├── storage.rules              Public-read generations bucket rules
├── keystore-sample.properties Template for signing
└── gradle/libs.versions.toml  Centralised dependency versions
```

---

## 1. Build & sign

### Debug build

```bash
./gradlew :app:assembleDebug
```

Outputs `app/build/outputs/apk/debug/app-debug.apk`.

### Release AAB

By default the release config falls back to debug signing, so the build
won't fail on contributor machines. To produce a Play-uploadable signed
AAB:

1. Generate a keystore once:

   ```bash
   keytool -genkey -v \
     -keystore ~/.keystores/sleep-sounds-release.jks \
     -alias sleepsounds \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Copy `keystore-sample.properties` to `keystore.properties` and fill in
   the values (it is in `.gitignore`):

   ```properties
   storeFile=/home/USER/.keystores/sleep-sounds-release.jks
   storePassword=...
   keyAlias=sleepsounds
   keyPassword=...
   ```

3. Build and inspect the signed bundle:

   ```bash
   ./gradlew :app:bundleRelease
   ls -lh app/build/outputs/bundle/release/app-release.aab
   ```

Release minification (R8) and resource shrinking are on. ProGuard
rules covering Retrofit, OkHttp, Hilt, Coil, Media3, Room, Firebase,
Play Billing, and Crashlytics live in `app/proguard-rules.pro`.

### Lint and tests

```bash
./gradlew :app:lintDebug                # static analysis
./gradlew :app:testDebugUnitTest        # JUnit 5 + Turbine + MockK
./gradlew :app:connectedDebugAndroidTest  # Compose UI tests (needs a device)
```

---

## 2. Firebase project setup

> All AI keys live in Cloud Functions secrets. The client never sees
> them.

1. Install the Firebase CLI: `npm i -g firebase-tools` and login: `firebase login`.

2. Create a Firebase project (e.g. `sleep-sounds-prod`) at
   https://console.firebase.google.com.

3. Copy the project config:

   * Add an Android app with package name **`com.sleepsounds.app`**.
   * Download `google-services.json` and place it at `app/google-services.json`.
   * The Gradle file conditionally enables the
     `com.google.gms.google-services` and Crashlytics plugins when this
     file is present.

4. Wire the local CLI to your project:

   ```bash
   cp .firebaserc.example .firebaserc
   # edit projects.default to match your Firebase project id
   ```

5. Enable services in the console:

   * Authentication → enable **Anonymous** and **Email/Password** (and
     **Google** if you want SSO).
   * Cloud Firestore → start in production mode.
   * Storage → enable.
   * Functions → upgrade to the **Blaze** plan (required for outbound
     HTTP from Functions).

6. Deploy security rules:

   ```bash
   firebase deploy --only firestore:rules,storage
   ```

---

## 3. Cloud Functions deployment

```bash
cd functions
npm install
cp .env.example .env  # edit with local keys for emulator (optional)
```

Set production secrets (these never appear in source):

```bash
firebase functions:secrets:set STABILITY_API_KEY
firebase functions:secrets:set REPLICATE_API_TOKEN
firebase functions:secrets:set OPENAI_API_KEY                  # optional fallback
firebase functions:secrets:set GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
```

For `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` paste the contents of a service
account JSON that has the `androidpublisher` scope (Google Play Console
→ Setup → API access).

Deploy:

```bash
npm run lint && npm run build
firebase deploy --only functions
```

The Function URLs follow the pattern:

```
https://<region>-<project>.cloudfunctions.net/generateSound
https://<region>-<project>.cloudfunctions.net/checkSubscription
```

Set the client to point at them by editing `app/build.gradle.kts`:

```kotlin
buildConfigField(
    "String",
    "BACKEND_BASE_URL",
    "\"https://us-central1-<project>.cloudfunctions.net/\""
)
```

---

## 4. Google Play subscription products

Create two subscription products in Play Console → Monetize → Products
→ Subscriptions:

| Product ID                  | Base plan      | Recommended price |
| --------------------------- | -------------- | ----------------- |
| `sleep_premium_monthly`     | `monthly`      | $4.99 / month     |
| `sleep_premium_yearly`      | `annual`       | $34.99 / year     |

These IDs are referenced from
`SubscriptionRepositoryImpl.PRODUCT_MONTHLY` /
`SubscriptionRepositoryImpl.PRODUCT_YEARLY`.

Optional bulk-create script (uses the
[Android Publisher API](https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions/create)):

```bash
# functions/scripts/createSubscriptions.ts (sample, run with ts-node)
```

`checkSubscription` Cloud Function automatically verifies grace period
and account hold by mapping `subscriptionState`.

---

## 5. AI generation prompt suggestions

Use these in **Midjourney / DALL-E / Stable Diffusion** for the launcher
icon and recurring covers. They produce a consistent calm pastel look:

* **App icon**

  > "Calm crescent moon with soft stars, pastel gradient sky, minimalist
  > flat illustration with subtle volumetric glow, centered composition,
  > 1024x1024, vector-friendly, no text, suitable as Android adaptive
  > icon."

* **Splash logo (vector)**

  > "Single line monochrome moon glyph, soft gradient #A4C2FF on
  > #0E1330 background, minimal, no text, 512x512."

* **Default cover (rain, fireplace, etc.)** — substitute the subject:

  > "Dreamy concept art of \[SUBJECT], pastel sunset colours, painterly,
  > soft volumetric mist, cinematic, square framing, no people, no
  > text."

* **AI dream sound covers** — appended automatically inside the Cloud
  Function (`generateSound.ts → stylized` constant).

For audio, the Function calls Replicate / MusicGen with this prompt
prefix: `"Ambient relaxation soundscape, gentle, suitable for sleep. "`.

---

## 6. Bundled assets

Six royalty-free MP3s are required at `app/src/main/assets/sounds/`:

```
rain.mp3 fireplace.mp3 ocean.mp3 forest.mp3 singing_bowls.mp3 white_noise.mp3
```

Drop matching JPG covers (1024x1024 recommended) into
`app/src/main/assets/covers/`.  Each file is referenced by
`PresetCatalog.builtIn`. See `app/src/main/assets/sounds/README.md` for
specifics. Tracks should be 60–120 s loops at 192 kbps.

---

## 7. Project structure & key files

* `SleepPlaybackService` — `MediaSessionService` running multiple
  `ExoPlayer` instances simultaneously and applying the fade-out timer.
* `PlaybackRepositoryImpl` — orchestrates state, enforces the 1-track
  free / 3-track premium cap.
* `SubscriptionRepositoryImpl` — Play Billing 7 client; queries product
  details, launches purchase flow, acknowledges, calls
  `checkSubscription` Cloud Function.
* `GenerationRepositoryImpl` — calls `generateSound` Cloud Function and
  emits `GenerationStage` for the progress UI.
* `MainActivity` — `installSplashScreen()` driven by stored `ThemeMode`.
* `theme/Theme.kt` — Material 3 dark/light schemes with dynamic colour
  fallback on API 31+.

---

## 8. Continuous integration tips

The repository ships with a working Gradle wrapper (8.10.2). On a fresh
JDK 17 + Android SDK 34 host you can replicate the local build chain:

```bash
./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug \
          :app:bundleRelease
```

(`bundleRelease` falls back to debug signing so it works without a
keystore in CI; switch to release signing once `keystore.properties` is
provided as a CI secret.)

---

## License

Source code is provided under the MIT license (see `LICENSE`). Replace
this section with your preferred license before publishing.
