# Unresolved design decisions

The architecture requires undefined behavior to be recorded instead of guessed.

| Decision | Current handling | Required before |
| --- | --- | --- |
| Distribution signing identity | No signing material in repository. Release keystore/store credentials remain external by design. | P16 release architecture |

## Clarified Begin-Sleep expedition boundary

The recovery note previously treated a missing pre-sleep `ExpeditionEntity.regionId` rule as a blocker. That interpretation was too strong and is no longer the implementation contract.

The source-of-truth architecture's specific lifecycle and deterministic-resolution sections require explicit Begin Sleep to durably persist the sleep-session timestamp and a `SecureRandom` expedition seed. Morning resolution then combines that seed with the finalized sleep outcome, pet state, content-pack version, and progression-rules version; the atomic reward transaction inserts the resolved expedition/path/reward ledger and marks the session `EXPEDITION_RESOLVED`.

This is also the behavior of the last fully Android-validated baseline, `fb66f9a5bd2e3b8792a7f10b7a1f806c8455b650`: Begin Sleep persisted the seed on `sleep_session`, while `NightResolutionService` selected the region from the finalized reach result and inserted `ExpeditionEntity` during resolution.

Accordingly, the high-level product wording that Begin Sleep “creates and persists the night expedition” is implemented as sealing the deterministic night identity through the persisted session + expedition seed. It does **not** require inventing a partially populated expedition row or a pre-sleep region selection rule before reach is known.

This clarification does not weaken determinism: the persisted seed is created once at Begin Sleep, and retries/process death must use that same seed and persisted finalized inputs. Resolved expedition history remains immutable and idempotent.
