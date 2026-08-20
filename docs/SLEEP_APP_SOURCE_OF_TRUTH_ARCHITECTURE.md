# Sleep App — Source of Truth Architecture

## 1. Purpose and Authority

This file is the design authority for the Android Sleep App.

It defines:

- the product rules;
- the sleep-to-game loop;
- the Android runtime architecture;
- the pet, progression, expedition, quest, world, and game systems;
- state, persistence, content, and asset contracts;
- background, privacy, accessibility, and failure behavior;
- build, test, and verification boundaries.

Implementation must follow this file unless an approved requirement explicitly changes it.

If a required behavior is not defined here, do not guess it. Record it as an unresolved design decision before implementation.

The following creative content is replaceable and must remain data-driven:

- product and brand name;
- pet species, form, and display names;
- NPC names;
- region and scene names;
- item and quest names;
- lore, narrative, dialogue, and final artwork.

Display text must never be used as a database key, content key, file identity, or code identity.

---

## 2. Product Definition

The product is an Android-only, phone-first sleep companion. The player's real nightly routine powers one persistent pet and a persistent 2D RPG world.

The pet is the single main object of attachment.

The complete product loop is:

```text
Sleep Plan
    ↓
Wind-Down
    ↓
Begin Sleep
    ├── create and persist a night expedition
    ↓
Phone-derived signals and user anchors
    ↓
Resolved Sleep Session
    ↓
Progression and Expedition Resolvers
    ├── XP
    ├── pet stat growth
    ├── expedition reach
    ├── loot
    ├── quest progress
    └── world discoveries
    ↓
Morning Confirmation
    ↓
Morning Reveal
    ↓
Pet and persistent world update
    ↓
Short active 2D exploration
    ↓
Return to the daily sleep loop
```

### 2.1 Product invariants

These rules are enforced by architecture and code.

| Rule | Required consequence |
| --- | --- |
| No wearable is required. | Use phone-derived signals and user input. |
| Do not claim medical precision. | Do not present game logic as sleep stages, clinical data, or verified biological measurements. |
| Poor sleep must not harm the pet. | No starvation, death, sadness penalty, or broken-streak punishment. |
| Every legitimate finalized night gives progress. | Apply a base participation reward. |
| Better routines create more opportunity. | Increase expedition reach, discovery, and loot opportunity; never pet survival. |
| Do not render gameplay overnight. | Resolve the night as a deterministic simulation. |
| Daytime play remains lightweight. | Active scenes normally last about 2–5 minutes. |
| Game items affect game systems only. | Never label an item as improving deep sleep, REM, or a health score. |
| Sleep data and RPG stats are separate. | Pet Energy, Focus, and Resilience are game values only. |
| The core product does not require a network. | The first release is local-first and fully playable offline. |
| Sleep Mode is quiet. | No busy game HUD, particle spam, or unexpected RPG music. |
| Morning is the main emotional payoff. | The reward reveal may be expressive and cinematic. |

### 2.2 Explicit non-goals

The initial architecture does not include:

- a required account or backend;
- a cloud progression dependency;
- wearable integration;
- microphone or raw-audio sleep analysis;
- medical diagnosis or sleep-stage claims;
- combat or an HP system;
- a seven-hour live game simulation;
- a speculative advertising SDK;
- premature expansion into many Gradle modules.

---

## 3. System Context

```text
                         SLEEP APP
                             │
          ┌──────────────────┴──────────────────┐
          │                                     │
      SLEEP SYSTEM                         GAME SYSTEM
          │                                     │
      Sleep plan                         Persistent hub
      Wind-down                          Active platform scenes
      User anchors                       Quests and discoveries
      Phone signals                      Items and equipment
      Sleep resolver                     Regions and collections
          │                                     │
          └──────────────────┬──────────────────┘
                             ▼
                         PET SYSTEM
                    XP / Stats / Evolution
                             │
                             ▼
                    NIGHT EXPEDITION
                  deterministic simulation
                             │
                 Loot / Discoveries / Quests
                             │
                             ▼
                     MORNING REVEAL
                             │
                             ▼
                     Persistent world
                             │
                             └───────────────↺
```

### 3.1 Runtime and engineering separation

```text
Android runtime shipped in APK/AAB
├── Kotlin
├── Jetpack Compose and Material 3
├── ViewModels and Kotlin Flow
├── domain and repository boundaries
├── Room and DataStore
├── Android platform integrations
└── 2D game runtime

Engineering infrastructure never shipped in APK/AAB
├── repository instructions
├── deterministic harness and controller scripts
├── GitHub Actions
├── schemas and validators
├── build evidence
└── exact-commit verification
```

The Android runtime must have no dependency on the engineering harness.

---

## 4. Fixed Technology Decisions

| Concern | Decision |
| --- | --- |
| Platform | Android only; phone first; portrait first. |
| Language | Kotlin. |
| App UI | Jetpack Compose with Material 3. |
| Activity model | Single activity. |
| Code organization | Feature-oriented modular monolith. |
| Presentation | Unidirectional data flow. |
| Screen state | ViewModel plus `StateFlow`. |
| One-shot effects | `Channel` exposed with `receiveAsFlow()`. |
| Local UI state | Compose state or a plain `@Stable` state holder. |
| Business logic | Required domain layer for sleep, progression, expedition, quest, inventory, pet, and world logic. |
| Dependency injection | Hilt with constructor injection. |
| Structured persistence | Room. |
| Small settings | DataStore. |
| Durable background work | WorkManager. |
| Sleep estimation | Explicit user session plus Google Play services Sleep API plus targets plus morning confirmation. |
| Automatic-detection fallback | Manual start and wake must remain fully supported. |
| Active game | Custom deterministic 2D runtime separated from Compose application state. |
| Build authority | GitHub Actions at an exact Git commit. |

