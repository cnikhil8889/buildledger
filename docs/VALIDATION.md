# GitHub source validation

Checked 19 October 2026 against the cleaned version 0.3.0 source package.

| Check | Result |
| --- | --- |
| TypeScript | Passed |
| JavaScript/domain/database tests | 12 passed |
| Personal UI browser checks | Passed, including 320–800 px widths, large text and keyboard forms |
| Company UI browser checks | 105 role/width/view combinations and 16 forms passed |
| Native JVM tests | 12 passed |
| Android lint (debug) | 0 issues |
| Debug APK | Compiled successfully |
| Release AAB | Compiled successfully; verified unsigned |
| GitHub workflow | actionlint passed; YAML and embedded shell syntax checked |
| Pinned actions | Commit references resolved from their official repositories |
| Gradle wrapper | Official 8.13 wrapper SHA-256 matched |
| Source package | Local configuration, signing keys, APK/AAB outputs, caches and reports excluded |

Commands: `npm run check`, `npm test`, `npm run build`, `npm run test:layout`, `npm run test:team`; Gradle 8.13 with `testDebugUnitTest lintDebug assembleDebug bundleRelease`. Java 17, SDK 36 and Build Tools 36.0.0 were used. Browser checks used Chromium headless-shell with test fixtures and a local PGlite database.

The app UI source, branding and all backend SQL/function files match the working 0.3.0 kit. Packaging changes remove live project configuration and the owner-private website link, add a clear response for an unconfigured web workspace, update setup/privacy text and add GitHub automation/documentation.

This source distribution compiles with empty backend settings and leaves sign-in setup pending. No real user data or backend settings were changed. The included workflow has been validated locally; it has not run on GitHub because no repository was uploaded in this session. Actual-device OAuth, native file providers and multi-device recovery were not retested for this packaging change.
