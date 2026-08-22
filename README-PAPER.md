# Aether Legacy for Paper

This branch is the experimental Paper 26.2 / Java 25 port of The Aether codebase.

## Fae Realm

The Paper port creates **Fae Realm** as a generator-driven world. It is not a pre-generated map download. The world folder defaults to `fae_realm`, and new chunks are produced deterministically from the configured world seed as players explore.

Current world-generation foundation includes:

- Large floating continents and smaller satellite islands
- Seeded terrain warping and elevation variation
- Five server-side biome identities: Golden Meadows, Crystal Woods, Mist Gardens, Ancient Fae Forest, and Sky Highlands
- Biome-specific surface palettes and small flora/crystal details
- Hollow island caverns
- Decorative cloud shelves below the main islands
- Guaranteed safe starter island at the realm origin
- No bundled original Aether assets

The same seed recreates the same generated terrain. Existing generated chunks are preserved by Minecraft; generator changes apply naturally to newly explored chunks unless the realm world folder is intentionally regenerated.

## Status

This is still an early port. Terrain generation is being established before structures, custom mobs, progression, and richer portal behavior are added.