Do not introduce an additional framework, service, storage system, sensor strategy, or Gradle module without a concrete requirement.

---

## 5. Dependency Direction

Runtime dependencies point inward through stable abstractions.

```text
Compose Screen / Route
        ↓
ViewModel and application state
        ↓
Domain logic
        ↓
Repository interface
        ↓
Data source or platform implementation
        ↓
Room / DataStore / Android OS / content assets
```

Rules:

- UI code may depend on domain models and presentation models.
- Domain code must not depend on Compose, Android `Context`, Room, WorkManager, or Google Play services classes.
- Repository interfaces belong at the application or domain boundary.
- Repository implementations belong in the data layer.
- Android-specific implementations belong in the platform layer.
- Reusable composables must not receive a repository, database, platform service, or ViewModel.
- The game frame loop must not query Room or call repositories every frame.

---

## 6. Repository and Package Structure

Start as one application Gradle module. Create another Gradle module only when a measured build, ownership, reuse, or isolation need exists.

```text
sleep-app/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/<application-package>/
│       │   │   ├── app/
│       │   │   ├── navigation/
│       │   │   ├── core/
│       │   │   ├── ui/
│       │   │   ├── feature/
│       │   │   ├── domain/
│       │   │   ├── game/
│       │   │   ├── data/
│       │   │   └── platform/
│       │   ├── assets/game/
│       │   └── res/
│       ├── test/
│       └── androidTest/
├── art/
│   ├── source/
│   └── export/
├── gradle/
├── tools/harness/
├── .github/workflows/
├── settings.gradle.kts
└── gradlew
```

### 6.1 Kotlin package layout

```text
<application-package>/
├── app/
│   ├── SleepApplication.kt
│   ├── MainActivity.kt
│   └── App.kt
├── navigation/
│   ├── AppNavigation.kt
│   ├── AppRoute.kt
│   └── NavigationState.kt
├── core/
│   ├── id/
│   ├── time/
│   ├── result/
│   └── util/
├── ui/
│   ├── theme/
│   ├── components/
│   └── animation/
├── feature/
│   ├── onboarding/
│   ├── world/
│   ├── sleep/
│   ├── morning/
│   ├── journal/
│   ├── pet/
│   ├── inventory/
│   ├── quest/
│   └── settings/
├── domain/
│   ├── sleep/
│   ├── expedition/
│   ├── progression/
│   ├── pet/
│   ├── inventory/
│   ├── equipment/
│   ├── quest/
│   └── world/
├── game/
│   ├── runtime/
│   ├── simulation/
│   ├── physics/
│   ├── input/
│   ├── camera/
│   ├── render/
│   ├── animation/
│   └── scene/
├── data/
│   ├── local/
│   │   ├── database/
│   │   └── preferences/
│   ├── content/
│   ├── mapper/
│   └── repository/
└── platform/
    ├── sleep/
    ├── alarm/
    ├── notification/
    ├── permission/
    ├── boot/
    └── audio/
```

Create a platform subdirectory only when that capability is implemented.

---

## 7. Feature, UI, and State Architecture

### 7.1 Feature contract

A substantial feature follows this structure:

```text
feature/<feature>/
├── <Feature>Route.kt
├── <Feature>Screen.kt
├── <Feature>ViewModel.kt
├── <Feature>UiState.kt
└── <Feature>Action.kt
```

Add a separate one-shot event type only when the feature has genuine single-consumer effects.

```text
Repository or Domain
        ↓
ViewModel
        │ StateFlow<UiState>
        ↓
Route
        ↓
Screen
        │ Action callback
        └──────────────→ ViewModel
```

The screen is close to a pure rendering function:

```kotlin
@Composable
fun FeatureScreen(
    state: FeatureUiState,
    onAction: (FeatureAction) -> Unit,
)
```

### 7.2 State ownership

Use Compose-owned state for values that belong to one UI element or one local interaction:

- sheet visibility;
- text input;
- selected visual tab;
- focus state;
- scroll-related presentation state.

Use:

- `remember` for transient local state;
- `rememberSaveable` for restorable local state;
- a plain `@Stable` holder when several related local values and operations require coordination.

ViewModels own screen and application state.

```text
MutableStateFlow
       ↓
StateFlow<FeatureUiState>
```

Prefer atomic state updates:

```kotlin
_state.update { current ->
    current.copy(/* changed values */)
}
```

Use a `Channel` for navigation and transient, single-consumer effects:

```text
Channel
   ↓ receiveAsFlow()
FeatureRoute
```

Do not represent a one-shot effect as persistent render state.

### 7.3 Complete UI states

Every applicable feature explicitly handles:

```text
Loading
Content
Empty
Error
PermissionDenied
CapabilityUnavailable
```

Use a sealed state when states are mutually exclusive. Use an immutable data class when parts may legitimately coexist.

No valid state may become an accidental blank screen.

---

## 8. Navigation and Screen Inventory

### 8.1 Top-level navigation

The normal bottom navigation contains:

```text
WORLD
SLEEP
JOURNAL
MENU
```

Normal bottom navigation is hidden during:

- Sleep Mode;
- Morning Confirmation;
- Morning Reveal.

### 8.2 Navigation groups

```text
WORLD
├── WorldHub
├── RegionMap
├── PlatformScene
├── NpcDialogue
└── WorldDiscovery

SLEEP
├── SleepPlan
├── WindDown
├── AlarmSetup
├── BeginSleep
└── SleepMode

JOURNAL
├── Journal
├── SessionDetail
├── Trends
└── MorningNotes

MENU
├── PetProfile
├── Equipment
├── Inventory
├── QuestLog
├── Collection
└── Settings
```

### 8.3 Required screens

