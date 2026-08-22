# Android infrastructure recovery stage

This stage closes build/DI prerequisites for the recovered v2 implementation.

Contents:
- `DataModule` registers `MIGRATION_1_2` on the production Room database;
- `RoomSchemaTest` asserts the complete 25-table v2 source-of-truth schema;
- `app/build.gradle.kts` keeps the green dependency/toolchain contract and extends `validateGameAssets` to require content pack v2 plus the existing production music, ambience, and wake SFX assets.

Validation performed without CI:
- `INFRA_BUILD_PARITY_OK` (19 content-manifest files represented in preBuild requirements)
- `INFRASTRUCTURE_SOURCE_AUDIT_OK`
- Kotlin parser scan found no syntax diagnostics; Android/Room/Hilt code generation is intentionally not claimed.

Archive SHA-256:
`814cdfc639bf28fa040819f76f80555d8b7a52be6d9130a00194a5e70974f178`

The branch workflow remains disabled as `.github/workflows/android.yml.bak`.
