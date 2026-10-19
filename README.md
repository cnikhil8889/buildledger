<p align="center"><img src="web/public/brand-wordmark.png" width="320" alt="BuildLedger"></p>

# BuildLedger

An Android construction management app for builders in India, with private personal records and shared company workspaces.

**Version 0.3.0 · Android 8.0+ · Java 17 + React/TypeScript · Supabase Auth, Database and Storage**

## Features

- Builder profile and projects organized by name and year.
- Document folders for project details, legal approvals, 30-year title history, drawings, buyer agreements, budgets, receipts and reports.
- Project budgets, labour/material bills, buyer collections, outstanding balances, CSV and print/PDF reports.
- Separate owner, office staff and worker logins with project-level access rules.
- Company invitations, tasks, daily progress notes, attendance, daily/hourly wages, recorded payments and material stock/requests.
- Phone or private application-cloud storage for personal records; import/export through Android file providers, including Google Drive when installed.
- Supplied orange/navy branding, cream surfaces, responsive phone layouts and Android font scaling.

| Role | Access |
| --- | --- |
| Owner | Company administration, invitations, roles, projects, assignments, wages, stock and reports |
| Office staff | Company projects, tasks, attendance approval, wage records, stock and reports |
| Worker | Assigned sites, own tasks/attendance/wages, site progress reports/stock and own material requests |

Company permissions are enforced by database policies and validated commands. See [the team guide](docs/TEAM_GUIDE.md).

## Start here

1. Open this repository folder in Android Studio.
2. Use JDK 17 and install Android SDK 36 and Build Tools 36.0.0.
3. Follow [cloud setup](backend/CLOUD_SETUP.md) to configure login and your backend.
4. Build and run the debug app on a test phone.

The repository contains an empty configuration example. A clean build succeeds without credentials, but sign-in and cloud/team features need configuration. Personal Phone storage is chosen after sign-in.

### Configure your backend locally

Copy `backend/public-config.example.json` to `backend/public-config.json`, then enter your Supabase project URL, **publishable key** and enabled providers. The local file is ignored by Git.

```json
{
  "url": "https://YOUR-PROJECT-REF.supabase.co",
  "publishableKey": "sb_publishable_YOUR_PUBLIC_KEY",
  "providers": ["google"]
}
```

Only enable providers configured in Supabase. Supported names are `google`, `apple` and `azure` (Microsoft). Environment variables `BUILDLEDGER_SUPABASE_URL`, `BUILDLEDGER_SUPABASE_KEY` and `BUILDLEDGER_AUTH_PROVIDERS` override the file. Gradle does not load a .env file automatically.

Provider client secrets, backend secret/service-role keys and signing keys stay outside this repository and the APK.

### Build Android

```bash
# macOS/Linux
chmod +x gradlew
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows, use `gradlew.bat testDebugUnitTest lintDebug assembleDebug`.

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. This is a separate `com.buildledger.app.debug` installation. It does not update the previously distributed Cloud Test app.

### Edit and test the UI

Packaged UI assets are checked in, so an Android-only build does not need Node.js. After editing `web/src/`, rebuild the assets before building Android:

```bash
cd web
npm ci
npm run check
npm test
npm run build
```

Use Node.js 24 (`.nvmrc`). For responsive browser checks:

```bash
npx --no-install playwright-core install chromium
# Set CHROME_BIN to the installed Chrome/Chromium executable.
npm run test:layout
npm run test:team
```

The database tests use local PGlite fixtures and need no cloud credentials. Browser checks use a native-bridge test fixture; actual Android/OAuth/device testing remains separate.

## GitHub builds

[Android CI](.github/workflows/android.yml) runs on main-branch pushes, pull requests and manual runs. It checks TypeScript, domain/database rules, responsive screens, native unit tests and Android lint, then builds a debug APK and an **unsigned** release AAB.

Open **Actions → BuildLedger Android CI → successful run → Artifacts** to download the outputs. Default CI builds have empty backend configuration. The workflow does not deploy a backend or publish to Google Play.

See [GitHub setup](docs/GITHUB_SETUP.md) for uploading this project and [release signing](docs/RELEASING.md) for configured builds.

## Project structure

| Path | Contents |
| --- | --- |
| `app/` | Native Java host, secure token storage, document access, packaged UI and JVM tests |
| `web/src/` | Editable React/TypeScript screens and styling |
| `web/tests/` | Domain, cloud isolation, company roles and responsive browser checks |
| `backend/supabase/` | Personal-storage baseline, company migrations and disabled deletion-function source |
| `backend/CLOUD_SETUP.md` | Authentication, storage and database configuration |
| `docs/` | GitHub setup, architecture, company use, releases and validation |
| `play-store/` | Store artwork and drafts requiring owner review |

## Storage and release status

Personal records remain account-private. Company records are shared according to role and need internet; use Refresh to load other devices' changes. Personal ZIP backups do not include company data. Cloud login does not connect Google Drive, iCloud Drive or OneDrive; those providers can be used through Android's file picker when available.

Wage payments record money already paid; they do not transfer money. GPS attendance, facial recognition, shared photos/documents, chat, WhatsApp, equipment, procurement/tax and Tally integrations are future work.

Account deletion remains disabled. Its included function needs company ownership/history handling and explicit deployment approval before it can be enabled. Production signing, actual-device QA, privacy/deletion pages and Play Console requirements remain release work. The source package includes no production key or APK/AAB binaries.

An open-source license has not been selected. Dependency licenses are in [THIRD_PARTY_NOTICES.txt](THIRD_PARTY_NOTICES.txt).