```text
OnboardingWelcome
OnboardingPet
OnboardingSleepPlan
OnboardingPermissions

WorldHub
RegionMap
PlatformScene

SleepPlan
WindDown
SleepMode

WakeConfirmation
MorningReveal
JourneyReplay

Journal
SleepSessionDetail
Trends

PetProfile
Equipment
Inventory
QuestLog
Collections

Settings
Permissions
DataManagement
```

Each screen is a real feature boundary, not only a mockup page.

---

## 9. Application Lifecycle

### 9.1 First run

```text
Install
  ↓
Welcome
  ↓
Create and name pet
  ↓
Configure sleep target
  ↓
Explain phone-based estimation
  ↓
Request Activity Recognition permission
  ↓
Optional alarm setup
  ↓
Enter persistent hub
  ↓
First tutorial quest
```

### 9.2 Normal day and night

```text
WORLD
  ↓
2–5 minute activity
  ↓
SLEEP
  ↓
Wind-down
  ↓
Begin Sleep
  ↓
Sleep Mode
  ↓
Wake
  ↓
Confirm or correct estimate
  ↓
Morning Reveal
  ↓
Updated pet and world
```

---

## 10. Sleep Architecture

### 10.1 Acquisition strategy

The required first-release strategy is:

```text
Explicit user sleep session
          +
Google Play services Sleep API
          +
User sleep and wake targets
          +
Morning confirmation
```

Do not implement a continuously running accelerometer foreground service for this strategy.

Automatic estimation may use normalized Sleep API classification and sleep-segment events delivered through a platform `PendingIntent` boundary.

`ACTIVITY_RECOGNITION` is required on Android versions where the Sleep API requires it. Permission denial is a supported state, not a terminal error.

### 10.2 Component boundary

```text
platform/sleep/
├── SleepSignalSource.kt
├── PlayServicesSleepSignalSource.kt
├── SleepEventReceiver.kt
├── SleepSubscriptionManager.kt
└── SleepAvailabilityChecker.kt

domain/sleep/
├── SleepSession.kt
├── SleepSignal.kt
├── SleepResolution.kt
├── SleepConfidence.kt
├── SleepResolver.kt
└── SleepSessionRepository.kt
```

The domain layer must not import:

```text
ActivityRecognitionClient
PendingIntent
Intent
Context
```

### 10.3 Sleep state machine

```text
IDLE
  │ Begin Sleep
  ▼
ARMED
  │ persist session timestamp and expedition seed
  ▼
TRACKING
  ├── classify events
  ├── sleep-segment event
  ├── alarm
  └── manual wake
  ▼
WAKE_PENDING
  │ resolve estimate
  ▼
REVIEW_PENDING
  ├── user confirms
  └── user corrects
  ▼
FINALIZED
  ▼
EXPEDITION_RESOLVED
```

`ABORTED` is the valid terminal state for an accidental session start.

### 10.4 Signal fallback hierarchy

```text
Highest information
├── user anchors + sleep segment + classify events
├── user anchors + classify events
├── user anchors + sleep segment
└── explicit user start + manual wake only
Lowest information
```

Rules:

- The manual-only path must remain playable.
- Automatic detection improves estimation confidence.
- Automatic detection does not decide whether the user is eligible for game progress.
- Signal confidence must not reduce XP.

### 10.5 Resolved sleep record

The persistent normalized result is `ResolvedSleepSession`.

```text
ResolvedSleepSession
├── id
├── sessionStart
├── sessionEnd
├── estimatedSleepStart
├── estimatedSleepEnd
├── targetSleepStart
├── targetWakeTime
├── estimatedSleepMinutes
├── timingOffsetMinutes
├── windDownCompleted
├── resolutionMethod
├── confidence: HIGH | MEDIUM | LOW
└── finalizedAt
```

User-facing language must say that sleep is estimated and may show estimation confidence. It must not call the result medically verified sleep.

### 10.6 Raw signal retention

```text
Sleep API callback
    ↓
normalized temporary signal
    ↓
SleepResolver
    ↓
ResolvedSleepSession
    ↓
delete raw normalized signals after retention window
```

Retain raw normalized signals for no more than 48 hours after finalization. Historical Journal views use resolved sessions, not the raw event stream.

---

## 11. Nightly Progression

### 11.1 Behavioral progression quality

Do not expose one authoritative-looking biological sleep score.

The game calculates an internal `ProgressionQuality` from behavior:

```text
Base participation       30 XP
Target duration fit      0–20
Sleep timing fit         0–20
Schedule consistency     0–20
Wind-down                0–15
Morning reflection       0–5
```

The normal nightly maximum is 110 XP before quest or other game bonuses.

The values are initial configurable defaults, not constants scattered through Kotlin.

### 11.2 Expedition reach

The nightly result maps to one of five game-only reach bands:

```text
BAND 1  Trail
BAND 2  Path
BAND 3  Depth
BAND 4  Far Reach
BAND 5  Veil
```

These are RPG concepts, not health ratings.

```text
baseDepth
    + Energy contribution
    - hazard cost modified by Resilience
```

Focus may unlock hidden nodes when a configured requirement is met.

---

## 12. Pet and Progression Architecture

### 12.1 Persistent pet

The application owns one persistent pet instance.

```text
PetInstance
├── instanceId
├── speciesId
├── displayName
├── formId
├── level
├── totalXp
├── energy
├── focus
├── resilience
├── equipment
├── cosmeticLoadout
├── affinityProgress
└── createdAt
```

`displayName` is mutable presentation data and never an identifier.

### 12.2 Core stats

| Stat | Game meaning |
| --- | --- |
| Energy | Increases expedition reach and some traversal capability. |
| Focus | Reveals hidden routes, objects, and discoveries. |
| Resilience | Reduces expedition hazard cost and unlocks difficult regions. |

