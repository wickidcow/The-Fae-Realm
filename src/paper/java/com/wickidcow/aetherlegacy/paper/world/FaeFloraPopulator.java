package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.SplittableRandom;

/**
 * Deterministic Fae ecology pass for trees, understory and magical ground growth.
 *
 * <p>All placement is restricted to Paper's {@link LimitedRegion}; no chunks are
 * force-loaded and no Bukkit world access is performed from generation threads.</p>
 */
public final class FaeFloraPopulator {

    private static final long FLORA_SALT = 0xB7E151628AED2A6BL;

    public void populate(@NotNull WorldInfo info,
                         int chunkX,
                         int chunkZ,
                         @NotNull LimitedRegion region,
                         @NotNull FaeGeneratorSettings settings) {
        double density = settings.decorationDensity();
        if (density <= 0.0) {
            return;
        }

        SplittableRandom random = new SplittableRandom(
            mixSeed(info.getSeed() ^ FLORA_SALT, chunkX, chunkZ));
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        // Place the larger tree silhouettes before understory so ground plants
        // cannot become the highest block and accidentally suppress a tree site.
        int treeAttempts = scaledAttempts(5 + random.nextInt(4), density, random);
        for (int i = 0; i < treeAttempts; i++) {
            int x = baseX + 3 + random.nextInt(10);
            int z = baseZ + 3 + random.nextInt(10);
            int surfaceY = FaeSurfaceLocator.find(info, region, x, z);
            if (surfaceY == Integer.MIN_VALUE
                || surfaceY + 18 >= info.getMaxHeight()) {
                continue;
            }

            FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
            if (random.nextDouble() > treeChance(biome)) {
                continue;
            }

            int y = surfaceY + 1;
            if (!isAir(region, x, y, z)) {
                continue;
            }

            switch (biome) {
                case GOLDEN_MEADOWS -> placeSunCrown(region, random, x, y, z);
                case CRYSTAL_WOODS -> placeCrystalTree(region, random, x, y, z);
                case MIST_GARDENS -> placeMistWillow(region, random, x, y, z);
                case ANCIENT_FAE_FOREST -> placeTwistedAncient(region, random, x, y, z);
                case SKY_HIGHLANDS -> placeWindBirch(region, random, x, y, z);
            }
        }

        int groundAttempts = scaledAttempts(42 + random.nextInt(23), density, random);
        for (int i = 0; i < groundAttempts; i++) {
            int x = baseX + random.nextInt(16);
            int z = baseZ + random.nextInt(16);
            int surfaceY = FaeSurfaceLocator.find(info, region, x, z);
            if (surfaceY == Integer.MIN_VALUE) {
                continue;
            }
            placeGroundGrowth(info, region, random, x, surfaceY + 1, z);
        }
    }

    private double treeChance(FaeRealmBiome biome) {
        return switch (biome) {
            case GOLDEN_MEADOWS -> 0.55;
            case CRYSTAL_WOODS -> 0.90;
            case MIST_GARDENS -> 0.78;
            case ANCIENT_FAE_FOREST -> 0.96;
            case SKY_HIGHLANDS -> 0.64;
        };
    }

    private void placeGroundGrowth(WorldInfo info,
                                   LimitedRegion region,
                                   SplittableRandom random,
                                   int x,
                                   int y,
                                   int z) {
        if (!isAir(region, x, y, z)) {
            return;
        }

        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
        Material material = switch (biome) {
            case GOLDEN_MEADOWS -> pick(random,
                Material.SHORT_GRASS, Material.DANDELION, Material.OXEYE_DAISY,
                Material.ALLIUM, Material.CORNFLOWER, Material.PINK_PETALS);
            case CRYSTAL_WOODS -> pick(random,
                Material.PINK_PETALS, Material.SHORT_GRASS, Material.FLOWERING_AZALEA,
                Material.AZALEA, Material.ALLIUM, Material.PALE_MOSS_CARPET);
            case MIST_GARDENS -> pick(random,
                Material.PALE_MOSS_CARPET, Material.SHORT_GRASS, Material.FERN,
                Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.PALE_HANGING_MOSS);
            case ANCIENT_FAE_FOREST -> pick(random,
                Material.FERN, Material.MOSS_CARPET, Material.SHORT_GRASS,
                Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.AZALEA);
            case SKY_HIGHLANDS -> pick(random,
                Material.SHORT_GRASS, Material.AZURE_BLUET, Material.CORNFLOWER,
                Material.OXEYE_DAISY, Material.FERN, Material.PINK_PETALS);
        };

        if (material == Material.PALE_HANGING_MOSS) {
            material = Material.PALE_MOSS_CARPET;
        }
        setIfAir(region, x, y, z, material);
    }

