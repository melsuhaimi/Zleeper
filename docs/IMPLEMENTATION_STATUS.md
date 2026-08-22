# Zleeper implementation status

## Living-world refinement candidate

Current work on `feat/living-world-progression` implements the approved Living Companion / Persistent World / Meaningful Routine / Equipment-Synthesis refinement on top of the recovered Android candidate. Product refinement is complete; current work is candidate hardening only.

Implemented and connected in the current source tree:

- Android application ID `com.zleeper.sleepapp`, product name Zleeper, min SDK 23
- Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, WorkManager, and KSP
- top-level World, Sleep, Journal, and Menu navigation plus onboarding and morning flows
- dedicated World Hub, Region Map, Platform Scene, Journal detail/replay/trends, Pet Profile, Equipment/Synthesis, Inventory, Quest Log, Collections, Settings, Permissions, and Data Management feature boundaries
- V2 Room model with 25 tables, immutable economy/progression ledgers, and persisted world/pet refinement state
- explicit Begin Sleep with durable `ARMED -> TRACKING` transition, manual fallback, wake/reminder scheduling, boot recovery, and 48-hour raw-signal retention
- wake handling with durable `WAKE_PENDING -> REVIEW_PENDING` transition; interrupted wake resolution reuses the persisted wake anchor instead of re-timestamping the night
- deterministic manual/platform sleep-signal identities so duplicate callbacks or wake retries collapse onto the same persisted signal instead of creating duplicate rows
- notification Begin Sleep delegates to the same `SleepStartService` path as in-app Begin Sleep, without a second Sleep API subscription; duplicate notification taps are failure-safe
- boot restoration treats both `ARMED` and `TRACKING` sessions as eligible for automatic signal re-registration while manual fallback remains valid
- derived bedtime + wake-time planning with no independently persisted duration target
- confirm-or-correct morning review, optional persisted reflections, durable reward-resolution retry, and idempotent Morning Result reconstruction
- one persistent pet with authored forms, Energy/Focus/Resilience affinities, Dream Sparks, specialization state, memories, and player-selected evolution
- Hearth memory stages, permanent discoveries, scene-completion history, titles, and tracked quests
- exact inventory instances, four equipment slots, deterministic synthesis traits, enhancement/refinement, salvage, infusions, and ledger-backed acquisition provenance
- Behavior, World, and Hybrid quest lifecycle with accept/track/abandon/claim semantics and persisted objective progress
- deterministic game simulation with touch input, region audio, reduced motion, haptics, screen-shake control, tracked-quest HUD, region-entry events, and persistent scene completion
- independent game-music, region-ambience, and SFX controls; no placeholder sleep-audio control
- Activity Recognition onboarding request with manual fallback; notification and exact-alarm access requested just in time for the related user setting
- local JSON export and explicit Delete Sleep History / Reset Game Progress / Delete All Local Data controls
- no active `ProductionScreens.kt` monolith and no legacy three-value sleep-plan or aggregate-volume ViewModel API
- deterministic JVM test definitions covering sleep resolution, progression, unbounded levels, pet stat affinity rollover, expedition/loot determinism, quest lifecycle/typed events, schedule calculations, journal trends, UI-state derivation, game simulation, collision boundaries, duplicate Sleep API signal identity, and reward-worker retry policy
- Compose instrumentation test definitions covering top-level navigation plus the architecture-required World, Sleep, Morning Reveal, Inventory, and Quest surfaces; Room V2 table expectations and production content/assets also have Android instrumentation coverage
- persistence instrumentation definitions covering `ARMED` persistence, `WAKE_PENDING` retry/reuse, duplicate signal collapse, one expedition per sleep session, Room transaction rollback, duplicate morning-resolution reward idempotency, database reopen preservation of active-night seed/wake anchor, and V1-to-V2 migration/data preservation
- Room V2 entity SQL defaults are aligned with the authored V1-to-V2 migration, and production database DI registers `ZleeperMigrations.MIGRATION_1_2` from its actual owner
- the committed Room schema directory is exposed to androidTest so `MigrationTestHelper` can validate V1-to-V2 once KSP produces the authoritative V2 schema

## Not yet verified as CI-ready

This repository status must not claim release or CI readiness until the following remaining gates are closed:

1. Resolve the recorded Begin-Sleep expedition-region recovery seam in `UNRESOLVED_DECISIONS.md` without inventing undefined behavior. The sleep session and seed are durably armed, but the architecture also requires the night expedition itself to be created at Begin Sleep and the authoritative non-null `regionId` rule is still unrecovered.
2. Run an actual Android compiler pass for the current refined source tree, including Compose compiler, Hilt/Dagger code generation, Room/KSP, and migration integration.
3. Generate the authoritative Room V2 `2.json` schema from the compiler; do not fabricate it, then execute the defined V1-to-V2 migration validation against that generated schema.
4. Execute the defined JVM, persistence, Compose, content/schema, lint/static, instrumentation/device, and debug APK validation gates against one frozen exact commit.
5. Verify permission grant/denial/retry, exact alarm, notification delivery, boot restoration/re-registration, actual process-kill/restart recovery, real WorkManager retry behavior, navigation, and game rendering on Android runtime.
6. Add only Android-runtime evidence exposed by those runs; do not replace platform behavior with superficial unit-test substitutes.
7. Freeze and review the exact dependency-complete candidate before re-enabling or invoking GitHub Actions.

The GitHub Actions workflow remains intentionally disabled during this refinement/recovery phase. `main` is not the validation target and CI must not be triggered until explicit user approval is given for the frozen candidate.

## Release boundary

Distribution keystore material and store credentials remain external to the repository by design. Release signing and production distribution remain P16 work after the exact candidate has passed the required verification path.