Do not add HP before a real combat requirement exists.

### 12.3 Affinity and stat growth

RPG stats are not direct translations of sleep measurements.

```text
User behavior
     ↓
Affinity growth points
     ↓
Configured threshold
     ↓
Pet stat increase
```

Initial mapping:

| Behavior | Affinity |
| --- | --- |
| Sleep timing consistency | Resilience |
| Wind-down completion | Focus |
| Regular sleep and wake schedule | Energy |
| Active world exploration | General XP and item progression |

Persist hidden growth-point totals separately for Energy, Focus, and Resilience. This allows future balancing without rewriting historical sleep records.

### 12.4 Level curve

Every legitimate finalized night grants XP.

Initial configurable curve:

```text
xpToNextLevel(level) = round(120 + 70 × level^1.32)
```

Keep the curve and all related tuning in a versioned progression rules asset containing:

```text
level_curve
night_base_xp
behavior_weights
stat_growth_rates
rarity_thresholds
expedition_depth_rules
```

Every historical expedition records the progression rules version that produced it.

### 12.5 Evolution

Evolution is the main long-term pet progression system.

Initial level gates:

```text
Form 01  Level 1
Form 02  Level 10
Form 03  Level 30
Form 04  Level 60
Form 05  Level 100 plus a major quest
```

Behavior and accumulated affinities unlock choices. They must not automatically select a form. The player selects among unlocked forms.

No evolution path may imply that a user received a bad pet because they slept poorly.

---

## 13. Inventory and Equipment

### 13.1 Equipment slots

```text
HEAD
CHARM
PACK
RELIC
```

| Slot | Primary role |
| --- | --- |
| Head | Cosmetics and minor game utility. |
| Charm | Pet stat or interaction modifiers. |
| Pack | Expedition and exploration modifiers. |
| Relic | Unusual world mechanics. |

Equipment effects must target game systems only.

### 13.2 Inventory categories

```text
MATERIAL
EQUIPMENT
CONSUMABLE
QUEST
KEY
COSMETIC
COLLECTIBLE
RELIC
```

### 13.3 Rarity

```text
COMMON
UNCOMMON
RARE
EPIC
MYTHIC
```

Rarity must use a text label and non-color visual treatment such as a symbol, icon shape, or border. Color alone is not sufficient.

### 13.4 Stack rules

| Category | Rule |
| --- | --- |
| Materials | Stack. |
| Consumables | Stack. |
| Quest items | Configurable. |
| Keys | Normally unique. |
| Equipment | Individual instances. |
| Cosmetics | Ownership flag. |
| Collectibles | Ownership flag or count. |
| Relics | Individual instances. |

---

## 14. Quest and Domain Event Architecture

### 14.1 Quest families

There are three quest families.

1. **Behavior quests** use real-life sleep behavior. Use forgiving windows such as completing an objective three times within seven nights. Do not require fragile consecutive-night streaks.
2. **World quests** use only RPG activity such as exploring, collecting, talking, or discovering.
3. **Hybrid quests** combine behavior and world objectives and are the signature connection between sleep and RPG progress.

### 14.2 Typed objective primitives

Build quests from typed objective definitions. Do not hard-code individual quest logic inside a screen.

```text
SleepWithinTargetWindow
CompleteWindDown
FinalizeSleepSession
CompleteMorningReview
AccumulateConsistency
ReachPetLevel
ReachPetStat
EnterRegion
CompleteScene
CollectItem
EquipItem
TalkToNpc
InteractWithObject
DiscoverNode
CompleteExpedition
```

Every objective definition contains a stable objective type and its required parameters, such as count, time window, item ID, NPC ID, region ID, or scene ID.

### 14.3 Domain events

The Quest Engine consumes domain events instead of reading arbitrary UI state.

```text
SleepSessionStarted
WindDownCompleted
SleepSessionFinalized

ExpeditionStarted
ExpeditionResolved

PetXpGranted
PetLeveledUp
PetStatIncreased
PetFormUnlocked

ItemGranted
ItemConsumed
EquipmentChanged

RegionUnlocked
SceneCompleted
WorldNodeDiscovered

QuestObjectiveProgressed
QuestCompleted
```

Domain events are the common integration language between systems.

---

## 15. Night Expedition Architecture

### 15.1 Expedition graph

Each region owns a directed expedition graph.

```text
start → path node → event node → destination node
             └──→ reward node → optional branch
```

Every node contains:

```text
nodeId
type
depthCost
requirements
lootTableId
eventId
questHooks
nextNodes
```

The overnight resolver walks this graph using the night's expedition budget. It does not run the active platform game overnight.

### 15.2 Deterministic resolution

At `Begin Sleep`:

```text
SecureRandom
    ↓
expeditionSeed
    ↓
persist immediately
```

Resolution input is:

```text
expeditionSeed
+ finalized sleep outcome
+ pet state
+ contentPackVersion
+ progressionRulesVersion
        ↓
ExpeditionResolver
        ↓
ExpeditionResult
```

The same complete input must always produce the same result. Closing, reopening, process death, and retry must never reroll rewards.

### 15.3 Atomic and idempotent rewards

Apply morning rewards in one Room transaction:

```text
BEGIN TRANSACTION
  verify expedition is not already RESOLVED
  insert expedition result
  insert reward ledger entries
  apply XP
  apply stat growth
  apply inventory changes
  apply quest progress
  mark expedition RESOLVED
COMMIT
```

If the process dies before commit, the transaction rolls back. A retry must create exactly one reward set.

---

## 16. Morning Experience

### 16.1 Morning workflow

