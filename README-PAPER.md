# The Fae Realm

**The Fae Realm** is a server-side fantasy world generator for **Paper 26.2 / Java 25**. It creates a separate bright floating-island realm from a seed as players explore. It is not a pre-generated map and does not require Forge, Fabric, NeoForge, Iris, or a client resource pack.

The Paper implementation owns its terrain, ecology, regional identities, resources, structures, dungeons, progression hooks, portals, configuration and runtime validation. Iris/Terra-style ideas such as layered noise fields, domain warping and deterministic object grids are implemented independently in this plugin; Iris is not a dependency.

## Generator v9 — Living Islands

Generator revision **v9** keeps the open Radiant End layout while making ordinary islands visibly alive. Terrain-aware surface lookup now finds valid ground beneath flowers, moss, tree canopies and nearby feature blocks, preventing later ecology passes from silently rejecting good placement sites.

Trees, understory clusters and small biome landmarks are substantially more common. Golden Meadows remain more open than Ancient Fae Forests, but every region now has enough canopy and ground cover to read as a living Fae biome instead of an empty grass platform.

The realm deliberately remains a Bukkit **NORMAL** environment. The custom generator provides the void/floating-island geography while vanilla clients keep the normal sky and sun.

Existing chunks are never rewritten automatically. Upgrading an existing realm changes only newly generated chunks. For the cleanest all-v9 terrain, create a fresh realm/world folder or test beyond the pregenerated border.

### Terrain

- Large floating continents and satellite islands
- Broad macro-noise creating archipelagos and wide open-sky zones
- Irregular coastlines, ridges, caverns and vertical relief
- Five island profiles: **Balanced, Plateau, Spire, Terraced, Hollow**
- **Radiant End** preset with lower island density and greater height separation
- Open void below the islands
- Decorative cloud shelves
- Guaranteed safe starter continent at the origin

### Presets

`worldgen.preset` supports:

- **radiant_end** — default; bright End-like openness with living Fae islands
- **balanced** — general-purpose original layout
- **ethereal** — sparse islands and stronger height differences
- **lush** — dense archipelagos and gentler relief
- **wild** — stronger vertical relief and more caverns

`worldgen.radiant-end-layout: true` can also apply the open-sky spacing bias to the older presets.

## Multi-noise ecology

Generator v9 layers an additional ecology field over the biome flora. Independent fertility, moisture and magical-energy noise fields are domain-warped into broad coherent growth regions instead of placing every plant with unrelated random rolls.

The resulting ecology bands are:

- **Sparse**
- **Meadow**
- **Lush**
- **Ancient**
- **Enchanted**

These control additional ground growth, flowers, moss, root knots and hanging vegetation. `worldgen.growth-density` controls the strength of this pass independently from normal decoration density.

The original biome-aware trees remain and include Sun Crown trees, Crystal trees, Mist Willows, twisted Ancient trees and windswept birch growth.

## Fae regions

Five deterministic region identities control surface palettes, vegetation, structures, resources and landmarks:

- **Golden Meadows**
- **Crystal Woods**
- **Mist Gardens**
- **Ancient Fae Forest**
- **Sky Highlands**

Use `/fae biome` in the realm to inspect the current region. Admins can use `/fae locate <region>` without generating chunks.

## Rare large landmarks

Generator v9 uses a separate long-range landmark grid so major sights remain rare and memorable instead of appearing in every few chunks.

Depending on region, exploration can find:

- enormous rooted Great Fae Trees
- giant cherry/crystal trees
- Crystal Crowns
- glowing Hanging Gardens and waterfall spillways
- quartz/calcite Sky Arches
- broken Sun Causeways

`worldgen.landmark-spacing-chunks` controls their grid spacing and defaults to `28`.

Normal smaller landmarks continue to generate: flower circles, Sun Pools, crystal spires, mushroom clusters, fallen ancient logs, standing stones, small ruins, underside formations and biome-specific outcrops.

## Procedural structures and vaults

The Fae Realm owns its structure layer and does not require BetterStructures:

- Golden Meadows — Sun Court Shrines
- Crystal Woods — Crystal Temples
- Mist Gardens — Mist Sanctums
- Ancient Fae Forest — Ancient Watchtowers
- Sky Highlands — Sky Gates
- All regions — rare Dungeon Gates leading to Fae Vaults

Major structures, large landmarks and ecology are deterministic and use Paper `LimitedRegion` generation without force-loading neighboring chunks.

## Multiverse-Core setup

**Yes — when Multiverse is the plugin importing or creating the realm, set TheFaeRealm as its custom generator.** Otherwise Multiverse can load the folder without knowing which generator must create future chunks.

After installing TheFaeRealm and starting the server, first verify it is visible:

```text
/mv generators
```

You should see `TheFaeRealm` in the list.

For an existing Fae Realm world:

```text
/mv import fae_realm normal --generator TheFaeRealm
```

For a brand-new realm created by Multiverse:

```text
/mv create fae_realm normal --generator TheFaeRealm
```

Use **`normal`**, not `the_end`. The `NORMAL` environment is intentional: it supplies the normal bright sun/sky while TheFaeRealm replaces terrain generation with floating Fae continents.

If TheFaeRealm itself creates `fae_realm`, it already constructs the world with its `AetherChunkGenerator`; the explicit `--generator` is important when Multiverse is doing the create/import operation.

## Bright-sky defaults

Fresh v0.3.0 configs default to:

```yaml
world:
  daylight-cycle: false
  weather-cycle: false

worldgen:
  preset: radiant_end
  radiant-end-layout: true
  growth-density: 1.45
  decoration-density: 1.15
  landmark-spacing-chunks: 28
```

`saveDefaultConfig()` does not overwrite an existing config. If you upgraded an older installation and want permanent bright daylight, manually set `world.daylight-cycle: false` and `world.weather-cycle: false` in the existing `plugins/TheFaeRealm/config.yml`, then restart.

## Generator configuration

```yaml
worldgen:
  preset: radiant_end
  radiant-end-layout: true
  island-density: null
  vertical-scale: null
  cave-density: null
  cloud-level: null
  terrain-profiles: true
  growth-density: 1.45
  decoration-density: 1.15
  resource-density: 1.0
  structure-spacing-chunks: 10
  landmark-spacing-chunks: 28
  dungeon-chance: 0.12
  clouds: true
  decorations: true
  structures: true
  resources: true
```

Generator settings are captured when the world is loaded and require a restart. They affect newly generated chunks only.

## Generator provenance

Each realm stores `fae-realm-generator.yml` with the generator revision, seed, preset, resolved terrain values, Radiant End setting, growth density, decoration/resource density, structure and landmark spacing, settings fingerprints, plugin version and world name. This makes mixed-generation worlds diagnosable after upgrades.

## Commands

- `/fae` — enter the Fae Realm
- `/fae return` — return to the saved portal location
- `/fae biome` — show the current Fae region
- `/fae info` — show generator/settings information
- `/fae locate <region>` — admin deterministic region locator without chunk generation
- `/fae reload` — reload non-generator settings
- `/fae help` — command help

## Build target

- Minecraft/Paper: **26.2**
- Java: **25**
- Generator: **v9 Living Islands**
- Iris: **not required**
- BetterStructures: **optional runtime integration only; not a plugin load dependency**
- Client mods/resource pack: **not required**
- Output: raw `TheFaeRealm-<version>.jar`
