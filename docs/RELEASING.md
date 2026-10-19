# Build and signing guide

## Local debug

Configure login using `backend/CLOUD_SETUP.md`, then run `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`). Output: `app/build/outputs/apk/debug/app-debug.apk`.

Package `com.buildledger.app.debug`, version `0.3.0-test`, callback `com.buildledger.app.debug://auth/callback/*`. It is separate from the existing Cloud Test app.

## Update the existing Cloud Test app

Use the separately held **BuildLedger-Cloud-Test-Signing.zip**. Extract it outside this repository. Configure `backend/public-config.json` with the existing backend settings and enabled providers, then set:

```bash
export BUILDLEDGER_CLOUD_TEST_KEYSTORE=/absolute/private/path/to/test-key.keystore
./gradlew testCloudTestUnitTest lintCloudTest assembleCloudTest
```

Cloud Test expects alias `buildledger-cloud-test` and test passwords `android`. These are test settings, not a production signing arrangement. The key itself is not included in this repository.

Output: `app/build/outputs/apk/cloudTest/app-cloudTest.apk`. Package `com.buildledger.app.cloudtest`, version `0.3.0-cloud-test`, versionCode 5. Its callback is `com.buildledger.app://auth/callback/*`. Avoid multiple installed apps claiming that scheme on one phone.

The same private key is needed to update the previously distributed Cloud Test APK. Without the environment variable, this variant inherits debug signing and cannot replace that installed APK. Check the signer before distributing. Do not uninstall an existing app just to bypass a signing mismatch; preserve/export its records first.

## Production bundle

Release package: `com.buildledger.app`. Confirm that identifier before first Play publication. Increase versionCode for each later Play update.

Set the following in a trusted local environment or use Android Studio's **Generate Signed Bundle / APK**:

| Variable | Value |
| --- | --- |
| `BUILDLEDGER_KEYSTORE` | Absolute path to the owner's upload keystore |
| `BUILDLEDGER_STORE_PASSWORD` | Keystore password |
| `BUILDLEDGER_KEY_ALIAS` | Upload-key alias |
| `BUILDLEDGER_KEY_PASSWORD` | Upload-key password |

Then run `./gradlew testReleaseUnitTest lintRelease bundleRelease`. The bundle is `app/build/outputs/bundle/release/app-release.aab`. Without release signing settings, it is **unsigned** and cannot be submitted as-is. Release builds never fall back to a debug key.

The GitHub workflow creates a debug APK and an unsigned AAB with empty backend settings. Production signing credentials are not requested or used by that workflow.

Review `play-store/RELEASE_GUIDE.md` and current Play Console requirements before release. Account deletion remains disabled and its function must not be enabled unchanged for company owners. Publishing is a separate owner action.