```text
Wake
  ↓
Resolve sleep estimate
  ↓
Ask user to confirm or adjust
  ↓
Finalize ResolvedSleepSession
  ↓
Resolve expedition exactly once
  ↓
Morning Reveal
  ├── expedition reach
  ├── XP
  ├── items
  ├── discoveries
  └── quest changes
```

The reveal includes an action to view the journey.

### 16.2 Journey reconstruction

The journey is not a recorded video and not an overnight recording.

It is a short cinematic reconstruction of `ExpeditionResult.path`. Authored vignettes display the recorded sequence of graph nodes.

The reconstruction must use the already-resolved result and must not execute reward logic again.

---

## 17. Active 2D RPG Architecture

### 17.1 Active loop

```text
Open WORLD
   ↓
Explore persistent hub
   ↓
Choose unlocked route
   ↓
Play a 2–5 minute platform scene
   ↓
Collect, interact, discover, or advance a quest
   ↓
Reach checkpoint
   ↓
Return to hub
```

### 17.2 Initial verbs

```text
MOVE
JUMP
DROP
INTERACT
COLLECT
INSPECT
TALK
EQUIP
DISCOVER
```

Do not add combat or an attack button to the initial product.

### 17.3 Portrait platforming

Scenes are side-view micro-regions about 1.5–3 phone-screen widths, with camera tracking.

Controls:

```text
bottom-left: left and right movement
bottom-right: jump
context action: shown near an interactable
```

Do not use a permanent multi-button RPG controller.

### 17.4 Game runtime boundary

```text
GameInput
   ↓
GameSimulation
   ↓
GameState
   ↓
Renderer
```

Scene lifecycle:

```text
enter scene
    ↓
load WorldSnapshot
    ↓
run simulation in memory
    ↓
emit meaningful GameEvent
    ↓
persist through repository
```

Persist meaningful events such as:

- checkpoint reached;
- quest interaction;
- item collected;
- scene completed;
- permanent world-object change.

Do not persist frame-by-frame coordinates.

### 17.5 Simulation and rendering

Pure Kotlin simulation:

```text
game/simulation/
├── GameState
├── GameEntity
├── MovementSystem
├── CollisionSystem
├── InteractionSystem
├── CollectibleSystem
└── GameEvent
```

Android rendering:

```text
game/render/
├── GameRenderer
├── SpriteRenderer
├── WorldRenderer
├── EffectRenderer
└── HudRenderer
```

### 17.6 Physics

Use deterministic, purpose-built physics:

```text
tile collision
AABB collision
gravity
velocity
ground detection
one-way platforms
trigger volumes
```

This supports walking, jumping, falling, platforms, collection, interaction, hazards, and checkpoints.

Do not add a heavyweight general-purpose physics engine unless future mechanics require rigid bodies, ropes, projectiles, or physics puzzles.

---

## 18. World Architecture

### 18.1 Persistent hub

The world has one persistent hub that changes visibly over long-term use.

Examples of valid structural growth include:

- a larger shelter;
- a workbench or garden;
- visiting NPCs;
- displayed relics;
- quest structures;
- rare environmental effects;
- visible changes associated with pet progression.

After months of use, the hub must visually show accumulated progress rather than only display a numeric night counter.

### 18.2 Region contract

Every region has one `RegionDefinition`:

```text
RegionDefinition
├── regionId
├── displayName
├── unlockRules
├── expeditionGraph
├── activeScenes
├── npcs
├── lootTables
├── questHooks
├── ambience
├── palette
└── assetBundle
```

The first complete product proof contains one hub and two connected production regions. Final display names and lore are content, not architecture.

---

## 19. Persistence Architecture

### 19.1 Room responsibilities

Use Room for structured and relational data:

- resolved sleep sessions;
- temporary normalized sleep signals;
- nightly outcomes;
- pet state;
- progression ledgers;
- expeditions and expedition paths;
- expedition rewards;
- inventory and equipment;
- quests and objective progress;
- world unlocks and discoveries;
- collections;
- morning notes.

Use immutable transaction or ledger records for economy-sensitive operations where practical.

### 19.2 DataStore responsibilities

Use DataStore for small settings:

- theme;
- sound volume;
- motion preference;
- sleep targets;
- notification preferences;
- accessibility and game-control preferences;
- onboarding flags.

Do not store relational game or sleep history in DataStore.

### 19.3 Initial Room tables

```text
sleep_session
sleep_signal

night_outcome

expedition
expedition_path_node
expedition_reward

pet
pet_progression_event

inventory_stack
inventory_instance
inventory_transaction

equipment_slot

quest_progress
quest_objective_progress

world_unlock
world_discovery

collection_entry

morning_note
```

Every schema change requires a tested migration.

---

## 20. Content Architecture

Game content must not be hard-coded into screens.

```text
app/src/main/assets/game/content/
├── content_manifest.json
├── progression_rules_v1.json
├── pet_species.json
├── pet_forms.json
├── items.json
├── equipment_effects.json
├── quests.json
├── regions.json
├── scenes.json
├── expedition_nodes.json
├── loot_tables.json
└── dialogue/
```

### 20.1 Stable IDs

Every content entity has a permanent stable ID. Use these forms:

```text
species_<key>
form_<species-key>_<sequence>
region_<key>
scene_<region-key>_<sequence>
npc_<key>
item_<category>_<key>
quest_<key>
loot_<scope>_<key>
```

Rules:

- IDs are lowercase ASCII snake case.
- IDs are never derived from localized display text at runtime.
- Renaming display content must not change saved-data identity.
- Removing or replacing content requires an explicit migration or compatibility rule.

### 20.2 Independent versions

Persist three independent versions:

```text
databaseSchemaVersion
contentPackVersion
progressionRulesVersion
```

Every expedition result stores the content pack and progression rules versions used to create it. A later rebalance must not change an already-resolved or historical expedition.

---

## 21. Asset Architecture