    private void placeSunCrown(LimitedRegion region,
                               SplittableRandom random,
                               int x,
                               int y,
                               int z) {
        int height = 6 + random.nextInt(3);
        int trunkX = x;
        int trunkZ = z;
        for (int dy = 0; dy < height; dy++) {
            if (dy >= height / 2 && dy % 2 == 0 && random.nextDouble() < 0.34) {
                trunkX += random.nextInt(-1, 2);
                trunkZ += random.nextInt(-1, 2);
            }
            set(region, trunkX, y + dy, trunkZ, Material.OAK_LOG);
        }

        int crownY = y + height - 1;
        addBranches(region, random, trunkX, crownY - 1, trunkZ, Material.OAK_WOOD, 3, 2, 3);
        placeCanopy(region, random, trunkX, crownY + 1, trunkZ,
            Material.OAK_LEAVES, Material.FLOWERING_AZALEA_LEAVES, 3, 2, 0.18);
        setIfAir(region, trunkX, crownY + 2, trunkZ, Material.SHROOMLIGHT);
    }

    private void placeCrystalTree(LimitedRegion region,
                                  SplittableRandom random,
                                  int x,
                                  int y,
                                  int z) {
        int height = 7 + random.nextInt(4);
        int trunkX = x;
        int trunkZ = z;
        int leanX = random.nextInt(-1, 2);
        int leanZ = leanX == 0 ? (random.nextBoolean() ? 1 : -1) : random.nextInt(-1, 2);

        for (int dy = 0; dy < height; dy++) {
            if (dy > 2 && dy % 3 == 0) {
                trunkX += leanX;
                trunkZ += leanZ;
            }
            set(region, trunkX, y + dy, trunkZ, Material.CHERRY_LOG);
        }

        int crownY = y + height - 1;
        addBranches(region, random, trunkX, crownY - 2, trunkZ, Material.CHERRY_WOOD, 4, 2, 4);
        placeCanopy(region, random, trunkX, crownY + 1, trunkZ,
            Material.CHERRY_LEAVES, Material.FLOWERING_AZALEA_LEAVES, 4, 2, 0.10);

        for (int i = 0; i < 3; i++) {
            int ox = random.nextInt(-3, 4);
            int oz = random.nextInt(-3, 4);
            int oy = crownY + random.nextInt(0, 3);
            if (Math.abs(ox) + Math.abs(oz) >= 2) {
                setIfAir(region, trunkX + ox, oy, trunkZ + oz,
                    random.nextBoolean() ? Material.AMETHYST_BLOCK : Material.CALCITE);
            }
        }
    }

    private void placeMistWillow(LimitedRegion region,
                                 SplittableRandom random,
                                 int x,
                                 int y,
                                 int z) {
        int height = 7 + random.nextInt(3);
        for (int dy = 0; dy < height; dy++) {
            set(region, x, y + dy, z, Material.PALE_OAK_LOG);
        }

        int crownY = y + height - 1;
        addBranches(region, random, x, crownY - 2, z, Material.PALE_OAK_WOOD, 4, 3, 4);
        placeCanopy(region, random, x, crownY, z,
            Material.PALE_OAK_LEAVES, Material.AZALEA_LEAVES, 4, 2, 0.08);

        for (int i = 0; i < 14; i++) {
            int ox = random.nextInt(-4, 5);
            int oz = random.nextInt(-4, 5);
            if (Math.abs(ox) + Math.abs(oz) < 3) {
                continue;
            }
            int startY = crownY + random.nextInt(-1, 2);
            if (!region.isInRegion(x + ox, startY, z + oz)
                || !isLeaves(region.getType(x + ox, startY, z + oz))) {
                continue;
            }
            int length = 1 + random.nextInt(4);
            for (int d = 1; d <= length; d++) {
                if (!setIfAir(region, x + ox, startY - d, z + oz, Material.PALE_HANGING_MOSS)) {
                    break;
                }
            }
        }
    }

