# The Fae Realm

**The Fae Realm** is a server-side fantasy world generator for **Paper 26.2 / Java 25**. It creates a separate floating-island realm entirely from a seed as players explore; it is not a pre-generated map download and does not require Forge, Fabric, or NeoForge on the client.

The project began as a Paper-port experiment from a fork of The Aether. The active Paper implementation now has its own Fae Realm identity, generator, structures, resources, dungeons, and gameplay systems. The original NeoForge branch remains only as historical/reference source. No original Aether all-rights-reserved assets are bundled in the Paper plugin.

## Generator model

The world folder defaults to `fae_realm`. New chunks are generated deterministically from the realm seed, so the same seed and generator version recreate the same terrain. Minecraft preserves already-generated chunks normally; generator updates apply to newly explored chunks unless the realm is intentionally regenerated.

Current generator revision: **v4**.

### Terrain

- Large floating continents
- Smaller satellite islands
- Seeded edge warping and elevation variation
- Hollow island caverns
- Open void beneath the islands
- Optional decorative cloud shelves
- Guaranteed starter island at the realm origin
- Configurable island density

### Fae regions

Five server-side region identities control terrain palette, vegetation, structures, and resources while mapping to vanilla client-visible biome data:

- **Golden Meadows**
- **Crystal Woods**
- **Mist Gardens**
- **Ancient Fae Forest**
- **Sky Highlands**

### Decorations and resources

- Region-specific trees and foliage
- Flowers, moss, crystal outcrops, and small ruins
- Biome-weighted underground resource generation
- Golden Meadows favor gold and copper
- Crystal Woods favor amethyst and lapis, with rare diamonds
- Mist Gardens favor lapis and iron with rare glowstone pockets
- Ancient Fae Forest favors emeralds with rare diamonds
- Sky Highlands favors iron and copper with rarer gold

## Procedural structures

The Fae Realm owns its core structure layer and does not need another plugin to function.

- **Golden Meadows:** Sun Court Shrines
- **Crystal Woods:** Crystal Temples
- **Mist Gardens:** Mist Sanctums
- **Ancient Fae Forest:** Ancient Watchtowers
- **Sky Highlands:** Sky Gates
- **All regions:** rare Dungeon Gates

Structure candidates are deterministic, terrain-tested, and placed through Paper `LimitedRegion` generation without force-loading neighboring chunks.

## Fae Vault dungeons

Rare Dungeon Gates descend into chunk-local **Fae Vaults**. The first dungeon framework includes:

- Descending gate passage
- Main hall
- Lower relic vault
- Biome-specific block palettes
- Three deterministic room plans: Hall of Echoes, Twin Reliquaries, and Faerie Crossing
- Loot-ready generated barrels
- Deterministic first-open loot so generated vault storage has gameplay value without touching player-placed barrels

The dungeon system is intentionally chunk-local for the first production-safe implementation. Larger multi-chunk dungeons can later use a queued placement system or a dedicated BetterStructures content pack.

## BetterStructures compatibility

BetterStructures is optional; The Fae Realm does not require it.

By default, generic BetterStructures packs are kept out of `fae_realm` so the realm retains its own structure identity. A compatible BetterStructures build can expose `ValidWorldsConfig.setWorldValidity(...)`, allowing The Fae Realm to mark its world invalid **before BetterStructures runs its new-chunk scanners**. Older BetterStructures builds use a cancellable placement-event fallback instead.

A future dedicated Fae Realm BetterStructures pack can be explicitly enabled for large castles, cities, or elaborate schematic dungeons without making BetterStructures mandatory.

## Portal and void behavior

Build a 4x5 Glowstone frame and use a water bucket inside it to activate the portal. Entering the water-filled interior transports the player to Fae Realm.

By default, falling out of the floating realm returns the player to the normal world instead of killing them. This is configurable as:

- `return-to-overworld`
- `fae-spawn`
- `death`

## Commands

- `/fae` — travel to the Fae Realm
- `/faerealm` — alias for `/fae`
- `/aether` — legacy compatibility alias
- `/fae return` — return to the normal world
- `/fae info` — show generator/plugin and BetterStructures integration information
- `/fae reload` — reload non-generator configuration (admin)

## Configuration

Generator layers can be independently controlled:

```yaml
worldgen:
  version: 4
  island-density: 1.0
  clouds: true
  decorations: true
  structures: true
  resources: true
```

Generator/world-name changes require a restart and only affect newly generated chunks unless the realm folder is deliberately regenerated.

## Build target

- Paper 26.2
- Java 25
- Output: `TheFaeRealm-0.1.0-SNAPSHOT.jar`
- BetterStructures: optional soft dependency

## Development status

The current branch is pre-release, but it now contains a usable generator foundation: terrain, five regional identities, vegetation, resources, structures, dungeon vaults, portal travel, configurable void behavior, and BetterStructures coexistence. The next major milestones are richer terrain profiles, expanded dungeon progression, custom mob ecology, structure packs, generator diagnostics, and runtime/performance smoke testing.