### 21.1 Source art and runtime assets

Editable source art is separate from optimized APK assets.

```text
art/
├── source/
│   ├── pet/
│   ├── npc/
│   ├── regions/
│   ├── items/
│   └── ui/
└── export/
```

Only optimized runtime files ship in the APK:

```text
app/src/main/assets/game/
├── atlas/
│   ├── pet/
│   ├── npc/
│   ├── region/
│   └── vfx/
├── item/icon/
├── audio/
│   ├── music/
│   ├── ambience/
│   └── sfx/
└── content/
```

Compose application resources remain in standard Android resource folders:

```text
res/
├── drawable/
├── mipmap/
├── font/
├── raw/
├── values/
└── values-*/
```

### 21.2 Runtime filename convention

Use:

```text
<domain>_<entity>_<variant>_<purpose>.<extension>
```

Structural examples:

```text
pet_<species>_<form>_atlas.webp
pet_<species>_<form>_atlas.json

npc_<npc-key>_atlas.webp
npc_<npc-key>_atlas.json

region_<region-key>_bg_far.webp
region_<region-key>_bg_mid.webp
region_<region-key>_bg_near.webp
region_<region-key>_tiles.webp
region_<region-key>_props.webp

item_<category>_<item-key>_icon.webp
vfx_reward_<rarity>_atlas.webp

amb_<region-key>_<period>_loop.ogg
mus_<region-key>_<mode>_loop.ogg
sfx_<entity>_<action>_<sequence>.ogg
```

Filename rules:

- lowercase only;
- ASCII only;
- snake case;
- no spaces;
- no temporary suffixes such as `final_final2`;
- no display-name dependency;
- no image dimensions embedded in the name.

### 21.3 Pet animation contract

Every standard pet form implements:

```text
idle
blink
walk
run
jump_start
jump_loop
fall
land
interact
sleep
wake
celebrate
```

Optional animations:

```text
inspect
sit
equip
special
```

The renderer may horizontally flip a character. Separate left and right art is required only for meaningful visual asymmetry.

Animation ID pattern:

```text
pet.<species-key>.<form-key>.<animation>
```

### 21.4 NPC animation contract

Required:

```text
idle
talk
gesture
```

Optional:

```text
walk
work
special
```

Animation ID pattern:

```text
npc.<npc-key>.<animation>
```

### 21.5 Region asset contract

Every production region requires:

```text
background_far
background_mid
background_near
tileset
props_static
props_animated
foreground
interactables
collectibles
region_vfx
ambient_audio
music
expedition_node_illustrations
```

A region cannot be marked production-ready while a required asset class is missing.

### 21.6 UI asset contract

Game UI may use:

```text
pet portraits
item icons
rarity symbols
equipment slot icons
quest symbols
region emblems
discovery emblems
collection icons
reward effects
dialogue portraits
world-map markers
```

Sleep UI uses only what is functionally needed:

```text
functional icons
status indicators
sleep timeline graphics
alarm controls
journal visualizations
```

---

## 22. Design System and Art Direction

### 22.1 Compose design system

```text
ui/
├── theme/
│   ├── Color.kt
│   ├── Shape.kt
│   ├── Spacing.kt
│   ├── Typography.kt
│   └── SleepTheme.kt
└── components/
```

Centralize color, typography, shape, and spacing tokens. Do not scatter hard-coded design values through features.

Use:

- Material 3 typography and color tokens;
- one consistent application accent;
- clear visual hierarchy;
- restrained cards and elevation;
- tactile interaction feedback;
- Compose-native motion;
- consistent iconography;
- accessible layouts and controls.

Do not import web-specific architecture or styling systems such as React, Next.js, Tailwind, CSS Grid, Framer Motion, GSAP, Three.js, or web client directives.

### 22.2 Visual baseline

```text
DESIGN_VARIANCE    8
MOTION_INTENSITY   6
VISUAL_DENSITY     4
```

Interpretation:

- distinctive composition without reducing phone readability or touch ergonomics;
- fluid state changes and tactile feedback;
- comfortable daily-app density;
- no perpetual animation by default.

### 22.3 Visual modes

```text
DAY / GAME    rich and exploratory
WIND-DOWN     slower and quieter
SLEEP         near-static
MORNING       expressive
```

### 22.4 Art direction

Use soft nocturnal storybook-style painted 2D art, strong silhouettes, restrained texture, and atmospheric parallax.

Do not default to:

- generic neon-purple sleep-app styling;
- retro pixel art only because the game is 2D;
- overloaded mobile-gacha presentation.

The pet silhouette must remain recognizable at approximately icon size.

Functional sleep UI uses a restrained neutral palette:

```text
warm charcoal
deep neutral
off-white
muted warm gray
desaturated mineral accent
```

Regions may use distinct environmental palettes. Those palettes must not turn functional UI into an inconsistent rainbow.

---

## 23. Audio Architecture

Separate these audio groups:

```text
GAME MUSIC       short exploration loops
REGION AMBIENCE  wind, foliage, cave, water, and similar beds
GAME SFX         movement, collection, interaction, and UI
SLEEP SOUNDS     optional long-form soundscapes
MORNING REVEAL   short cinematic sting
```

Rules:

- Game music fades before Wind-down.
- Wind-down audio fades further.
- Sleep Mode never starts RPG music unexpectedly.
- Users can control relevant audio groups independently.

---

## 24. Android Platform and Background Architecture

### 24.1 Platform isolation

```text
Feature or domain
       ↓
application abstraction
       ↓
platform implementation
       ↓
Android OS or Google Play services
```

Platform capabilities include sleep signals, permissions, alarms, notifications, boot handling, background work, and audio.

### 24.2 Wake alarm

The wake alarm is user-critical and lives behind `platform/alarm/`.

