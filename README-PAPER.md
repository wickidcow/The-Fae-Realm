# Aether Legacy for Paper

This branch is the experimental Paper 26.2 / Java 25 port of The Aether codebase.

## Fae Realm

The Paper port creates **Fae Realm** as a generator-driven world. It is not a pre-generated map download. The world folder defaults to `fae_realm`, and new chunks are produced deterministically from the configured world seed as players explore.

Current world-generation foundation includes:

- Large floating continents and smaller satellite islands
- Seeded terrain warping and elevation variation
- Five Fae Realm regions: Golden Meadows, Crystal Woods, Mist Gardens, Ancient Fae Forest, and Sky Highlands
- Vanilla client-visible biome data mapped beneath those regions
- Biome-specific surface palettes, trees, flowers, crystal outcrops, and occasional small fae ruins
- Hollow island caverns
- Decorative cloud shelves below the main islands, kept separate from island terrain
- Guaranteed safe starter island at the realm origin
- No bundled original Aether assets

The same seed recreates the same generated terrain. Existing generated chunks are preserved by Minecraft; generator changes apply naturally to newly explored chunks unless the realm world folder is intentionally regenerated.

## Commands

- `/aether` — travel to the Fae Realm
- `/fae` — alias for `/aether`
- `/faerealm` — alias for `/aether`
- `/aether return` — return to the normal world
- `/aether info` — show realm/plugin information
- `/aether reload` — reload plugin configuration (admin)

## Status

This is still an early port. Terrain and biome generation are now established as the first major foundation. Next milestones are richer portal activation, larger procedural structures/dungeons, resources, mobs, and progression.
