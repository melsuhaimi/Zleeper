# Zleeper

Zleeper is an Android-only, phone-first sleep companion whose nightly routine
powers one persistent pet and a persistent 2D RPG world.

This repository currently contains the P1 product foundation. It intentionally
does not contain speculative sleep logic, game content, or a browser-game stack.

The implementation authority is
[docs/SLEEP_APP_SOURCE_OF_TRUTH_ARCHITECTURE.md](docs/SLEEP_APP_SOURCE_OF_TRUTH_ARCHITECTURE.md).

## Build

Requirements:

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- no system Gradle installation; the checked-in wrapper uses Gradle 9.1.0

Run:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

GitHub Actions performs the authoritative exact-commit build and publishes the
debug APK artifact.
