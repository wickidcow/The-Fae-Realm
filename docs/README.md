# ✨ The Fae Realm

**The Fae Realm** is a custom server-side fantasy world generator built for **Paper 26.2 / Java 25**.

It creates a bright, magical floating-island dimension filled with strange forests, ancient ruins, glowing gardens, crystal formations, hanging roots, waterfalls, structures, vaults, resources, and large procedural landmarks.

No client mods are required.

## 🌤️ A Bright Floating Realm

The Fae Realm is designed to feel a little like **The End reimagined as a living fantasy world**.

Instead of endless darkness and barren islands, expect:

* ☀️ Bright Overworld-style sky and sunlight
* 🏝️ Large floating continents and smaller satellite islands
* 🌳 Dense magical forests
* 🌸 Flower meadows and overgrown gardens
* 💎 Crystal forests and amethyst formations
* 🍄 Misty mushroom regions
* 🌿 Hanging roots, vines, moss, and underside vegetation
* 💧 Floating waterfalls
* 🏛️ Ancient ruins, shrines, temples, and gates
* 🗝️ Fae Vaults and dungeon areas
* 🌲 Huge procedural landmark trees
* 🌉 Broken sky bridges and Sun Causeways
* ✨ Rare enchanted growth zones

Generation is completely server-side and uses vanilla Minecraft blocks.

---

## 🌎 Fae Regions

The realm contains several deterministic fantasy regions:

### 🌻 Golden Meadows

Warm flower-covered islands with glowing pools, Sun Court structures, and large open landscapes.

### 💎 Crystal Woods

Cherry forests, amethyst formations, crystal trees, and magical structures.

### 🌫️ Mist Gardens

Pale vegetation, mushrooms, moss, hanging growth, and mysterious ruins.

### 🌲 Ancient Fae Forest

Dense forests filled with giant twisted trees, roots, ferns, moss, ruins, and ancient structures.

### ⛰️ Sky Highlands

Higher floating islands with windswept forests, stone formations, arches, and dramatic terrain.

---

## 🌱 Procedural Ecology

The Fae Realm uses layered procedural noise to control how alive an island becomes.

Different areas can generate as:

* Sparse
* Meadow
* Lush
* Ancient
* Enchanted

The system controls vegetation density, flowers, moss, fungi, hanging roots, magical growth, trees, and rare environmental features.

This gives islands much more variation than simply placing random trees across the terrain.

---

## 🏰 Structures & Landmarks

The Fae Realm contains its own procedural structure system.

You may discover:

* Sun Court Shrines
* Crystal Temples
* Mist Sanctums
* Ancient Watchtowers
* Sky Gates
* Fae Vault entrances
* Small ruins
* Crystal outcrops
* Flower circles
* Mushroom groves
* Standing stones

There are also much rarer large landmarks, including:

* 🌳 Great Fae Trees
* 🌸 Giant Crystal / Cherry Trees
* 💎 Crystal Crowns
* 🌿 Hanging Gardens
* 🌉 Broken Sun Causeways
* 🏛️ Massive Sky Arches

No external structure plugin is required for the core experience.

---

## 🌌 Radiant End Generation

The default generator profile is **Radiant End**.

It creates:

* Wider areas of open sky
* More separated floating islands
* Stronger vertical variation
* Large central continents
* Satellite islands
* Dramatic cliffs and island undersides
* Bright sunlight instead of The End's dark atmosphere

The realm intentionally uses a **NORMAL world environment** with a custom floating-island generator.

This means you get End-like floating geography while keeping a bright vanilla sky.

---

# 🌍 Multiverse-Core Installation

If you use **Multiverse-Core**, make sure the Fae Realm is imported using **TheFaeRealm as the world generator**.

Install the plugin and restart your server first.

You can verify that Multiverse sees the generator with:

`/mv generators`

You should see:

`TheFaeRealm`

### Import an existing Fae Realm

Use:

`/mv import minecraft:fae_realm normal --generator TheFaeRealm`

### Create a new Fae Realm through Multiverse

Use:

`/mv create minecraft:fae_realm normal --generator TheFaeRealm`

⚠️ **Use `normal`, not `the_end`.**

The Fae Realm handles the floating-island generation itself while keeping the world environment bright and sunny.

---

## 🚪 Entering the Realm

You can travel directly with:

`/fae`

Aliases:

`/faerealm`

`/aether`

You can also build a Fae portal using a **Glowstone frame with water inside**.

Additional commands include:

`/fae return`

Return to your saved portal location.

`/fae biome`

Displays your current Fae region.

`/fae info`

Shows generator, realm, and configuration information.

`/fae locate <region>`

Admin command for locating Fae regions without generating chunks.

---

## ⚙️ Generator Configuration

World generation can be customized through the plugin configuration.

Available terrain presets include:

* `radiant_end`
* `balanced`
* `ethereal`
* `lush`
* `wild`

You can independently control:

* Island density
* Vertical terrain scale
* Cave density
* Growth density
* Decoration density
* Resource density
* Structure spacing
* Landmark spacing
* Vault frequency
* Cloud generation

Generator settings affect **newly generated chunks**.

Existing terrain is never automatically rewritten.

---

## 🧚 Compatibility

Designed for:

**Minecraft 26.2**
**Paper 26.2**
**Java 25**

Also designed with server performance and asynchronous Paper chunk generation in mind.

The generator does **not** require:

* Iris
* Terra
* TerraformGenerator
* BetterStructures
* Forge
* Fabric
* NeoForge
* Client-side mods

The Fae Realm uses its own standalone procedural generation system.

---

## 💜 Explore Somewhere Different

The Fae Realm is meant to feel like another world rather than another Overworld biome.

Floating forests.

Ancient ruins.

Massive trees.

Crystal gardens.

Open skies.

Hidden vaults.

And islands stretching through the clouds.

**Welcome to The Fae Realm. ✨**