    private void placeTwistedAncient(LimitedRegion region,
                                     SplittableRandom random,
                                     int x,
                                     int y,
                                     int z) {
        int height = 9 + random.nextInt(5);
        int trunkX = x;
        int trunkZ = z;
        int bendX = random.nextInt(-1, 2);
        int bendZ = bendX == 0 ? (random.nextBoolean() ? 1 : -1) : random.nextInt(-1, 2);

        set(region, x + 1, y, z, Material.DARK_OAK_LOG);
        set(region, x, y, z + 1, Material.DARK_OAK_LOG);
        set(region, x - 1, y, z, Material.DARK_OAK_WOOD);
        set(region, x, y, z - 1, Material.DARK_OAK_WOOD);

        for (int dy = 0; dy < height; dy++) {
            if (dy > 2 && dy % 3 == 0) {
                if (random.nextDouble() < 0.72) {
                    trunkX += bendX;
                    trunkZ += bendZ;
                }
                if (random.nextDouble() < 0.28) {
                    bendX = random.nextInt(-1, 2);
                    bendZ = random.nextInt(-1, 2);
                }
            }
            set(region, trunkX, y + dy, trunkZ, Material.DARK_OAK_LOG);
            if (dy < height / 2 && dy % 2 == 0) {
                setIfAir(region, trunkX + 1, y + dy, trunkZ, Material.DARK_OAK_WOOD);
            }
        }

        int crownY = y + height - 1;
        addBranches(region, random, trunkX, crownY - 3, trunkZ, Material.DARK_OAK_WOOD, 5, 3, 5);
        placeCanopy(region, random, trunkX, crownY, trunkZ,
            Material.DARK_OAK_LEAVES, Material.AZALEA_LEAVES, 4, 3, 0.16);

        for (int i = 0; i < 5; i++) {
            int ox = random.nextInt(-3, 4);
            int oz = random.nextInt(-3, 4);
            setIfAir(region, x + ox, y, z + oz,
                random.nextBoolean() ? Material.FERN : Material.MOSS_CARPET);
        }
    }

    private void placeWindBirch(LimitedRegion region,
                                SplittableRandom random,
                                int x,
                                int y,
                                int z) {
        int height = 6 + random.nextInt(4);
        int dx = random.nextBoolean() ? (random.nextBoolean() ? 1 : -1) : 0;
        int dz = dx == 0 ? (random.nextBoolean() ? 1 : -1) : 0;
        int trunkX = x;
        int trunkZ = z;

        for (int dy = 0; dy < height; dy++) {
            if (dy >= 2 && dy % 2 == 0) {
                trunkX += dx;
                trunkZ += dz;
            }
            set(region, trunkX, y + dy, trunkZ, Material.BIRCH_LOG);
        }

        int crownY = y + height - 1;
        addBranches(region, random, trunkX, crownY - 1, trunkZ, Material.BIRCH_WOOD, 3, 2, 4);
        int canopyX = trunkX + dx * 2;
        int canopyZ = trunkZ + dz * 2;
        placeCanopy(region, random, canopyX, crownY + 1, canopyZ,
            Material.BIRCH_LEAVES, Material.FLOWERING_AZALEA_LEAVES, 3, 2, 0.06);
    }

    private void addBranches(LimitedRegion region,
                             SplittableRandom random,
                             int x,
                             int y,
                             int z,
                             Material wood,
                             int count,
                             int minLength,
                             int maxLength) {
        int[][] directions = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {-1, -1}, {1, -1}, {-1, 1}
        };
        for (int branch = 0; branch < count; branch++) {
            int[] direction = directions[random.nextInt(directions.length)];
            int length = minLength + random.nextInt(maxLength - minLength + 1);
            int riseAt = 1 + random.nextInt(Math.max(1, length));
            for (int step = 1; step <= length; step++) {
                int py = y + (step >= riseAt ? 1 : 0);
                set(region, x + direction[0] * step, py, z + direction[1] * step, wood);
            }
        }
    }

    private void placeCanopy(LimitedRegion region,
                             SplittableRandom random,
                             int x,
                             int y,
                             int z,
                             Material primary,
                             Material accent,
                             int radius,
                             int verticalRadius,
                             double accentChance) {
        for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
            double verticalFactor = 1.0 - (Math.abs(dy) / (double) (verticalRadius + 1));
            int layerRadius = Math.max(1, (int) Math.round(radius * verticalFactor));
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > layerRadius + 0.25 || random.nextDouble() < 0.10) {
                        continue;
                    }
                    Material leaves = random.nextDouble() < accentChance ? accent : primary;
                    setIfAir(region, x + dx, y + dy, z + dz, leaves);
                }
            }
        }
    }

    private Material pick(SplittableRandom random, Material... choices) {
        return choices[random.nextInt(choices.length)];
    }

    private int scaledAttempts(int baseAttempts, double density, SplittableRandom random) {
        double expected = Math.max(0.0, baseAttempts * density);
        int whole = (int) Math.floor(expected);
        return whole + (random.nextDouble() < expected - whole ? 1 : 0);
    }

    private boolean setIfAir(LimitedRegion region, int x, int y, int z, Material material) {
        if (!region.isInRegion(x, y, z) || !region.getType(x, y, z).isAir()) {
            return false;
        }
        region.setType(x, y, z, material);
        return true;
    }

    private void set(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) {
            region.setType(x, y, z, material);
        }
    }

    private boolean isAir(LimitedRegion region, int x, int y, int z) {
        return region.isInRegion(x, y, z) && region.getType(x, y, z).isAir();
    }

    private boolean isLeaves(Material material) {
        return material.name().endsWith("_LEAVES");
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
