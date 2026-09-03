# The Fae Realm

**The Fae Realm** is a server-side fantasy world generator for **Paper 26.2 / Java 25**. It creates a separate bright floating-island realm from a seed as players explore. It is not a pre-generated map and does not require Forge, Fabric, NeoForge, Iris, or a client resource pack.

The Paper implementation owns its terrain, ecology, regional identities, resources, structures, dungeons, progression hooks, portals, configuration and runtime validation. Iris/Terra-style ideas such as layered noise fields, domain warping and deterministic object placement are implemented independently in this plugin; Iris is not a dependency.

## Generator v10 — Sculpted & Overgrown Islands

Generator revision **v10** keeps the bright Radiant End layout while pushing the realm toward a deliberately excessive fantasy look: sculpted floating continents, dense overlapping vegetation, frequent magical details and unusually rich resources.

The arrival island keeps a safe calm center, but its outer terrain now rolls into hills and cliffs instead of reading as a flat starter platform. Ordinary islands use stronger broad relief, valleys, ridges, coastline lobes, elongated silhouettes, deeper undersides and more varied satellite-island elevations.

Ecology is intentionally abundant. Trees, understory, flowers, moss, mushrooms, hanging roots, root knots and magical growth overlap much more heavily than before. Small biome features can appear multiple times in ordinary chunks, while large landmarks and structures remain on deterministic spacing grids.

The realm deliberately remains a Bukkit **NORMAL** environment. The custom generator provides the void/floating-island geography while vanilla clients keep the normal sky and sun.

Existing chunks are never rewritten automatically. Upgrading an existing realm changes only newly generated chunks. For the cleanest v10 terrain and ecology, create a fresh realm/world folder or test beyond the pregenerated border.

### Terrain

- Large floating continents and satellite islands
- Broad macro-noise creating archipelagos and wide open-sky zones
- Sculpted coastlines with lobes, bays and stretched silhouettes
- Stronger hills, ridges, valleys, terraces and vertical relief
- Six profiles including the safe **Arrival** profile plus **Balanced, Plateau, Spire, Terraced, Hollow**
- More dramatic Spire islands and natural Terraced shelves
- Deeper undersides with hanging formations
- **Radiant End** preset with open sky and strong height separation
- Open void below the islands
- Decorative cloud shelves
- Guaranteed safe starter continent at the origin

### Presets

`worldgen.preset` supports:

- **radiant_end** — default; bright End-like openness with overgrown Fae islands
- **balanced** — general-purpose original layout
- **ethereal** — sparse islands and stronger height differences
- **lush** — dense archipelagos and gentler relief
- **wild** — stronger vertical relief and more caverns

`worldgen.radiant-end-layout: true` can also apply the open-sky spacing bias to the older presets.

## Overgrown multi-noise ecology

Generator v10 layers an additional ecology field over the biome flora. Independent fertility, moisture and magical-energy noise fields are domain-warped into broad coherent growth regions instead of placing every plant with unrelated random rolls.

The ecology bands are:

- **Sparse** — intentionally uncommon with the v10 defaults
- **Meadow**
- **Lush**
- **Ancient**
- **Enchanted**

These control additional ground growth, flowers, moss, root knots and hanging vegetation. `worldgen.growth-density` controls the strength of this pass independently from normal decoration density.

The biome-aware tree layer includes Sun Crown trees, Crystal trees, Mist Willows, twisted Ancient trees and windswept birch growth. At the default decoration density, chunks make roughly 12–18 tree placement attempts and roughly 95–140 understory attempts before the separate multi-noise ecology pass is applied.

## Giant fantasy vegetation

Generator v10 also adds a separate large-vegetation pass before ordinary trees and ground cover. These are common enough to shape the skyline but still spaced so islands retain paths, clearings and usable terrain.

- **Golden Meadows** — oversized glowing blossoms and living root arches
- **Crystal Woods** — End-influenced chorus groves, giant ethereal blossoms and crystal/purpur mushrooms
- **Mist Gardens** — oversized red/brown mushroom groves with pale hanging moss and pale-root arches
- **Ancient Fae Forest** — large mushroom groves and twisted living root arches with hanging vines
- **Sky Highlands** — End-influenced chorus groves and tall ethereal blossoms

