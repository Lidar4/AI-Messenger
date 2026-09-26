# Google Play Release Checklist

A production release must pass all items below before submission to the Google Play Console:

## 1. Quality & Compliance Checklist
- [ ] **Package Verification**: Verify unique `applicationId` (e.g., `com.aistudio.aimessenger.vkyqta`).
- [ ] **SDK Alignment**: Targets API 36 (Android 16) with a minSdk of 24.
- [ ] **Accessibility (48dp targets)**: Ensure all Compose buttons meet the 48.dp minimum interactive component threshold.
- [ ] **Zero-Permission Media Selection**: Ensure we use modern system document pickers instead of broad storage permissions.

## 2. Secure Signing Path
- [ ] **No Unsigned/Fake Keys**: Production signing MUST use the dedicated upload keystore configured inside GitHub Secrets.
- [ ] **Secrets Verification**: Ensure the following secrets are correctly configured inside the repo:
  - `KEYSTORE_BASE64`
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`

## 3. Play Console Ingestion
- [ ] Build stable Release AAB using: `./gradlew bundleRelease`
- [ ] Ensure `versionCode` and `versionName` are incrementally updated in `app/build.gradle.kts` for each release track.
