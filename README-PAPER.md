# The Fae Realm

**The Fae Realm** is a server-side fantasy world generator for **Paper 26.2 / Java 25**. It creates a separate floating-island realm from a seed as players explore. It is not a pre-generated map and does not require Forge, Fabric, or NeoForge on the client.

The Paper implementation has its own generator, regional identities, resources, structures, dungeons, progression hooks, portals, configuration and runtime validation. The original NeoForge source remains only as historical/reference material; the Paper plugin does not bundle the original Aether project's protected game assets.

## Generator v6

Generator revision **v6** is code-owned and recorded in each realm's `fae-realm-generator.yml`. It is deliberately not a user-editable config value. Existing chunks are never rewritten automatically; when generator revisions or resolved settings change, the plugin warns that newly explored chunks will use the newer rules.

The same **seed + generator revision + resolved generator settings** produces deterministic new terrain.

### Terrain engine

- Large floating continents and smaller satellite islands
- Broad macro-noise for dense archipelagos and quieter open-sky zones
- Irregular coastlines, ridges and configurable vertical relief
- Internal caverns and open void beneath islands
- Region-specific hanging underside formations
- Optional decorative cloud shelves
- Guaranteed safe starter continent at the origin
- Five deterministic island profiles: **Balanced, Plateau, Spire, Terraced, Hollow**

### Terrain presets

`worldgen.preset` provides four starting personalities:

- **balanced** — default all-purpose realm
- **ethereal** — sparser islands and stronger height differences
- **lush** — denser archipelagos with gentler relief
- **wild** — stronger vertical relief and more caverns

`island-density`, `vertical-scale`, `cave-density`, and `cloud-level` are `null` by default, which means **inherit the selected preset**. Set a number only when you want to override that individual preset value.

## Fae regions

Five server-side region identities control surface palettes, vegetation, structures, resources and small landmarks while mapping to vanilla client-visible biome data:

- **Golden Meadows**
- **Crystal Woods**
- **Mist Gardens**
- **Ancient Fae Forest**
- **Sky Highlands**

Use `/fae biome` while in the realm to inspect your current region. Admins can use `/fae locate <region>` to search the deterministic region field without loading or generating chunks.

## Ecology and landmarks

Ordinary exploration includes deterministic biome-aware decoration:

- Golden Meadows: flower circles and glowing Sun Pools
- Crystal Woods: cherry growth, crystal spires and amethyst outcrops
- Mist Gardens: pale vegetation and mushroom clusters
- Ancient Fae Forest: dense trees, moss, ferns and fallen ancient logs
- Sky Highlands: birch growth and standing-stone formations
- Sparse small ruins
- Hanging stone, crystal, moss and light formations under islands

`decoration-density` scales trees, micro-landmarks, outcrops, ruins and underside formations. `resource-density` independently scales generated resource attempts.

## Realm resources

- Golden Meadows: gold, copper and rare emerald
- Crystal Woods: amethyst, lapis and rare diamond
- Mist Gardens: lapis, iron and rare glowstone pockets
- Ancient Fae Forest: emerald, coal and rare diamond
- Sky Highlands: iron, copper and rarer gold

## Procedural structures

The Fae Realm owns its core structure layer and does not require an external structure plugin:

- **Golden Meadows:** Sun Court Shrines
- **Crystal Woods:** Crystal Temples
- **Mist Gardens:** Mist Sanctums
- **Ancient Fae Forest:** Ancient Watchtowers
- **Sky Highlands:** Sky Gates
- **All regions:** rare Dungeon Gates leading into Fae Vaults

Structure candidates are seed-derived, terrain-tested and placed through Paper `LimitedRegion` generation without force-loading neighboring chunks. `structure-spacing-chunks` controls major-structure spacing and `dungeon-chance` controls how often a valid site becomes a vault.

## Fae Vaults

Rare Dungeon Gates descend into chunk-local Fae Vaults with:

- descending entrance passage
- main hall and lower relic vault
- side chambers and biome-specific palettes
- deterministic room plans: **Hall of Echoes, Twin Reliquaries, Faerie Crossing**
- generated loot barrels with deterministic first-open loot

Vault barrels are explicitly tagged during generation. Player-placed barrels and containers created by other plugins are never silently converted into Fae loot containers.

## Progression hooks

- **Fae Essence** — amethyst-shard-backed custom item with stable `thefaerealm:` identity
- **Fae Vault Key** — trial-key-backed custom item with stable `thefaerealm:` identity
- Naturally generated Fae resource blocks can release Essence when mined
- Player-placed Essence-source blocks are tracked per chunk and do not qualify for repeat Essence rolls
- Fae Vault loot can contain Essence and rare Vault Keys

These are vanilla-client-safe hooks for future recipes, bosses, advancements and optional custom-model integrations.

## BetterStructures compatibility

BetterStructures is optional. Compatible builds expose `ValidWorldsConfig.setWorldValidity(...)`, allowing The Fae Realm to exclude `fae_realm` before BetterStructures runs its chunk scanners. Generic BetterStructures packs stay out of the realm by default. If generic structures are later enabled, The Fae Realm actively restores the world's BetterStructures validity.

Older BetterStructures builds use a cancellable placement-event fallback. A dedicated Fae Realm BetterStructures content pack can be allowed later for very large schematic-based landmarks.

## Portal and realm behavior

Build a **4x5 Glowstone frame** and use a water bucket inside it. The plugin fills the 2x3 interior and turns it into the realm portal. Portal travel requires the complete frame and water interior, and the return point is persisted on the player.

The generated arrival island and return portal are initialized **once**. `world.spawn-y` is therefore a first-creation setting only; after initialization, Minecraft's saved Fae world spawn is authoritative and later config edits do not rebuild the platform or move the arrival point.

Falling into the void can be configured as `return-to-overworld`, `fae-spawn`, or `death`. The default return mode uses the player's saved portal return point when available.

## Commands

- `/fae` — enter the Fae Realm
- `/faerealm` — alias
- `/aether` — legacy development alias
- `/fae return` — return to the saved portal location
- `/fae biome` — show current Fae region
- `/fae info` — show seed, generator v6, preset and generator-layer settings
- `/fae help` — command help
- `/fae locate <region>` — admin deterministic region locator; does not generate chunks
- `/fae reload` — admin reload for non-generator settings

## Configuration

```yaml
worldgen:
  preset: balanced
  island-density: null
  vertical-scale: null
  cave-density: null
  cloud-level: null
  terrain-profiles: true
  decoration-density: 1.0
  resource-density: 1.0
  structure-spacing-chunks: 10
  dungeon-chance: 0.12
  clouds: true
  decorations: true
  structures: true
  resources: true
```

Generator settings are captured when the world is loaded and require a restart. They affect newly generated chunks only.

## Generator provenance

Every Fae Realm stores `fae-realm-generator.yml` with:

- first and current generator revision
- original seed
- first resolved generator settings and fingerprint
- current resolved generator settings and fingerprint
- preset, terrain values, decoration/resource density
- structure spacing and dungeon chance
- plugin version and world name

This makes mixed-generation worlds diagnosable after upgrades or config changes.

## Validation and artifacts

CI has two release gates:

1. **Build The Fae Realm** — Java 25 compilation, finished-JAR content checks, and an unarchived raw `.jar` Actions artifact.
2. **Paper 26.2 Runtime Smoke** — downloads stable Paper 26.2, boots the candidate twice, verifies realm storage and v6 metadata/provenance, exercises `/fae info` and `/fae locate`, requires clean shutdown, and confirms the arrival area initializes only once.

## Build target

- Paper 26.2
- Java 25
- BetterStructures: optional soft dependency
- Client mods: not required
- Output: raw `TheFaeRealm-<version>.jar`

The Paper implementation is intended to be a standalone configurable fantasy generator: install the JAR, choose the realm personality, restart, and explore newly generated Fae terrain indefinitely.