Use an exact-alarm path only when the user explicitly enables a feature that requires precise wake timing and the platform permits it. Use inexact scheduling when exact timing is not required.

### 24.3 Wind-down reminder

Wind-down reminders are not second-critical. Use inexact scheduling or suitable scheduled background work.

### 24.4 Reward resolution

Reward resolution is durable but not time-critical. Use WorkManager and preserve idempotency.

### 24.5 Boot and package replacement

After device reboot or application replacement, the platform layer restores applicable state:

```text
Sleep API subscription
scheduled wind-down reminder
wake alarm
pending durable workers
```

### 24.6 Notification channels

Use separate channels:

```text
sleep_reminders
wake_alarm
morning_results
```

A user must be able to disable morning-result notifications without disabling the wake alarm.

---

## 25. Privacy and Data Control

The first release is local-first.

```text
No account required
No server required
No microphone
No raw audio
No wearable required
No advertising SDK required
No cloud progression dependency
```

Raw normalized sleep signals are temporary. Resolved sleep sessions are persistent until the user deletes them.

Data Management must provide:

```text
Export Data
Delete Sleep History
Reset Game Progress
Delete All Local Data
```

---

## 26. Accessibility

Accessibility is part of component and game architecture.

Functional UI requires:

- minimum 48 × 48 dp interactive targets;
- meaningful semantics for custom controls;
- content descriptions for icon-only actions;
- font scaling support;
- layout-direction support;
- non-color-only state and rarity indicators;
- appropriate contrast;
- reduced-motion support;
- Material 3 components and tokens where applicable.

Game settings require:

- reduced motion;
- screen shake off;
- larger controls;
- adjustable control opacity;
- left-handed control layout;
- independent audio controls.

---

## 27. Failure Behavior

Failure behavior must preserve the core loop whenever possible.

| Condition | Required behavior |
| --- | --- |
| Activity Recognition permission denied | Explain the limitation and use manual sleep sessions. |
| Sleep API unavailable | Use manual sleep sessions; do not block the game. |
| Missing automatic signals | Resolve from user anchors at lower confidence. |
| Process death during sleep | Restore the persisted session and expedition seed. |
| Process death during rewards | Roll back and retry the atomic transaction. |
| Duplicate platform callback | Deduplicate it. |
| Duplicate morning resolution request | Return the already-resolved result; do not grant rewards again. |
| Empty Journal | Show a useful first-night empty state. |
| Content reference invalid | Reject the candidate during validation. |

---

## 28. Test Architecture

### 28.1 Pure JVM and domain tests

```text
SleepResolverTest
ProgressionResolverTest
ExpeditionResolverTest
QuestEngineTest
LootResolverTest
PetLevelTest
PetStatGrowthTest
GameSimulationTest
CollisionSystemTest
ContentReferenceValidatorTest
```

Most game-rule confidence should come from deterministic pure Kotlin tests.

### 28.2 Persistence tests

```text
Room DAO tests
transaction tests
migration tests
reward idempotency tests
```

### 28.3 Compose tests

```text
WorldScreenTest
SleepScreenTest
MorningRevealTest
InventoryScreenTest
QuestScreenTest
```

Use semantics-based assertions and interactions.

### 28.4 Android integration tests

```text
permission granted
permission denied
Sleep API callback handling
boot re-registration
alarm scheduling
process death
worker retry
duplicate sleep callback
duplicate morning resolution
```

### 28.5 Critical end-to-end test

```text
Create fresh user and pet
       ↓
configure sleep
       ↓
Begin Sleep
       ↓
persist expedition seed
       ↓
inject controlled sleep signals
       ↓
kill and restart process
       ↓
wake and finalize session
       ↓
resolve expedition
       ↓
kill process during resolution
       ↓
retry
       ↓
assert exactly one XP grant
assert exactly one item reward set
assert exactly one quest progression update
       ↓
show Morning Reveal
       ↓
verify Journal record
       ↓
verify persistent world update
```

This test is the primary health check for the complete product architecture.

---

## 29. Content and Asset Validation

Continuous integration must reject:

```text
duplicate content ID
missing item reference
missing quest reference
missing sprite atlas
invalid animation name
unknown region ID
loot table referencing a missing item
evolution referencing a missing form
scene referencing a missing region
database migration failure
content schema mismatch
```

These checks must be deterministic and mechanical.

---

## 30. Build and Verification Authority

GitHub Actions is the primary build and verification authority.

```text
Exact Git commit
       ↓
GitHub Actions
       ↓
verification gates
       ↓
APK/AAB artifact and evidence
```

A candidate is not verified merely because generated Kotlin appears correct or because it compiles on one local machine.

### 30.1 Required CI path

Run applicable steps in this order:

```text
checkout exact commit
        ↓
validate repository and bootstrap
        ↓
validate content and asset references
        ↓
configure Android and Gradle environment
        ↓
compile
        ↓
unit tests
        ↓
Android lint and static architecture checks
        ↓
Compose/UI verification where applicable
        ↓
assembleDebug
        ↓
validate APK
        ↓
publish APK artifact
        ↓
publish evidence tied to the exact commit
```

Compilation alone is not feature correctness unless an approved task explicitly defines compilation as sufficient.

### 30.2 Verification categories

Fast deterministic gates:

- Kotlin compilation;
- unit tests;
- Android lint;
- static architecture checks;
- content and asset validation;
- APK assembly.

Device-level gates, when behavior depends on Android runtime semantics:

- Compose instrumentation;
- navigation;
- permissions;
- Sleep API integration;
- lifecycle and process death;
- alarm and notification behavior;
- boot restoration;
- background worker behavior;
- active game controls and rendering.

All evidence must be tied to the exact candidate commit.

---

## 31. Engineering Harness Boundary

