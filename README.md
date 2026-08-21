# Zleeper

Zleeper is an Android-only, phone-first sleep companion whose nightly routine
powers one persistent pet and a persistent 2D RPG world.

This branch contains a complete local-first vertical-slice implementation of the
source architecture: onboarding, sleep sensing and scheduling, deterministic
morning resolution, pet progression, quests, inventory, equipment, collections,
world exploration, authored game content, and the native Compose game surface.

The implementation authority is
[docs/SLEEP_APP_SOURCE_OF_TRUTH_ARCHITECTURE.md](docs/SLEEP_APP_SOURCE_OF_TRUTH_ARCHITECTURE.md).

## Build

Requirements:

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- no system Gradle installation; the checked-in wrapper uses Gradle 8.13

Run:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

GitHub Actions performs the authoritative exact-commit build and publishes the
debug APK artifact. Release signing material is intentionally external to the
repository.
