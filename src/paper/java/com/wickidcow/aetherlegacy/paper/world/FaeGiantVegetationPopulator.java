package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.SplittableRandom;

/**
 * Large vegetation silhouettes that make the Fae Realm feel overgrown at a distance.
 *
 * <p>The shapes are intentionally built from vanilla blocks and only use
 * {@link LimitedRegion}, keeping the pass deterministic and safe for Paper's
 * asynchronous chunk generation and pregeneration tools.</p>
 */
public final class FaeGiantVegetationPopulator {

    private static final long GIANT_GROWTH_SALT = 0x6A09E667F3BCC909L;

    public void populate(@NotNull WorldInfo info,
                         int chunkX,
                         int chunkZ,
                         @NotNull LimitedRegion region,
                         @NotNull FaeGeneratorSettings settings) {
        double density = settings.growthDensity();
        if (density <= 0.0) {
            return;
        }

        SplittableRandom random = new SplittableRandom(
            mixSeed(info.getSeed() ^ GIANT_GROWTH_SALT, chunkX, chunkZ));

        // Large silhouettes should be common enough to overlap visually across
        // nearby islands without putting a giant object in every single chunk.
        double chance = clamp(0.0, 0.78, 0.22 * density);
        if (random.nextDouble() > chance) {
            return;
        }

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int attempts = density >= 1.9 && random.nextDouble() < 0.34 ? 2 : 1;

        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = baseX + 4 + random.nextInt(8);
            int z = baseZ + 4 + random.nextInt(8);
            int surfaceY = FaeSurfaceLocator.find(info, region, x, z);
            if (surfaceY == Integer.MIN_VALUE || surfaceY + 24 >= info.getMaxHeight()) {
                continue;
            }
            if (!hasOpenVolume(region, x, surfaceY + 1, z, 2, 8)) {
                continue;
            }

            FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
            switch (biome) {
                case GOLDEN_MEADOWS -> {
                    if (random.nextDouble() < 0.72) {
                        placeGiantBlossom(region, random, x, surfaceY + 1, z, false);
                    } else {
                        placeRootArch(info, region, random, x, surfaceY + 1, z, Material.OAK_WOOD);
                    }
                }
                case CRYSTAL_WOODS -> {
                    if (random.nextDouble() < 0.58) {
                        placeChorusGrove(info, region, random, x, surfaceY + 1, z, true);
                    } else if (random.nextBoolean()) {
                        placeGiantBlossom(region, random, x, surfaceY + 1, z, true);
                    } else {
                        placeCrystalMushroom(region, random, x, surfaceY + 1, z);
                    }
                }
                case MIST_GARDENS -> {
                    if (random.nextDouble() < 0.64) {
                        placeMushroomGrove(info, region, random, x, surfaceY + 1, z, true);
                    } else {
                        placeRootArch(info, region, random, x, surfaceY + 1, z, Material.PALE_OAK_WOOD);
                    }
                }
                case ANCIENT_FAE_FOREST -> {
                    if (random.nextDouble() < 0.56) {
                        placeRootArch(info, region, random, x, surfaceY + 1, z, Material.DARK_OAK_WOOD);
                    } else {
                        placeMushroomGrove(info, region, random, x, surfaceY + 1, z, false);
                    }
                }
                case SKY_HIGHLANDS -> {
                    if (random.nextDouble() < 0.68) {
                        placeChorusGrove(info, region, random, x, surfaceY + 1, z, false);
                    } else {
                        placeGiantBlossom(region, random, x, surfaceY + 1, z, true);
                    }
                }
            }
        }
    }

    private void placeChorusGrove(WorldInfo info,
                                  LimitedRegion region,
                                  SplittableRandom random,
                                  int centerX,
                                  int baseY,
                                  int centerZ,
                                  boolean crystalAccent) {
        int stalks = 4 + random.nextInt(5);
        for (int stalk = 0; stalk < stalks; stalk++) {
            int x = centerX + random.nextInt(-4, 5);
            int z = centerZ + random.nextInt(-4, 5);
            int surfaceY = FaeSurfaceLocator.find(info, region, x, z);
            if (surfaceY == Integer.MIN_VALUE) {
                continue;
            }

            // Chorus is not just decorative here: give each stalk a legal End-style
            // root so later neighbor updates do not make the plant pop off.
            if (region.isInRegion(x, surfaceY, z)) {
                region.setType(x, surfaceY, z,
                    crystalAccent && random.nextDouble() < 0.24 ? Material.PURPUR_BLOCK : Material.END_STONE);
            }

            int y = surfaceY + 1;
            int height = 4 + random.nextInt(7);
            int px = x;
            int pz = z;
            for (int dy = 0; dy < height; dy++) {
                if (dy > 1 && dy % 3 == 0 && random.nextDouble() < 0.52) {
                    px += random.nextInt(-1, 2);
                    pz += random.nextInt(-1, 2);
                }
                setIfAir(region, px, y + dy, pz, Material.CHORUS_PLANT);

                if (dy >= 2 && random.nextDouble() < 0.28) {
                    int bx = random.nextBoolean() ? 1 : -1;
                    int bz = random.nextBoolean() ? 1 : -1;
                    setIfAir(region, px + bx, y + dy, pz, Material.CHORUS_PLANT);
                    if (random.nextDouble() < 0.62) {
                        setIfAir(region, px + bx, y + dy + 1, pz + bz, Material.CHORUS_FLOWER);
                    }
                }
            }
            setIfAir(region, px, y + height, pz, Material.CHORUS_FLOWER);

            if (crystalAccent && random.nextDouble() < 0.44) {
                setIfAir(region, x + 1, y, z, Material.AMETHYST_BLOCK);
                setIfAir(region, x + 1, y + 1, z, Material.AMETHYST_CLUSTER);
            }
        }

        // End-stone patches visually tie the borrowed End flora into the Fae palette.
        for (int i = 0; i < 12; i++) {
            int x = centerX + random.nextInt(-5, 6);
            int z = centerZ + random.nextInt(-5, 6);
            int surfaceY = FaeSurfaceLocator.find(info, region, x, z);
            if (surfaceY != Integer.MIN_VALUE && region.isInRegion(x, surfaceY, z)
                && random.nextDouble() < 0.68) {
                region.setType(x, surfaceY, z,
                    crystalAccent && random.nextDouble() < 0.24 ? Material.PURPUR_BLOCK : Material.END_STONE);
            }
        }
    }

    private void placeMushroomGrove(WorldInfo info,
                                    LimitedRegion region,
                                    SplittableRandom random,
                                    int centerX,
                                    int baseY,
                                    int centerZ,
                                    boolean misty) {
        int mushrooms = 2 + random.nextInt(4);
        for (int i = 0; i < mushrooms; i++) {
            int x = centerX + random.nextInt(-4, 5);
            int z = centerZ + random.nextInt(-4, 5);
            int surfaceY = FaeSurfaceLocator.find(info, region, x, z);
            if (surfaceY == Integer.MIN_VALUE) {
                continue;
            }
            int height = 5 + random.nextInt(7);
            int radius = 2 + random.nextInt(3);
            Material cap = random.nextBoolean() ? Material.RED_MUSHROOM_BLOCK : Material.BROWN_MUSHROOM_BLOCK;
            placeGiantMushroom(region, random, x, surfaceY + 1, z, height, radius, cap, misty);
        }
    }

    private void placeGiantMushroom(LimitedRegion region,
                                    SplittableRandom random,
                                    int x,
                                    int y,
                                    int z,
                                    int height,
                                    int radius,
                                    Material cap,
                                    boolean misty) {
        int stemX = x;
        int stemZ = z;
        for (int dy = 0; dy < height; dy++) {
            if (dy > 2 && dy % 3 == 0 && random.nextDouble() < 0.38) {
                stemX += random.nextInt(-1, 2);
                stemZ += random.nextInt(-1, 2);
            }
            set(region, stemX, y + dy, stemZ, Material.MUSHROOM_STEM);
        }

        int capY = y + height;
        for (int dy = -1; dy <= 1; dy++) {
            int layerRadius = dy == 0 ? radius : Math.max(1, radius - 1);
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > layerRadius + 0.25 || random.nextDouble() < 0.08) {
                        continue;
                    }
                    Material material = random.nextDouble() < 0.10 ? Material.SHROOMLIGHT : cap;
                    setIfAir(region, stemX + dx, capY + dy, stemZ + dz, material);
                }
            }
        }

        if (misty) {
            for (int i = 0; i < 10; i++) {
                int ox = random.nextInt(-radius, radius + 1);
                int oz = random.nextInt(-radius, radius + 1);
                int startY = capY - 1;
                if (!region.isInRegion(stemX + ox, startY, stemZ + oz)
                    || region.getType(stemX + ox, startY, stemZ + oz).isAir()) {
                    continue;
                }
                int length = 1 + random.nextInt(4);
                for (int d = 1; d <= length; d++) {
                    if (!setIfAir(region, stemX + ox, startY - d, stemZ + oz, Material.PALE_HANGING_MOSS)) {
                        break;
                    }
                }
            }
        }
    }

    private void placeCrystalMushroom(LimitedRegion region,
                                      SplittableRandom random,
                                      int x,
                                      int y,
                                      int z) {
        int height = 6 + random.nextInt(6);
        for (int dy = 0; dy < height; dy++) {
            set(region, x, y + dy, z, dy % 3 == 2 ? Material.CALCITE : Material.MUSHROOM_STEM);
        }
        int capY = y + height;
        int radius = 3 + random.nextInt(2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 2) {
                    continue;
                }
                Material cap = random.nextDouble() < 0.18 ? Material.AMETHYST_BLOCK : Material.PURPUR_BLOCK;
                setIfAir(region, x + dx, capY, z + dz, cap);
                if (random.nextDouble() < 0.08) {
                    setIfAir(region, x + dx, capY + 1, z + dz, Material.AMETHYST_CLUSTER);
                }
            }
        }
        setIfAir(region, x, capY + 1, z, Material.AMETHYST_CLUSTER);
    }

    private void placeGiantBlossom(LimitedRegion region,
                                   SplittableRandom random,
                                   int x,
                                   int y,
                                   int z,
                                   boolean ethereal) {
        int height = 7 + random.nextInt(7);
        Material stem = ethereal ? Material.CHERRY_LOG : Material.MANGROVE_LOG;
        int stemX = x;
        int stemZ = z;
        for (int dy = 0; dy < height; dy++) {
            if (dy > 2 && dy % 3 == 0 && random.nextDouble() < 0.42) {
                stemX += random.nextInt(-1, 2);
                stemZ += random.nextInt(-1, 2);
            }
            set(region, stemX, y + dy, stemZ, stem);
        }

        int bloomY = y + height;
        Material primary = ethereal ? Material.CHERRY_LEAVES : Material.FLOWERING_AZALEA_LEAVES;
        Material secondary = ethereal ? Material.FLOWERING_AZALEA_LEAVES : Material.AZALEA_LEAVES;
        int[][] directions = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
        };
        for (int[] direction : directions) {
            int length = 2 + random.nextInt(3);
            for (int step = 1; step <= length; step++) {
                int px = stemX + direction[0] * step;
                int pz = stemZ + direction[1] * step;
                int py = bloomY + (step == length && random.nextBoolean() ? 1 : 0);
                setIfAir(region, px, py, pz,
                    random.nextDouble() < 0.34 ? secondary : primary);
                if (step > 1 && random.nextDouble() < 0.46) {
                    setIfAir(region, px, py - 1, pz, primary);
                }
            }
        }
        setIfAir(region, stemX, bloomY, stemZ, Material.SHROOMLIGHT);
        setIfAir(region, stemX, bloomY + 1, stemZ, primary);
    }

    private void placeRootArch(WorldInfo info,
                               LimitedRegion region,
                               SplittableRandom random,
                               int x,
                               int y,
                               int z,
                               Material wood) {
        boolean alongX = random.nextBoolean();
        int halfSpan = 4 + random.nextInt(4);
        int height = 5 + random.nextInt(5);

        for (int offset = -halfSpan; offset <= halfSpan; offset++) {
            double normalized = offset / (double) halfSpan;
            int rise = (int) Math.round((1.0 - normalized * normalized) * height);
            int px = alongX ? x + offset : x;
            int pz = alongX ? z : z + offset;
            int surfaceY = FaeSurfaceLocator.find(info, region, px, pz);
            int py = surfaceY == Integer.MIN_VALUE ? y : surfaceY + 1;
            for (int dy = 0; dy <= rise; dy++) {
                Material material = dy <= 1 && random.nextDouble() < 0.44 ? Material.MANGROVE_ROOTS : wood;
                setIfAir(region, px, py + dy, pz, material);
            }

            if (rise >= height - 1 && random.nextDouble() < 0.72) {
                setIfAir(region, px, py + rise + 1, pz, Material.FLOWERING_AZALEA_LEAVES);
            }
        }

        // Curtains hanging from the arch make the silhouette read as vegetation,
        // not a wooden bridge.
        for (int i = 0; i < 14; i++) {
            int offset = random.nextInt(-halfSpan + 1, halfSpan);
            double normalized = offset / (double) halfSpan;
            int rise = (int) Math.round((1.0 - normalized * normalized) * height);
            int px = alongX ? x + offset : x;
            int pz = alongX ? z : z + offset;
            int surfaceY = FaeSurfaceLocator.find(info, region, px, pz);
            if (surfaceY == Integer.MIN_VALUE) {
                continue;
            }
            int topY = surfaceY + 1 + rise;
            int length = 2 + random.nextInt(5);
            Material hanging = random.nextBoolean() ? Material.CAVE_VINES : Material.HANGING_ROOTS;
            for (int d = 1; d <= length; d++) {
                if (!setIfAir(region, px, topY - d, pz, hanging)) {
                    break;
                }
            }
        }
    }

    private boolean hasOpenVolume(LimitedRegion region,
                                  int x,
                                  int y,
                                  int z,
                                  int radius,
                                  int height) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= height; dy += 2) {
                    if (!region.isInRegion(x + dx, y + dy, z + dz)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void set(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) {
            region.setType(x, y, z, material);
        }
    }

    private boolean setIfAir(LimitedRegion region, int x, int y, int z, Material material) {
        if (!region.isInRegion(x, y, z) || !region.getType(x, y, z).isAir()) {
            return false;
        }
        region.setType(x, y, z, material);
        return true;
    }

    private static double clamp(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }

    private static long mixSeed(long seed, int x, int z) {
        long mixed = seed;
        mixed ^= (long) x * 341873128712L;
        mixed ^= (long) z * 132897987541L;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return mixed;
    }
}
