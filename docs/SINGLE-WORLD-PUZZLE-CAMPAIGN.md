# Single-World Puzzle Campaign Goal

## Direction

The Fae Realm remains one Bukkit world named `fae_realm`.

The world is an explorable fantasy realm, not a menu of dimensions. Players arrive at a safe sanctuary near the center, then move outward through the generated islands. Portal landmarks, clues and puzzles reward exploration and gradually reveal a final Secret Cow Level hidden elsewhere in the same world.

The linked-plane experiment from `fae-realm-fantasy-worldgen` is not the target architecture. Its useful visual ideas may be adapted into regions or puzzle pockets, but its additional world folders and direct plane commands must not be merged into the active Paper line.

## Realm entry portal

The player-facing entrance should behave like a vanilla Nether portal while keeping the classic Aether-style identity already present in the plugin:

1. Build a complete glowstone frame with a 2x3 interior.
2. Empty a water bucket inside the frame to activate it.
3. The plugin fills the frame with water and marks it as an active Fae portal.
4. Standing in the portal teleports the player to the safe arrival sanctuary in `fae_realm`.
5. Entering the sanctuary portal returns the player to the remembered source world and safe exit location.

This requires no client mod. `fae_realm` is a separate server world and feels like dimension travel, but it intentionally uses the vanilla `NORMAL` environment for its sun and sky. A brand-new portal block, custom sky renderer or client-defined dimension type is outside the vanilla-client goal and would require client assets or a mod.

Portal activation must validate that the interior is replaceable, respect build protection, tag active frames so arbitrary water cannot teleport players, provide a short configurable warm-up/cooldown, and calculate a collision-safe exit. Portal return data must persist by player UUID across restarts.

## Multiverse compatibility

Multiverse-Core support is a primary compatibility target, not a required dependency.

- Expose `TheFaeRealm` through `JavaPlugin#getDefaultWorldGenerator` early enough for Multiverse to select it.
- Allow Multiverse to import or create `fae_realm` with `--generator TheFaeRealm`.
- After plugin startup, bind to the already loaded `fae_realm` when Multiverse owns its lifecycle.
- Create the world with Bukkit `WorldCreator` only when it is still absent and plugin-managed fallback is enabled.
- Never create a second fallback world under a different name because Multiverse startup order was delayed.
- Validate the loaded world's generator metadata and warn clearly when it was imported without `TheFaeRealm`.
- Keep Multiverse-Portals optional. Its portals may target `fae_realm`, while the built-in glowstone/water portal continues to work without it.
- Test direct plugin creation, Multiverse create/import, restart loading, disabled Multiverse, and a missing/renamed world.

## Player journey

1. **Center sanctuary** — `/fae` and the normal portal place players at the safe arrival island. The sanctuary teaches the portal language without exposing the campaign finale.
2. **Outward exploration** — the chance of finding campaign landmarks increases beyond a configurable safe radius. Ordinary structures and vaults remain useful discoveries.
3. **Regional portals** — deterministic landmarks open short-range or long-range travel to authored puzzle pockets within `fae_realm`.
4. **Puzzle sigils** — completing themed pockets records permanent player progress and unlocks later portal behavior or clues.
5. **Cow sanctuaries** — rare side portals lead to cow-filled islands with a puzzle, protected reward chest or limited unique cow reward.
6. **Secret Cow Level** — the complete clue/sigil chain reveals the final Cow Court, still inside `fae_realm` and unreachable through a public direct command.

## Regional puzzle themes

The five existing generated regions become the campaign's chapters without imposing a rigid single path:

| Region | Puzzle character | Possible clue |
| --- | --- | --- |
| Golden Meadows | sunlight, mirrors and shrine alignment | Sun Sigil |
| Crystal Woods | crystal tones, colors and ordered resonance | Crystal Sigil |
| Mist Gardens | visibility, memory and changing paths | Mist Sigil |
| Ancient Fae Forest | roots, ruins and environmental logic | Root Sigil |
| Sky Highlands | wind, vertical traversal and sky gates | Sky Sigil |

Players should be able to discover early puzzles in different orders. Later puzzles can require a configurable number of sigils rather than forcing every group through one exact route.

## Cow encounters

Cow areas are secrets worth finding, not ordinary passive-mob farms.

- A cow sanctuary is a bounded puzzle pocket or distinctive island inside `fae_realm`.
- It can contain a herd, themed Fae cows, environmental clues and one protected reward point.
- A reward can be a per-player chest, a one-time completion grant, or a cooldown-limited unique cow drop.
- Killing ordinary spawned cows must not bypass puzzle completion or create an unlimited rare-item farm.
- Unique rewards should be tagged with plugin `PersistentDataContainer` keys and exposed through API events or console commands so AlbionMC quests can react to them.
- The final Cow Court should have its own reward pool and completion marker.

Potential reward identities can be finalized when the first encounter is implemented. The initial code should use configurable Material/items and avoid hard-wiring balance values into the generator.

## Technical guardrails

- Keep exactly one managed Fae world in the default configuration.
- Intra-realm portals teleport between safe locations in `fae_realm`; they do not create or load additional worlds.
- Portal and puzzle placement must be deterministic from the world seed and must not force-load neighboring chunks during generation.
- Generated puzzle landmarks need stable IDs derived from the seed/grid location so progress survives restarts.
- Store per-player campaign state by UUID outside disposable chunk state. Chunk PDC may identify generated portal/puzzle instances.
- Protect puzzle blocks, reward containers and scripted cows from normal editing unless an administrator explicitly bypasses protection.
- Recover safely when a destination chunk is missing, obstructed or from an older generator revision.
- Do not rewrite existing chunks. Servers need fresh chunks or a fresh `fae_realm` to receive newly generated campaign landmarks.
- Keep `/fae return`; do not add public commands that skip sigils or reveal the Cow Court coordinates. Admin debug commands may exist behind `faerealm.admin`.
- Avoid entity-heavy decoration. Cow herds must use configurable population limits and cleanup rules suitable for AlbionMC.

## Delivery slices

1. **Campaign foundation** — stable landmark IDs, player progress store, portal safety service and admin inspection commands.
2. **First playable pocket** — one regional portal, one puzzle, checkpoint persistence and a per-player reward.
3. **Cow sanctuary prototype** — bounded herd, protected prize, anti-farm rules and quest/API hook.
4. **Regional expansion** — puzzle pockets and sigils for all five regions with non-linear discovery.
5. **Secret Cow Level** — clue validation, concealed Cow Court portal, finale encounter and final reward.
6. **Runtime validation** — multiplayer concurrency, restart persistence, Chunky generation, portal recovery and entity-budget tests.

This document records the product goal. Each delivery slice should be implemented and runtime-tested independently before the Secret Cow Level is considered complete.
