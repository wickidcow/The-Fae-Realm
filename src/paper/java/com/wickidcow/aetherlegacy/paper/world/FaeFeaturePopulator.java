package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;
import java.util.SplittableRandom;

/**
 * Small biome-specific landmarks that make ordinary exploration feel authored
 * without consuming the rarity budget used by major structures.
 */
public final class FaeFeaturePopulator {

    private static final long FEATURE_SALT = 0xA4093822299F31D0L;

    public void populate(WorldInfo info,
                         Random chunkRandom,
                         int chunkX,
                         int chunkZ,
                         LimitedRegion region,
                         double density) {
        SplittableRandom random = new SplittableRandom(
            mixSeed(info.getSeed() ^ FEATURE_SALT, chunkX, chunkZ));

        // Ordinary islands should regularly gain a recognizable focal detail.
        double featureChance = Math.min(1.0, Math.max(0.0, density / 2.5));
        if (random.nextDouble() >= featureChance) {
            return;
        }

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int x = baseX + 4 + random.nextInt(8);
        int z = baseZ + 4 + random.nextInt(8);
        int y = FaeSurfaceLocator.find(info, region, x, z);
        if (y == Integer.MIN_VALUE || y + 10 >= info.getMaxHeight()) {
            return;
        }

        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
        if (!stable(info, region, x, y, z, 3)) {
            return;
        }

        switch (biome) {
            case GOLDEN_MEADOWS -> {
                if (random.nextBoolean()) {
                    placeFlowerCircle(region, x, y + 1, z);
                } else {
                    placeSunPool(region, x, y, z);
                }
            }
            case CRYSTAL_WOODS -> placeCrystalSpire(region, x, y + 1, z, random);
            case MIST_GARDENS -> placeMistMushrooms(region, x, y + 1, z, random);
            case ANCIENT_FAE_FOREST -> placeFallenAncient(region, x, y + 1, z, random);
            case SKY_HIGHLANDS -> placeStandingStones(region, x, y + 1, z, random);
        }
    }

    private void placeFlowerCircle(LimitedRegion region, int x, int y, int z) {
        int[][] ring = {
            {-3, 0}, {-2, -2}, {0, -3}, {2, -2}, {3, 0},
            {2, 2}, {0, 3}, {-2, 2}
        };
        Material[] flowers = {
            Material.DANDELION,
            Material.CORNFLOWER,
            Material.OXEYE_DAISY,
            Material.ALLIUM
        };
        for (int i = 0; i < ring.length; i++) {
            int px = x + ring[i][0];
            int pz = z + ring[i][1];
            if (region.isInRegion(px, y, pz) && region.getType(px, y, pz).isAir()) {
                region.setType(px, y, pz, flowers[i % flowers.length]);
            }
        }
        set(region, x, y, z, Material.GLOWSTONE);
        set(region, x, y + 1, z, Material.LIGHT);
    }

    private void placeSunPool(LimitedRegion region, int x, int surfaceY, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 2.25) {
                    continue;
                }
                set(region, x + dx, surfaceY, z + dz,
                    Math.abs(dx) <= 1 && Math.abs(dz) <= 1 ? Material.WATER : Material.SMOOTH_SANDSTONE);
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    set(region, x + dx, surfaceY - 1, z + dz, Material.GLOWSTONE);
                }
            }
        }
    }

    private void placeCrystalSpire(LimitedRegion region,
                                   int x,
                                   int y,
                                   int z,
                                   SplittableRandom random) {
        int height = 4 + random.nextInt(5);
        for (int dy = 0; dy < height; dy++) {
            Material material = dy < 2
                ? Material.CALCITE
                : (dy == height - 1 ? Material.AMETHYST_CLUSTER : Material.AMETHYST_BLOCK);
            set(region, x, y + dy, z, material);
        }
        set(region, x - 1, y, z, Material.BUDDING_AMETHYST);
        set(region, x + 1, y, z, Material.AMETHYST_BLOCK);
        set(region, x, y, z - 1, Material.CALCITE);
        set(region, x, y, z + 1, Material.CALCITE);
    }

    private void placeMistMushrooms(LimitedRegion region,
                                    int x,
                                    int y,
                                    int z,
                                    SplittableRandom random) {
        placeSmallMushroom(region, x, y, z, Material.RED_MUSHROOM_BLOCK, 3 + random.nextInt(2));
        placeSmallMushroom(region, x - 3, y, z + 2, Material.BROWN_MUSHROOM_BLOCK, 2 + random.nextInt(2));
        placeSmallMushroom(region, x + 3, y, z - 2, Material.RED_MUSHROOM_BLOCK, 2 + random.nextInt(2));
        set(region, x - 1, y, z + 3, Material.PALE_MOSS_CARPET);
        set(region, x + 2, y, z + 2, Material.PALE_MOSS_CARPET);
    }

    private void placeSmallMushroom(LimitedRegion region,
                                    int x,
                                    int y,
                                    int z,
                                    Material cap,
                                    int height) {
        for (int dy = 0; dy < height; dy++) {
            set(region, x, y + dy, z, Material.MUSHROOM_STEM);
        }
        int capY = y + height;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(region, x + dx, capY, z + dz, cap);
            }
        }
        set(region, x, capY + 1, z, cap);
    }

    private void placeFallenAncient(LimitedRegion region,
                                    int x,
                                    int y,
                                    int z,
                                    SplittableRandom random) {
        boolean alongX = random.nextBoolean();
        for (int offset = -4; offset <= 4; offset++) {
            int px = alongX ? x + offset : x;
            int pz = alongX ? z : z + offset;
            set(region, px, y, pz, Material.DARK_OAK_LOG);
            if ((offset & 1) == 0) {
                set(region, px, y + 1, pz, Material.MOSS_CARPET);
            }
        }
        set(region, x - 2, y, z - 2, Material.FERN);
        set(region, x + 2, y, z + 2, Material.LARGE_FERN);
    }

    private void placeStandingStones(LimitedRegion region,
                                     int x,
                                     int y,
                                     int z,
                                     SplittableRandom random) {
        int[][] stones = {{-3, 0}, {3, 0}, {0, -3}, {0, 3}};
        for (int i = 0; i < stones.length; i++) {
            int height = 3 + random.nextInt(4);
            Material material = (i & 1) == 0 ? Material.POLISHED_ANDESITE : Material.CALCITE;
            for (int dy = 0; dy < height; dy++) {
                set(region, x + stones[i][0], y + dy, z + stones[i][1], material);
            }
            set(region, x + stones[i][0], y + height, z + stones[i][1], Material.SEA_LANTERN);
        }
        set(region, x, y, z, Material.LODESTONE);
    }

    private boolean stable(WorldInfo info, LimitedRegion region, int x, int y, int z, int radius) {
        int[][] samples = {
            {0, 0}, {-radius, 0}, {radius, 0}, {0, -radius}, {0, radius},
            {-radius, -radius}, {-radius, radius}, {radius, -radius}, {radius, radius}
        };
        for (int[] sample : samples) {
            int sx = x + sample[0];
            int sz = z + sample[1];
            if (!region.isInRegion(sx, y, sz)) {
                return false;
            }
            int sy = FaeSurfaceLocator.find(info, region, sx, sz);
            if (sy == Integer.MIN_VALUE || Math.abs(sy - y) > 2) {
                return false;
            }
        }
        return true;
    }

    private void set(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) {
            region.setType(x, y, z, material);
        }
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