Chorus groves are intentionally blended into the Fae palette rather than turning entire regions into End terrain. Each chorus stalk roots into End stone or purpur, with small End-stone/purpur patches bleeding into the surrounding Fae ground. Crystal variants can grow beside amethyst outcrops.

Oversized mushrooms reach roughly 5–11 blocks above their ground level and use broad irregular caps with occasional shroomlight. Giant blossoms rise roughly 7–13 blocks and use living log stems, leaf petals and glowing centers. Root arches span the landscape with mangrove roots, biome wood and curtains of cave vines or hanging roots.

All giant vegetation uses vanilla blocks and Paper `LimitedRegion`; it does not spawn entities, force-load chunks or require a client resource pack.

## Fae regions

Five deterministic region identities control surface palettes, vegetation, structures, resources and landmarks:

- **Golden Meadows**
- **Crystal Woods**
- **Mist Gardens**
- **Ancient Fae Forest**
- **Sky Highlands**

Use `/fae biome` in the realm to inspect the current region. Admins can use `/fae locate <region>` without generating chunks.

## Busy fantasy details

Ordinary terrain is intentionally decorated so long empty stretches are rare. Depending on region, chunks can contain multiple small details such as:

- flower circles and glowing Sun Pools
- crystal spires and visible magical resource blooms
- mushroom clusters and pale-moss growth
- fallen ancient logs and root tangles
- standing stones and glowing markers
- small Fae ruins
- underside roots, vines and hanging formations

Crystal outcrops and small ruins are more common than in v9, while the stability checks still reject placements on unsuitable steep or crowded ground.

## Large landmarks

Generator v10 uses a separate long-range landmark grid so major sights stay recognizable while appearing often enough to support the busy-world feel.

Depending on region, exploration can find:

- enormous rooted Great Fae Trees
- giant cherry/crystal trees
- Crystal Crowns
- glowing Hanging Gardens and waterfall spillways
- quartz/calcite Sky Arches
- broken Sun Causeways

`worldgen.landmark-spacing-chunks` controls their grid spacing and defaults to `22`.

## Abundant resources

The Fae Realm is intentionally richer than the overworld. Resource generation is biome-weighted so regions still feel different, but the default density produces many more veins and larger deposits than v9.

Examples include:

- Golden Meadows — gold, copper and emeralds
- Crystal Woods — amethyst, lapis and diamonds
- Mist Gardens — lapis, iron and glowstone
- Ancient Fae Forest — emeralds, coal and diamonds
- Sky Highlands — iron, copper and gold

At the default `resource-density: 1.80`, the generator makes roughly 20–32 underground resource attempts per chunk before optional secondary vein growth. It also places visible biome-themed resource blooms on the surface so islands look magically rich before mining begins.

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

## Bright, overgrown defaults

Fresh 0.3.2 configs on generator v10 default to:

```yaml
world:
  daylight-cycle: false
  weather-cycle: false

worldgen:
  preset: radiant_end
  radiant-end-layout: true
  growth-density: 2.05
  decoration-density: 1.65
  resource-density: 1.80
  structure-spacing-chunks: 8
  landmark-spacing-chunks: 22
  dungeon-chance: 0.16
```

`saveDefaultConfig()` does not overwrite an existing config. If you upgrade an installation that already has `plugins/TheFaeRealm/config.yml`, copy the new worldgen values manually if you want the same overgrown/resource-rich balance, then restart.

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
  growth-density: 2.05
  decoration-density: 1.65
  resource-density: 1.80
  structure-spacing-chunks: 8
  landmark-spacing-chunks: 22
  dungeon-chance: 0.16
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
- Plugin version: **0.3.2**
- Generator: **v10 Sculpted & Overgrown Islands**
- Iris: **not required**
- BetterStructures: **optional runtime integration only; not a plugin load dependency**
- Client mods/resource pack: **not required**
- Output: raw `TheFaeRealm-<version>.jar`