The engineering harness controls repository work but is not application runtime code.

```text
task
  ↓
deterministic bootstrap
  ↓
verified manifest
  ↓
structured proposal
  ↓
proposal validation
  ↓
controlled repository mutation
  ↓
controller-owned verification
  ↓
deterministic evidence
```

Harness material belongs in repository infrastructure such as:

```text
tools/harness/
.github/workflows/
schemas/
verification scripts/
evidence/
```

Do not add harness screens, ViewModels, repositories, dependencies, or agent concepts to the APK.

### 31.1 Engineering authority order

When engineering sources disagree, use this order:

```text
exact Git commit
      >
approved task requirements
      >
verification evidence for that commit
      >
repository instructions
      >
applicable engineering skills
      >
this architecture file
      >
engineering assumptions
```

Assumptions never override a defined architecture rule.

---

## 32. First Complete Vertical Slice

The first complete playable proof contains:

```text
1 starter pet species
2 pet forms

1 persistent hub
2 connected production regions
3 NPCs

6 quests
├── 2 behavior quests
├── 2 world quests
└── 2 hybrid quests

12–20 items
4 equipment slots

1 complete nightly expedition graph
1 morning cinematic reconstruction system
1 active platform scene

sleep target
wind-down
phone-based sleep estimation
manual sleep fallback
wake alarm capability
sleep journal
full local persistence
```

The slice must prove the complete loop. It must not be a collection of disconnected screens.

---

## 33. Implementation Order

Build the product in this order.

| Slice | Required result |
| --- | --- |
| P1 — Product foundation | Navigation, theme, Hilt, Room, and DataStore. |
| P2 — Sleep session | Plan → Begin Sleep → Wake → Journal using manual anchors. |
| P3 — Phone sleep estimation | Sleep API, resolver, permission handling, and fallback. |
| P4 — Pet foundation | Persistent pet, XP, levels, stats, and affinity storage. |
| P5 — Night expedition | Seed, graph, deterministic resolver, and atomic rewards. |
| P6 — Morning reveal | Confirmation, finalization, reveal, and journey reconstruction. |
| P7 — Inventory and equipment | Items, slots, effects, transactions, and UI. |
| P8 — Quest engine | Typed behavior, world, and hybrid objectives. |
| P9 — World hub | Persistent hub and visible state changes. |
| P10 — 2D runtime | Input, movement, collision, camera, simulation, and rendering. |
| P11 — First platform region | One complete active region vertical slice. |
| P12 — Evolution | Forms, level gates, affinity unlocks, and player choice. |
| P13 — Journal and trends | Historical resolved sessions and behavior views. |
| P14 — Audio and visual polish | Production assets, motion, audio, and morning presentation. |
| P15 — Android hardening | Boot, process death, alarms, workers, permissions, and accessibility. |
| P16 — Release architecture | Signing boundary, release variant, AAB, metadata, and production verification. |

Each slice must end with the applicable deterministic and device-level verification gates.

---

## 34. Delivery Checkpoints

### 34.1 Android build skeleton

Create:

- Gradle project and `:app`;
- manifest and application class;
- `MainActivity` and minimal Compose root;
- Material 3 theme;
- GitHub Actions build;
- debug APK artifact upload.

Acceptance:

```text
exact candidate commit
       ↓
CI
       ↓
lint passes
tests pass
assembleDebug passes
       ↓
valid debug APK produced
```

Do not add business functionality at this checkpoint.

### 34.2 UI architecture

Establish:

- navigation boundary;
- Route/Screen convention;
- design tokens and core components;
- preview and UI-test foundation;
- loading, content, empty, error, permission, and capability states.

### 34.3 Application and domain architecture

Establish:

- ViewModels and immutable UI state;
- `StateFlow` and one-shot effect boundary;
- repository interfaces;
- Hilt application graph;
- domain packages for defined business logic.

Prove the architecture with a real product feature, not placeholder abstractions.

### 34.4 Persistence and content

Establish:

- Room schema and migrations;
- DataStore settings;
- content manifest and stable IDs;
- versioned progression rules;
- content and asset validators.

### 34.5 Sleep and platform capability

Establish:

- manual session path first;
- Sleep API integration;
- resolver and confidence;
- permission-denied and unavailable paths;
- alarm, reminder, worker, and boot behavior.

### 34.6 Production pipeline

After product behavior is verified, add:

- release variant;
- signing boundary;
- release verification;
- APK/AAB artifacts;
- version metadata;
- release evidence.

Secrets must remain outside the repository.

---

## 35. Final Architecture Contract

```text
Android only
Phone first and portrait first
Single activity
Kotlin
Jetpack Compose
Material 3

Feature-oriented modular monolith
Required domain layer for real business logic
Hilt constructor injection
Room for structured data
DataStore for small settings
WorkManager for durable, non-time-critical work

Route → state-driven Screen
ViewModel owns screen state
StateFlow owns persistent render state
Channel owns single-consumer effects
Compose or @Stable holder owns local UI state

Repositories isolate data and platform sources
Android APIs remain behind platform boundaries
Game simulation remains separate from Compose and Room

One persistent pet
Non-punitive sleep-linked progression
No combat or HP in the initial product
Deterministic overnight expedition
Atomic and idempotent rewards
Short daytime platform scenes
Persistent world growth

Stable content IDs
Display names never used as identifiers
Versioned content and progression rules
Source art separated from runtime assets
Validated asset and animation contracts

Phone-derived sleep estimation
Manual fallback always playable
No medical precision claims
Local-first privacy

GitHub Actions verifies an exact commit
APK/AAB has no dependency on the engineering harness

No speculative backend
No speculative wearable integration
No microphone sleep analysis
No overnight live rendering
No premature Gradle module expansion
No invention when a required decision is undefined
```
