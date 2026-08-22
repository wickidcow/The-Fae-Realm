# The Fae Realm

**The Fae Realm** is a server-side fantasy world generator for **Paper 26.2 / Java 25**. It creates a separate floating-island realm from a seed as players explore. It is not a pre-generated map and does not require Forge, Fabric, or NeoForge on the client.

The active Paper implementation has its own generator, regional identities, resources, structures, dungeons, progression hooks, portals, configuration and runtime validation. The original NeoForge source remains only as historical/reference material; the Paper plugin does not bundle the original Aether project's protected game assets.

## Generator v6

Generator revision **v6** is code-owned and recorded in each realm's `fae-realm-generator.yml`. It is deliberately not a user-editable config value. If a later generator revision opens an existing realm, old chunks remain untouched and the plugin warns that newly explored chunks will use the newer terrain rules.

The same **seed + generator revision + generator settings** produces deterministic new terrain.

### Terrain engine

- Large floating continents and smaller satellite islands
- Broad macro-noise that forms dense archipelagos and quieter open-sky zones
- Irregular coastlines, ridges and configurable vertical relief
- Internal caverns and open void beneath islands
- Region-specific hanging underside formations
- Optional decorative cloud shelves
- Guaranteed safe starter continent at the origin
- Five deterministic island profiles:
  - **Balanced** — general-purpose floating continent
  - **Plateau** — broad, flatter buildable landmasses
  - **Spire** — narrower, taller dramatic islands
  - **Terraced** — stepped fantasy mesas
  - **Hollow** — thicker islands with stronger cavern identity

Terrain profiles can be disabled to use only the balanced shape.

### Terrain presets

`worldgen.preset` provides four starting personalities:

- **balanced** — default all-purpose realm
- **ethereal** — sparser islands and stronger height differences
- **lush** — denser archipelagos with gentler relief
- **wild** — stronger vertical relief and more caverns

Preset terrain can be overridden with `island-density`, `vertical-scale`, `cave-density`, and `cloud-level`.

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

`decoration-density` scales trees, micro-landmarks, outcrops and small ruins without changing the seed.

## Realm resources

A deterministic underground resource pass gives regions different mining identities:

- Golden Meadows: gold, copper and rare emerald
- Crystal Woods: amethyst, lapis and rare diamond
- Mist Gardens: lapis, iron and rare glowstone pockets
- Ancient Fae Forest: emerald, coal and rare diamond
- Sky Highlands: iron, copper and rarer gold

`resource-density` independently scales generated resource attempts. The entire resource layer can also be disabled.

## Procedural structures

The Fae Realm owns its core structure layer and does not require an external structure plugin:

- **Golden Meadows:** Sun Court Shrines
- **Crystal Woods:** Crystal Temples
- **Mist Gardens:** Mist Sanctums
- **Ancient Fae Forest:** Ancient Watchtowers
- **Sky Highlands:** Sky Gates
- **All regions:** rare Dungeon Gates leading into Fae Vaults

Structure candidates are seed-derived, terrain-tested and placed through Paper `LimitedRegion` generation without force-loading neighboring chunks.

Server owners can tune:

- `structure-spacing-chunks` — size of each major-structure placement cell
- `dungeon-chance` — chance a valid major site becomes a Fae Vault instead of its regional landmark

## Fae Vaults

Rare Dungeon Gates descend into chunk-local Fae Vaults designed to remain safe during asynchronous generation:

- descending entrance passage
- main hall
- lower relic vault
- side chambers and lighting
- biome-specific palettes
- deterministic room plans:
  - **Hall of Echoes**
  - **Twin Reliquaries**
  - **Faerie Crossing**
- generated loot barrels with deterministic first-open loot

Player-placed barrels are not converted into generated dungeon loot.

## Progression hooks

The first vanilla-client-safe progression layer includes:

- **Fae Essence** — amethyst-shard-backed custom item with plugin identity
- **Fae Vault Key** — trial-key-backed custom item with plugin identity
- Fae-generated resource blocks can release Essence when mined
- Fae Vault loot can contain Essence and rare Vault Keys

These provide stable hooks for future recipes, bosses, advancements and optional custom-model integrations without requiring a resource pack today.

## BetterStructures compatibility

BetterStructures is optional. The Fae Realm works standalone.

Compatible BetterStructures builds expose `ValidWorldsConfig.setWorldValidity(...)`, allowing The Fae Realm to exclude `fae_realm` before BetterStructures runs its surface, underground, sky or dungeon scans. Generic BetterStructures packs therefore stay out of the realm by default without wasted scan work.

Older BetterStructures builds use a cancellable placement-event fallback. A dedicated Fae Realm BetterStructures content pack can be allowed later for very large schematic-based landmarks.

## Portal and realm behavior

Build a **4x5 Glowstone frame** and use a water bucket inside it. The plugin fills the 2x3 interior and turns it into the realm portal. Walking into the water sends the player to Fae Realm and stores a persistent return point.

The generated arrival island and return portal are initialized **once**. On later boots the plugin preserves player changes around realm spawn instead of reconstructing the platform.

Falling into the void can be configured as:

- `return-to-overworld` (default)
- `fae-spawn`
- `death`

PvP, daylight cycle, weather cycle and mob spawning can be controlled independently for the realm.

## Commands

- `/fae` — enter the Fae Realm
- `/faerealm` — alias
- `/aether` — legacy development alias
- `/fae return` — return to the saved portal location
- `/fae biome` — show current Fae region
- `/fae info` — show seed, generator v6, preset, terrain/layer settings and BetterStructures status
- `/fae help` — command help
- `/fae locate <region>` — admin deterministic region locator; does not generate chunks
- `/fae reload` — admin reload for non-generator settings

`/fae info`, `/fae help`, `/fae locate`, and `/fae reload` are console-safe where applicable.

## Configuration

Core generator controls:

```yaml
worldgen:
  preset: balanced
  island-density: 1.0
  vertical-scale: 1.0
  cave-density: 1.0
  terrain-profiles: true
  cloud-level: 74
  decoration-density: 1.0
  resource-density: 1.0
  structure-spacing-chunks: 10
  dungeon-chance: 0.12
  clouds: true
  decorations: true
  structures: true
  resources: true
```

Generator settings are captured when the world is loaded and require a restart. They affect newly generated chunks only; existing chunks are never automatically rewritten.

## Generator provenance

Every Fae Realm stores `fae-realm-generator.yml` in the world folder with:

- first and current generator revision
- original seed
- current preset
- terrain density / vertical / cavern values
- terrain-profile state
- decoration and resource density
- structure spacing and dungeon chance
- plugin version and world name

This makes mixed-generation worlds diagnosable after upgrades.

## Validation

CI has two release gates:

1. **Build The Fae Realm** — Java 25 compilation, finished-JAR content checks, and an unarchived raw `.jar` Actions artifact named **TheFaeRealm-raw-jar**.
2. **Paper 26.2 Runtime Smoke** — downloads the current stable Paper 26.2 server, boots the candidate twice, verifies realm storage and v6 metadata, requires clean shutdown, and confirms the arrival area is initialized only on the first boot.

## Build target

- Paper 26.2
- Java 25
- BetterStructures: optional soft dependency
- Client mods: not required
- Output: raw `TheFaeRealm-<version>.jar`

The Paper implementation is intended to be a standalone fantasy generator in the same broad category as configurable world-generation plugins: install the JAR, configure the realm personality, restart, and explore newly generated Fae terrain indefinitely.
