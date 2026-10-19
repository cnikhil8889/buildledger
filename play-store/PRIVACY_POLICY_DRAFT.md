# BuildLedger privacy policy — owner draft

Updated 17 October 2026. Replace all bracketed owner details and have the actual release/service reviewed before publication.

Developer/controller: [legal business name and address]
Support and deletion requests: [support email]
Public privacy-policy URL: [HTTPS URL]
Public account-deletion request URL: [HTTPS URL]

## Accounts and storage choice

BuildLedger uses Supabase Auth to broker Google, Apple ID or Microsoft login. These providers process the sign-in under their own terms. The app receives an authenticated account identifier, email/profile information when provided, and native session tokens. Tokens are encrypted in app-private storage and are not exposed to the packaged JavaScript interface. Choosing a provider does not authorize access to that provider's Drive, iCloud Drive or OneDrive.

After signing in, users choose Phone or BuildLedger Cloud. Phone project data stays on that phone unless deliberately exported. The account/auth service still processes login information. Cloud choice stores the builder profile, projects, legal/property documents, drawings, budgets, buyer/contact/financial records and imported files in the owner's private Supabase storage region [region]. The app maintains a phone copy and synchronizes complete snapshots while open/resumed. Cloud access policies limit records to the owning account. There is no end-to-end encryption claim.

## Local records and imports

Workspace metadata and account tokens use Android Keystore encryption. Imported file bytes are stored in the app's private directory. A configured device screen lock protects access on launch and after background inactivity. External viewers receive read-only access only to a document the user chooses to open.

Android's file picker is used for imports and exports. Available providers may include local files, Google Drive and other installed services. ZIP backups contain readable records and documents and are not encrypted. CSV/PDF reports contain the information exported by the user. Selected providers and viewers process files under their own terms.

## Retention and deletion

Records remain until deleted by the user or through the owner's documented retention process [finalize server backup/retention period]. Delete phone copy removes this account's local workspace/documents and preserves cloud records and exports. Account deletion removes the authenticated account, its BuildLedger cloud objects and the current device's local copy, when the deployed deletion endpoint succeeds. Other devices' offline copies and exported files must be deleted separately. Users can also request deletion at [public deletion URL]. Authentication/server logs and backup retention must be accurately disclosed by the operator.

Earlier 0.1.0 phone records are preserved separately and can be opened or explicitly copied into an empty signed-in account. The earlier owner-private website is a separate service; this Android app does not sync to it.

## Permissions and sharing

The app requests Internet access for configured login and cloud sync. It has no ads or analytics SDKs and requests no broad storage, location, contacts or camera permissions. Third parties include the configured identity provider, Supabase, chosen file providers and external document viewers. [Finalize any processor agreement, support systems, hosting region and jurisdiction-specific disclosures.]

Do not publish this draft until owner identity/contact, account-deletion URL, actual provider configuration, hosting/retention and Play Data safety answers match the production service.
