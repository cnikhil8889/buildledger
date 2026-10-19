# Working on BuildLedger

Open changes on a branch and use a pull request into `main`. Include the concrete behavior changed, checks run and any migration/device impact.

After UI edits, run `npm ci`, `npm run check`, `npm test` and `npm run build` in `web/`. Commit the rebuilt assets with the source. For layout or workflow changes, run the responsive checks with `CHROME_BIN` configured.

Run `./gradlew testDebugUnitTest lintDebug assembleDebug` for native changes. Test authentication, storage and document behavior on actual Android devices when those flows change.

Keep backend changes additive and review access rules with owner, office, worker and unrelated-company fixtures. Existing migrations are deployment history; new database changes belong in new migrations. Do not run setup SQL again against an already configured project.

Use local ignored configuration and owner-held signing material. Choose a project license before offering public reuse; third-party dependency notices remain in `THIRD_PARTY_NOTICES.txt`.
