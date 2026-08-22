# Zleeper implementation status

## Living-world refinement candidate

Current work on `feat/living-world-progression` implements the approved Living Companion / Persistent World / Meaningful Routine / Equipment-Synthesis refinement on top of the recovered Android candidate.

Implemented and connected in the current source tree:

- Android application ID `com.zleeper.sleepapp`, product name Zleeper, min SDK 23
- Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, WorkManager, and KSP
- top-level World, Sleep, Journal, and Menu navigation plus onboarding and morning flows
- dedicated World Hub, Region Map, Platform Scene, Journal detail/replay/trends, Pet Profile, Equipment/Synthesis, Inventory, Quest Log, Collections, Settings, Permissions, and Data Management feature boundaries
- V2 Room model with 25 tables, immutable economy/progression ledgers, and persisted world/pet refinement state
- Sleep API signal ingestion, explicit Begin Sleep, manual fallback, wake/reminder scheduling, boot recovery, and 48-hour raw-signal retention
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
- deterministic JVM test definitions covering sleep resolution, progression, unbounded levels, pet stat affinity rollover, expedition/loot determinism, quest lifecycle/typed events, schedule calculations, journal trends, UI-state derivation, game simulation, and collision boundaries
- Compose instrumentation test definitions covering top-level navigation plus the architecture-required World, Sleep, Morning Reveal, Inventory, and Quest surfaces; Room V2 table expectations and production content/assets also have Android instrumentation coverage

## Not yet verified as CI-ready

This repository status must not claim release or CI readiness until the following remaining gates are closed:

1. Resolve the recorded Begin-Sleep expedition-region recovery seam in `UNRESOLVED_DECISIONS.md` without inventing undefined behavior.
2. Run an actual Android compiler pass for the current refined source tree, including Compose compiler, Hilt/Dagger code generation, Room/KSP, and migration integration.
3. Generate the authoritative Room V2 `2.json` schema from the compiler; do not fabricate it.
4. Execute the defined unit and Compose tests, Android lint/static architecture checks, instrumentation/device checks, and debug APK assembly/validation against one frozen exact commit.
5. Verify permission denial/retry, exact alarm, notification delivery, boot restoration, process-death recovery, WorkManager retry/idempotency, navigation, and game rendering on Android runtime.
6. Add/execute the remaining persistence and Android-integration evidence required by the architecture, including migration, reward idempotency, duplicate callback/resolution, process-death, boot, worker-retry, and permission-path verification.
7. Freeze and review the exact dependency-complete candidate before re-enabling or invoking GitHub Actions.

The GitHub Actions workflow remains intentionally disabled during this refinement/recovery phase. `main` is not the validation target and CI must not be triggered until explicit user approval is given for the frozen candidate.

## Release boundary

Distribution keystore material and store credentials remain external to the repository by design. Release signing and production distribution remain P16 work after the exact candidate has passed the required verification path.
