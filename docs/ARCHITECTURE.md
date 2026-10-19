# BuildLedger architecture

The app is a Java Android host with a packaged React/TypeScript UI in a WebView. It is not a Flutter project. UI assets load from the application's local asset origin. Network access, authentication, file imports/exports and encrypted storage are handled by the native bridge.

## Components

| Component | Responsibility |
| --- | --- |
| `MainActivity` | Local WebView, Android insets/font scaling, native bridge and screen-lock gate |
| `AccountManager` | Browser OAuth/PKCE, account/storage selection, snapshot sync and team requests |
| `SecureVault` | Android Keystore AES-GCM protection for metadata and tokens |
| `LedgerStore` | Account-private records/documents, validation and backup/restore |
| `CloudHttp`, `AuthProtocol` | Native HTTPS requests and authentication protocol checks |
| `DocumentProvider` | Read-only document access for selected external viewers |
| `BuildLedger.tsx` | Personal projects, buyers, payments, files and reports |
| `AccountApp.tsx` | Login, storage choice and account controls |
| `TeamApp.tsx` | Shared sites, people, tasks, attendance, wages and stock |

Tokens remain native and are not exposed to JavaScript. Provider passwords are entered in the system browser. The app uses HTTPS and refuses redirects for cloud API requests.

## Data boundaries

Personal Phone storage uses an account-specific local directory after authentication. Personal Cloud storage keeps a local copy and synchronizes complete ZIP snapshots to a private Supabase bucket with a revision check. Concurrent financial edits are not automatically merged.

Shared company records use normalized database tables, company memberships and project assignments. Client table access is SELECT-only with RLS. Writes go through validated command RPCs; private command functions check current membership, role, site access and record versions. Private idempotency receipts protect command retries and financial audit rows retain before/after history.

Workers see assigned projects and their own tasks, attendance, wages and requests. Owners and office staff have the broader access documented in the team guide. Member revocation is evaluated on subsequent server requests.

Company screens require connectivity and keep displayed data in memory. Personal ZIP backup/restore excludes company records. Company CSV is a report of visible records, not a restorable company database backup. Some history lists are capped; wage balances use all approved attendance and recorded payments.

## Platform and limits

Android minSdk 26, compile/target SDK 36, Build Tools 36.0.0, Java 17, Gradle 8.13 and Android Gradle Plugin 8.13.2. React/TypeScript dependencies are pinned in `web/package-lock.json`.

Personal limits: 200 projects, 3,000 buyers, 10,000 payments, 5,000 documents; 20 MB per imported file and 250 MB total document bytes. Backend object-size limits can be lower. Exported personal backup ZIPs are unencrypted. Imported documents remain in app-private storage; no end-to-end encryption is claimed.

The separate earlier website is outside this repository's runtime configuration. No automatic backend deployment, email sending, analytics, location capture or bank transfer is included.
