# Aether Legacy for Paper

This branch is the experimental Paper 26.2 / Java 25 port of The Aether codebase.

## Fae Realm

The Paper port creates **Fae Realm** as a generator-driven world. It is not a pre-generated map download. The world folder defaults to `fae_realm`, and new chunks are produced deterministically from the configured world seed as players explore.

Current world-generation foundation includes:

- Large floating continents and smaller satellite islands
- Seeded terrain warping and elevation variation
- Five Fae Realm regions: Golden Meadows, Crystal Woods, Mist Gardens, Ancient Fae Forest, and Sky Highlands
- Vanilla client-visible biome data mapped beneath those regions
- Biome-specific surface palettes, trees, flowers, crystal outcrops, and small fae ruins
- Hollow island caverns
- Decorative cloud shelves below the main islands, kept separate from island terrain
- Guaranteed safe starter island at the realm origin
- Deterministic regional structures and rare dungeon gates
- No bundled original Aether assets

The same seed recreates the same generated terrain. Existing generated chunks are preserved by Minecraft; generator changes apply naturally to newly explored chunks unless the realm world folder is intentionally regenerated.

## Procedural structures

The Fae Realm currently generates its own server-side structure families:

- **Golden Meadows:** Sun Court Shrines
- **Crystal Woods:** Crystal Temples
- **Mist Gardens:** Mist Sanctums
- **Ancient Fae Forest:** Ancient Watchtowers
- **Sky Highlands:** Sky Gates
- **All regions:** rare Dungeon Gates, reserved as entry points for the larger dungeon framework

Structure candidates are derived from the world seed, terrain-tested, and placed during safe LimitedRegion chunk generation.

## BetterStructures compatibility

BetterStructures is optional; Aether Legacy for Paper does not require it.

By default, generic BetterStructures packs are kept out of `fae_realm` so the realm retains its own structure identity. A compatible BetterStructures build can expose `ValidWorldsConfig.setWorldValidity(...)`, allowing Aether Legacy to mark Fae Realm invalid **before BetterStructures runs its new-chunk scanners**. Older BetterStructures builds use a cancellable placement-event fallback instead.

The default can be changed in `config.yml`:

```yaml
integrations:
  betterstructures:
    allow-generic-structures-in-fae-realm: false
```

A future dedicated Fae Realm BetterStructures content pack can use the same integration without making BetterStructures a mandatory dependency.

## Portal and commands

A completed 4x5 Glowstone frame can be activated with a water bucket. Entering its water-filled interior transports the player to Fae Realm and preserves a return location.

- `/aether` — travel to the Fae Realm
- `/fae` — alias for `/aether`
- `/faerealm` — alias for `/aether`
- `/aether return` — return to the normal world
- `/aether info` — show realm/plugin and BetterStructures integration information
- `/aether reload` — reload plugin configuration (admin)

## Status

This is still an early port. Terrain, biomes, portals, and the first procedural structure layer are established. Next milestones are the dungeon interior framework, realm resources/loot, mobs, and progression.
