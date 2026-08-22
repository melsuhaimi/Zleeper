# Unresolved design decisions

The architecture requires undefined behavior to be recorded instead of guessed.

| Decision | Current handling | Required before |
| --- | --- | --- |
| Begin-Sleep expedition region selection | Recovery preserved the session seed, but the authoritative rule that selects and persists the expedition `regionId` at explicit Begin Sleep was lost. The current recovered resolver still chooses the region during Morning Resolution. Do not invent a replacement rule or nullable/placeholder region. | Dependency-complete refinement candidate |
| Distribution signing identity | No signing material in repository. | P16 release architecture |

## Recovery note

The source-of-truth lifecycle requires the deterministic night expedition to be durably sealed when the user explicitly begins sleep, before process death can occur. The recovered `SleepStartService` currently persists the session/seed but not an `ExpeditionEntity`; `NightResolutionService` still computes `chooseRegion(pet.level, reachBand)` after finalization. This mismatch remains intentionally visible until the original region-selection authority can be recovered or an explicit replacement decision is approved.
