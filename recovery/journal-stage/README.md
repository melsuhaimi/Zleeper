# Journal recovery stage

This stage implements the source-of-truth Journal boundaries using only persisted finalized session data.

Contents:
- immutable `JournalNight` / trend projection models;
- `JournalRepository` combining persisted sleep sessions, outcomes, expeditions, path nodes, rewards, and morning notes;
- dedicated `JournalViewModel` / StateFlow;
- real Overview -> Sleep Session Detail -> Journey Replay Compose boundaries;
- authored expedition narrative lookup by persisted `outcomeTextKey`;
- accessible trend graph semantics;
- JVM trend tests.

Journey Replay is presentation-only: it never calls Night Resolution or an expedition resolver and never grants rewards.
Raw Sleep API signals are not part of Journal history.

Validation performed without Android CI:
- `JOURNAL_TRENDS_OK`
- `JOURNAL_VIEWMODEL_KOTLIN_COMPILE_OK`
- `JOURNAL_SOURCE_AUDIT_OK`
- Compose parser scan: no Kotlin syntax/parser diagnostics; Android/Compose classpath remains unavailable locally.

Archive SHA-256:
`17c69c1494d61fa8ccb34c9cdc5ffb9dc22518fea5b84893116e4f6907810f4f`

The branch workflow must remain `.github/workflows/android.yml.bak`; no CI has been authorized.
