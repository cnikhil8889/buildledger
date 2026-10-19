# Google Play release handoff — draft

Updated 18 October 2026. No app has been uploaded or published to Google Play.

## Source package

This repository contains Android/UI source, pinned build tooling, tests, store artwork and listing/privacy drafts. APKs and AABs are generated locally or by GitHub Actions; no signed release binary is checked in. See `docs/RELEASING.md` for output paths and signing.

## Complete before submitting

1. Use the correct verified Play Console developer account and finalize the developer legal name and support email. The provisional app ID must be unique and should be confirmed before first publication.
2. Generate or select an owner-controlled upload key and use Play App Signing. Keep keys/passwords out of source and chat messages. Sign a new release AAB using that key; do not submit the debug-signed APK or the unsigned AAB.
3. Test on actual Android devices, including current Android 16 and an older supported version. Validate device unlock, each provider login, cancellation/deep links, phone/cloud selection, account isolation, cloud conflict/recovery/deletion, first-run profile, project creation, payments, buyer collection, document import/open, CSV/PDF, backup to Drive, restore, rotation and process recreation. Run this checklist against the exact candidate build and record the devices/results.
4. Capture real phone screenshots. This kit does not include fabricated screenshots of untested native behaviour.
5. Finalize and publicly host a privacy policy with the developer's real identity and support contact. The included policy is a draft. Do not expose the existing private builder website to serve it.
6. The owner backend and Google are connected. Complete real-device Google/cloud QA. Configure and test Apple/Microsoft before enabling them in the public app, or ship with only the tested provider. The account-deletion function remains undeployed pending explicit approval and is disabled in the test APK. After approval, deploy/test it and publicly host an account-deletion request page. Provide valid review access. The earlier private website is separate and should remain private.
7. Complete Data safety, app access, target audience, content rating, ads and other applicable declarations in Play Console based on the actual release. The release has identity-provider authentication and optional automatic project/document upload to private Supabase storage. Phone choice keeps project records local, but login information still reaches the auth service. There are no analytics/ad SDKs. Do not copy a blanket “no data” declaration without reviewing the final service.
8. Start with internal testing. Follow the testing and production-access requirements displayed for the actual account; check the official personal-account testing reference below.
9. Submit for Google's review only after signing, device QA, policy and listing preparation are complete. Approval is not guaranteed by a successful build.

## Current official references

- Target SDK: https://support.google.com/googleplay/android-developer/answer/11926878?hl=en — verify the required target level for the planned submission.
- Personal-account testing: https://support.google.com/googleplay/android-developer/answer/14151465?hl=en
- App bundle: https://developer.android.com/guide/app-bundle
- Store graphics and screenshots: https://support.google.com/googleplay/android-developer/answer/9866151?hl=en
- Signing: https://developer.android.com/studio/publish/app-signing

## Signing from a terminal

Set the four `BUILDLEDGER_*` environment variables documented in the root README in a trusted local shell, then run `./gradlew bundleRelease`. Do not place secrets in tracked files. The resulting signed bundle is at `app/build/outputs/bundle/release/app-release.aab`.

## Device test script

Sign in with each provider and select each storage mode on separate test accounts; create your own workspace; add a project and budget of ₹1,00,000; record a ₹12,000 steel bill with ₹7,500 paid; verify ₹4,500 payable. Add a buyer with an ₹80,000 agreement and collect ₹20,000; verify ₹60,000 buyer balance. Import a small PDF into the drawing folder and open it externally. Export a CSV and save a PDF. Save a full backup to a selected provider, then restore it after adding a temporary record; verify data and document integrity. Test cancellation and corrupt-backup errors. Finally test local deletion separately from backup/online records.
