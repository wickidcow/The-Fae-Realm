# The Fae Realm

**The Fae Realm** is a server-side fantasy world generator for **Paper 26.2 / Java 25**. It creates a separate floating-island realm entirely from a seed as players explore; it is not a pre-generated map download and does not require Forge, Fabric, or NeoForge on the client.

The project began as a Paper-port experiment from a fork of The Aether. The active Paper implementation now has its own Fae Realm identity, generator, structures, resources, dungeons, progression, and runtime validation. The original NeoForge branch remains historical/reference source. No original Aether all-rights-reserved assets are bundled in the Paper plugin.

## Generator model

The world folder defaults to `fae_realm`. New chunks are generated deterministically from the realm seed, so the same seed, settings, and generator version recreate the same terrain. Minecraft preserves already-generated chunks normally; generator updates apply to newly explored chunks unless the realm is intentionally regenerated.

Current generator revision: **v5**.

Each world also stores `fae-realm-generator.yml` in its world folder with its seed, first/current generator version, preset, major terrain settings, and plugin version. If an existing realm is opened with a newer generator revision, the plugin warns that existing chunks remain unchanged while new exploration will use the new terrain rules.

## Terrain

- Large floating continents and satellite islands
- Broad macro-noise producing dense archipelagos and quieter open-sky regions
- Seeded coastline warping and ridge/elevation variation
- Hollow island caverns with configurable frequency
- Biome-specific hanging underside formations
- Open void beneath the islands
- Optional decorative cloud shelves at a configurable altitude
- Guaranteed starter island at the realm origin
- Configurable island density and vertical relief

### Terrain presets

`worldgen.preset` provides four starting personalities:

- **balanced** — intended default
- **ethereal** — sparser islands, stronger height differences, fewer caverns
- **lush** — denser archipelagos with gentler relief
- **wild** — stronger vertical relief and more caverns

The preset values can be overridden individually with `island-density`, `vertical-scale`, `cave-density`, and `cloud-level`.

## Fae regions

Five server-side region identities control terrain palette, vegetation, structures, resources, and micro-features while mapping to vanilla client-visible biome data:

- **Golden Meadows**
- **Crystal Woods**
- **Mist Gardens**
- **Ancient Fae Forest**
- **Sky Highlands**

Use `/fae biome` while inside the realm to report the current Fae region.

## Decorations and micro-features

Ordinary exploration contains smaller deterministic landmarks in addition to major structures:

- Golden Meadows: flower circles and glowing Sun Pools
- Crystal Woods: crystal spires and amethyst outcrops
- Mist Gardens: mushroom clusters and pale-moss pockets
- Ancient Fae Forest: fallen ancient logs, moss, and fern details
- Sky Highlands: standing-stone formations
- Region-specific trees and surface vegetation
- Sparse small ruins
- Hanging stone, crystal, moss, and light formations beneath islands

## Realm resources

The Fae Realm has its own deterministic biome-weighted underground resource pass:

- Golden Meadows favor gold and copper
- Crystal Woods favor amethyst and lapis, with rare diamonds
- Mist Gardens favor lapis and iron with rare glowstone pockets
- Ancient Fae Forest favors emeralds with rare diamonds
- Sky Highlands favors iron and copper with rarer gold

Resource generation can be disabled independently from structures and decorations.

## Procedural structures

The Fae Realm owns its core structure layer and does not need another plugin to function.

- **Golden Meadows:** Sun Court Shrines
- **Crystal Woods:** Crystal Temples
- **Mist Gardens:** Mist Sanctums
- **Ancient Fae Forest:** Ancient Watchtowers
- **Sky Highlands:** Sky Gates
- **All regions:** rare Dungeon Gates

Structure candidates are seed-derived, terrain-tested, and placed through Paper `LimitedRegion` generation without force-loading neighboring chunks.

## Fae Vault dungeons

Rare Dungeon Gates descend into chunk-local **Fae Vaults**. The current dungeon framework includes:

- Descending gate passage
- Main hall
- Spiral descent
- Lower relic vault
- Biome-specific block palettes
- Three deterministic room plans: **Hall of Echoes**, **Twin Reliquaries**, and **Faerie Crossing**
- Loot-ready generated barrels
- Deterministic first-open loot while protecting player-placed barrels from being converted into loot containers

The first vault system deliberately stays inside one chunk for async-generation safety. Larger multi-chunk castles and dungeons can later use queued placement or a dedicated BetterStructures Fae content pack.

## Progression

The first vanilla-client-safe progression hooks are included without requiring ItemsAdder or a resource pack:

- **Fae Essence** — vanilla amethyst-shard-backed custom item with persistent plugin identity
- **Fae Vault Key** — vanilla trial-key-backed custom item with persistent plugin identity
- Fae-generated resource blocks can release Fae Essence when mined
- Fae Vault loot can contain Essence and rare Vault Keys
- Essence drop chance is configurable

These items provide stable hooks for future recipes, bosses, keys, advancement systems, and optional custom-model integrations.

## BetterStructures compatibility

BetterStructures is optional; The Fae Realm does not require it.

The BetterStructures fork now exposes `ValidWorldsConfig.setWorldValidity(...)`, allowing The Fae Realm to mark `fae_realm` invalid **before BetterStructures runs its new-chunk scanners**. Generic BetterStructures structures therefore stay out of the realm by default without wasting surface/underground/sky/dungeon scan work.

Older BetterStructures builds remain supported through a cancellable placement-event fallback. A future dedicated Fae Realm structure pack can explicitly opt back in for large schematic-based landmarks.

## Portal and void behavior

Build a 4x5 Glowstone frame and use a water bucket inside it to activate the portal. Entering its water-filled interior transports the player to the Fae Realm and remembers a return location.

By default, falling beneath the floating realm returns the player to a normal world rather than killing them. This is configurable as:

- `return-to-overworld`
- `fae-spawn`
- `death`

## Commands

- `/fae` — travel to the Fae Realm
- `/faerealm` — alias for `/fae`
- `/aether` — legacy compatibility alias
- `/fae return` — return to a normal world
- `/fae info` — show version, seed, generator revision/preset, terrain settings, and BetterStructures status
- `/fae biome` — report the current Fae region
- `/fae reload` — reload non-generator configuration (admin)

## Configuration

The major generator layers are independent:

```yaml
worldgen:
  version: 5
  preset: balanced
  island-density: 1.0
  vertical-scale: 1.0
  cave-density: 1.0
  cloud-level: 74
  clouds: true
  decorations: true
  structures: true
  resources: true

progression:
  enabled: true
  essence-drop-chance: 0.22
```

World-generator settings are captured when the world is loaded and require a restart. They affect newly generated chunks; existing chunks are never automatically rewritten.

## Validation

CI now has two separate gates:

1. **Build The Fae Realm** — Java 25 compilation and finished-JAR content verification.
2. **Paper 26.2 Runtime Smoke** — builds the candidate, downloads the current stable Paper 26.2 server, boots it twice, verifies `fae_realm` is created, verifies generator metadata is written, preserves the generated world between boots, and requires clean server shutdown.

## Build target

- Paper 26.2
- Java 25
- Output: `TheFaeRealm-0.1.0-SNAPSHOT.jar`
- BetterStructures: optional soft dependency

## Development status

The current branch is pre-release. It already contains the core pieces of a usable fantasy generator—macro terrain, five regional identities, biome-specific vegetation/features, resources, procedural structures, vault dungeons, portal travel, void handling, progression hooks, generator metadata, configuration presets, and BetterStructures coexistence. Remaining work before calling `0.1.0` production-ready centers on real-Paper runtime validation, richer ecology, expanded large structures/dungeons, admin locate/diagnostic tooling, performance profiling, and additional regression tests.
