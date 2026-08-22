# Platform/application recovery stage

This stage continues the approved living-world progression implementation from the durable service-layer recovery snapshot.

It contains 16 source/test files implementing:

- bedtime/wake sleep plan with duration derived from bedtime -> wake;
- independent music / ambience / SFX settings;
- bedtime and wind-down reminder scheduling;
- explicit notification actions: Begin Sleep / 15m Later / Skip Tonight;
- reminder delivery-time setting checks and midnight-safe recurrence;
- durable unique morning reward WorkManager scheduling;
- boot/package-replacement restoration for alarms, reminders, tracking subscription, and pending reward work;
- complete local data export/reset coverage for the 25-table Room v2 contract plus DataStore settings;
- manifest registration for the new reminder receivers.

Validation performed without Android CI:

- `SCHEDULE_TIMES_OK`
- `SETTINGS_REPOSITORY_KOTLIN_COMPILE_OK`
- `DATA_TABLE_COVERAGE_OK 25`
- `PLATFORM_SOURCE_AUDIT_OK`

The Android SDK / Gradle / Hilt / Room / WorkManager runtime has not been executed for this stage. The branch workflow remains intentionally disabled as `.github/workflows/android.yml.bak`.

Archive SHA-256:

`ec45126089be5bc6fcaddb21b01a8aeed4cab3d3c3300b4e2c1a27152dce2723`
