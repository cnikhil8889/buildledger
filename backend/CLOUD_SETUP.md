# Configure BuildLedger login and cloud storage

This GitHub source package uses an empty configuration by default. Configure the backend before testing sign-in, personal cloud sync or company teams. The supplied source supports Google, Apple and Microsoft sign-in through Supabase Auth.

For the existing BuildLedger backend, reuse its configuration and migrations already applied. This document does not require creating another project or rerunning its SQL.

## 1. New backend only: database and private storage

For a new Supabase project, review and apply these files once, in this order using the SQL Editor:

1. `supabase/migration.sql` — private personal storage bucket, per-account policies, snapshot index and revision-checked commits.
2. `supabase/migrations/20261018174552_company_teams.sql` — companies, roles, invitations, projects, tasks, attendance, wages, stock and validated RPCs.
3. `supabase/migrations/20261018180835_team_indexes_and_receipts.sql` — indexes and private command receipt access restrictions.
4. `supabase/migrations/20261018181340_team_financial_audit.sql` — before/after audit history for attendance, rates and wage payments.

The personal baseline is separate from the numbered migrations; a plain CLI push of the numbered directory alone is insufficient for an empty backend. Review existing schemas before applying any setup file.

Keep `buildledger_private` outside exposed Data API schemas. Keep the `buildledger` bucket private. Exposed company tables use RLS and SELECT-only client grants; writes use validated commands. The included `hosted-team-verification.sql` is a developer test script using rolled-back synthetic fixtures, not a setup migration.

## 2. Auth callbacks

Allow the callbacks for the variants you will use in **Authentication → URL Configuration**:

```text
com.buildledger.app.debug://auth/callback/*
com.buildledger.app://auth/callback/*
```

The app adds a random callback component and validates it with PKCE and an expiry check. Use an owner-controlled HTTPS Site URL for fallback pages. Production verified Android App Links remain future hardening work.

Register this broker callback with each identity provider:

```text
https://YOUR-PROJECT-REF.supabase.co/auth/v1/callback
```

## 3. Identity providers

| Provider | Setup |
| --- | --- |
| Google | Register a web OAuth client, configure consent/audience and the Supabase callback, then put its client ID/secret in the Supabase Google provider settings. |
| Apple | Configure Sign in with Apple, Services ID, domain/callback and the required developer credentials in Supabase. |
| Microsoft/Outlook | Register a Microsoft Entra app for the intended account types, configure its callback and enter its client ID/secret in the Supabase Azure provider settings. |

Use the official provider instructions below for current details. Keep provider secrets in the backend settings. Only enable the providers actually configured.

These are account logins. They do not connect Google Drive, iCloud Drive or OneDrive. BuildLedger Cloud uses Supabase application storage. Android's document picker can import/export through installed file providers.

## 4. Local app configuration

Copy `backend/public-config.example.json` to `backend/public-config.json` and fill:

```json
{
  "url": "https://YOUR-PROJECT-REF.supabase.co",
  "publishableKey": "sb_publishable_YOUR_PUBLIC_KEY",
  "providers": ["google"]
}
```

The file is ignored by Git. Environment variables override it:

- `BUILDLEDGER_SUPABASE_URL`
- `BUILDLEDGER_SUPABASE_KEY` — public publishable key only; never a secret/service-role key.
- `BUILDLEDGER_AUTH_PROVIDERS` — comma-separated `google,apple,azure`, limited to configured providers.

Gradle reads these process variables directly; it does not automatically load a .env file. A missing configuration still compiles but leaves sign-in setup pending.

Use `./gradlew assembleDebug` for a separate debug installation. For updates to the already distributed Cloud Test app, use the original test key as described in `docs/RELEASING.md`.

## 5. Verify on phones

1. Sign in with each enabled provider; test cancellation, expiry and returning after process recreation.
2. Choose Phone, create a project/document, restart and confirm persistence. Use another login and confirm account isolation.
3. Choose Cloud and confirm a private account folder/snapshot is created; test restoring with the same account on another device.
4. Edit on two devices and verify a stale snapshot cannot overwrite the newer revision. Back up before downloading a replacement copy; records do not automatically merge.
5. Create a company and invite office/worker logins. Verify site assignments, own attendance/wages, material requests and revocation with separate accounts.
6. Test interrupted uploads, expired sessions and read/write failures. Keep error states visible and avoid treating a retry as a second payment.

Personal cloud sync runs while the app is open/resumed and uploads full ZIP snapshots. Confirm backend object-size limits match the intended documents. Team screens need internet and Refresh; personal ZIP backup/restore excludes company data.

## Account deletion status

`supabase/functions/delete-account/index.ts` is included as unfinished, undeployed source. `ACCOUNT_DELETION_ENABLED` remains false. Company ownership, shared history and retention handling must be completed before enabling it, with explicit owner approval before deployment. Do not deploy the existing function unchanged for company owners. A public privacy policy, support contact and deletion-request process remain release tasks.

## Official references

- [API keys](https://supabase.com/docs/guides/getting-started/api-keys)
- [Google](https://supabase.com/docs/guides/auth/social-login/auth-google)
- [Apple](https://supabase.com/docs/guides/auth/social-login/auth-apple)
- [Microsoft / Azure](https://supabase.com/docs/guides/auth/social-login/auth-azure)
- [Mobile deep linking](https://supabase.com/docs/guides/auth/native-mobile-deep-linking)
- [PKCE](https://supabase.com/docs/guides/auth/sessions/pkce-flow)
- [Storage access control](https://supabase.com/docs/guides/storage/security/access-control)
