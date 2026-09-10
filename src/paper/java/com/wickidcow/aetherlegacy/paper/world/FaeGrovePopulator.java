package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.SplittableRandom;

/**
 * Deterministic clustered ecology scenes for the Fae worlds.
 *
 * <p>Unlike the landmark and ordinary flora layers, groves intentionally repeat
 * related shapes inside a small area. That creates the authored ecosystem effect
 * seen in strong fantasy generators: mushroom colonies, bloom thickets, root
 * gardens and small enchanted copses rather than isolated one-off objects.</p>
 */
public final class FaeGrovePopulator {

    private static final long GROVE_SALT = 0xBE5466CF34E90C6CL;

    public void populate(WorldInfo info,
                         int chunkX,
                         int chunkZ,
                         LimitedRegion region,
                         double density) {
        if (density <= 0.0) {
            return;
        }

        SplittableRandom random = new SplittableRandom(
            mixSeed(info.getSeed() ^ GROVE_SALT, chunkX, chunkZ));
        double chance = Math.min(0.42, 0.13 * Math.max(0.5, density));
        if (random.nextDouble() >= chance) {
            return;
        }

        int x = (chunkX << 4) + 4 + random.nextInt(8);
        int z = (chunkZ << 4) + 4 + random.nextInt(8);
        FaeRegionProfile profile = AetherChunkGenerator.regionProfileAt(info.getSeed(), x, z);
        int surfaceY = findSurface(info, region, x, z, profile.biome());
        if (surfaceY == Integer.MIN_VALUE) {
            return;
        }

        if (profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM
            || profile.biome() == FaeRealmBiome.MIST_GARDENS) {
            placeMushroomColony(info, region, x, surfaceY + 1, z, profile, random);
        } else if (profile.anomaly() == FaeRegionProfile.Anomaly.WILDBLOOM
            || profile.biome() == FaeRealmBiome.CRYSTAL_WOODS
            || profile.biome() == FaeRealmBiome.GOLDEN_MEADOWS) {
            placeBloomThicket(info, region, x, surfaceY + 1, z, profile, random);
        } else {
            placeAncientCopse(info, region, x, surfaceY + 1, z, profile, random);
        }
    }

    private void placeMushroomColony(WorldInfo info,
                                     LimitedRegion region,
                                     int centerX,
                                     int baseY,
                                     int centerZ,
                                     FaeRegionProfile profile,
                                     SplittableRandom random) {
        int count = 4 + random.nextInt(5);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = 2 + random.nextInt(8);
            int x = centerX + (int) Math.round(Math.cos(angle) * distance);
            int z = centerZ + (int) Math.round(Math.sin(angle) * distance);
            int surface = findSurface(info, region, x, z, profile.biome());
            if (surface == Integer.MIN_VALUE) {
                continue;
            }

            int height = 5 + random.nextInt(9);
            int radius = 2 + random.nextInt(3);
            if (i == 0) {
                height += 5 + random.nextInt(6);
                radius++;
            }
            placeMushroom(region, x, surface + 1, z, height, radius,
                random.nextBoolean() ? Material.RED_MUSHROOM_BLOCK : Material.BROWN_MUSHROOM_BLOCK,
                profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM,
                random);
        }

        int carpet = 24 + random.nextInt(18);
        for (int i = 0; i < carpet; i++) {
            int x = centerX + random.nextInt(-9, 10);
            int z = centerZ + random.nextInt(-9, 10);
            int surface = findSurface(info, region, x, z, profile.biome());
            if (surface == Integer.MIN_VALUE || !isAir(region, x, surface + 1, z)) {
                continue;
            }
            Material growth = random.nextInt(5) == 0
                ? Material.PALE_MOSS_CARPET
                : (random.nextBoolean() ? Material.RED_MUSHROOM : Material.BROWN_MUSHROOM);
            set(region, x, surface + 1, z, growth);
        }
    }

    private void placeBloomThicket(WorldInfo info,
                                   LimitedRegion region,
                                   int centerX,
                                   int baseY,
                                   int centerZ,
                                   FaeRegionProfile profile,
                                   SplittableRandom random) {
        int count = 5 + random.nextInt(6);
        for (int i = 0; i < count; i++) {
            int x = centerX + random.nextInt(-8, 9);
            int z = centerZ + random.nextInt(-8, 9);
            int surface = findSurface(info, region, x, z, profile.biome());
            if (surface == Integer.MIN_VALUE) {
                continue;
            }

            if (i < 2 && random.nextDouble() < 0.72) {
                placeBloomSapling(region, x, surface + 1, z, 8 + random.nextInt(7), random);
            } else {
                placeOversizedFlower(region, x, surface + 1, z, 4 + random.nextInt(5), random);
            }
        }

        int flowers = 30 + random.nextInt(24);
        for (int i = 0; i < flowers; i++) {
            int x = centerX + random.nextInt(-10, 11);
            int z = centerZ + random.nextInt(-10, 11);
            int surface = findSurface(info, region, x, z, profile.biome());
            if (surface == Integer.MIN_VALUE || !isAir(region, x, surface + 1, z)) {
                continue;
            }
            Material growth = switch (random.nextInt(7)) {
                case 0 -> Material.PINK_PETALS;
                case 1 -> Material.FLOWERING_AZALEA;
                case 2 -> Material.ALLIUM;
                case 3 -> Material.CORNFLOWER;
                case 4 -> Material.OXEYE_DAISY;
                default -> Material.MOSS_CARPET;
            };
            set(region, x, surface + 1, z, growth);
        }
    }

    private void placeAncientCopse(WorldInfo info,
                                   LimitedRegion region,
                                   int centerX,
                                   int baseY,
                                   int centerZ,
                                   FaeRegionProfile profile,
                                   SplittableRandom random) {
        Material log = profile.biome() == FaeRealmBiome.SKY_HIGHLANDS
            ? Material.BIRCH_LOG : Material.DARK_OAK_LOG;
        Material leaves = profile.biome() == FaeRealmBiome.SKY_HIGHLANDS
            ? Material.AZALEA_LEAVES : Material.DARK_OAK_LEAVES;

        int count = 3 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            int x = centerX + random.nextInt(-7, 8);
            int z = centerZ + random.nextInt(-7, 8);
            int surface = findSurface(info, region, x, z, profile.biome());
            if (surface == Integer.MIN_VALUE) {
                continue;
            }
            placeCrookedTree(region, x, surface + 1, z, 10 + random.nextInt(9), log, leaves, random);
        }

        for (int i = 0; i < 26; i++) {
            int x = centerX + random.nextInt(-9, 10);
            int z = centerZ + random.nextInt(-9, 10);
            int surface = findSurface(info, region, x, z, profile.biome());
            if (surface == Integer.MIN_VALUE || !isAir(region, x, surface + 1, z)) {
                continue;
            }
            set(region, x, surface + 1, z,
                random.nextInt(4) == 0 ? Material.BROWN_MUSHROOM : Material.FERN);
        }
    }

    private void placeMushroom(LimitedRegion region,
                               int x,
                               int y,
                               int z,
                               int height,
                               int radius,
                               Material cap,
                               boolean gloam,
                               SplittableRandom random) {
        int stemRadius = height >= 14 ? 1 : 0;
        for (int dy = 0; dy < height; dy++) {
            placeDisc(region, x, y + dy, z, stemRadius, Material.MUSHROOM_STEM);
        }

        int capY = y + height;
        for (int dy = -1; dy <= 2; dy++) {
            int layerRadius = Math.max(1, radius - Math.max(0, dy));
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    double d = dx * dx + dz * dz;
                    if (d <= layerRadius * layerRadius + 1.5 && random.nextDouble() > 0.08) {
                        setIfAir(region, x + dx, capY + dy, z + dz, cap);
                    }
                }
            }
        }
        setIfAir(region, x, capY - 1, z, gloam ? Material.SOUL_LANTERN : Material.SHROOMLIGHT);
    }

    private void placeBloomSapling(LimitedRegion region,
                                   int x,
                                   int y,
                                   int z,
                                   int height,
                                   SplittableRandom random) {
        int tx = x;
        int tz = z;
        for (int dy = 0; dy < height; dy++) {
            set(region, tx, y + dy, tz, Material.CHERRY_LOG);
            if (dy > 3 && dy < height - 2 && dy % 4 == 0 && random.nextBoolean()) {
                tx += random.nextBoolean() ? 1 : -1;
            }
        }
        placeLeafCloud(region, tx, y + height - 1, tz, 3 + random.nextInt(2),
            Material.FLOWERING_AZALEA_LEAVES, random);
        if (random.nextBoolean()) {
            set(region, tx, y + height - 1, tz, Material.SHROOMLIGHT);
        }
    }

    private void placeOversizedFlower(LimitedRegion region,
                                      int x,
                                      int y,
                                      int z,
                                      int height,
                                      SplittableRandom random) {
        for (int dy = 0; dy < height; dy++) {
            set(region, x, y + dy, z, Material.BAMBOO_BLOCK);
        }
        int fy = y + height;
        set(region, x, fy, z, Material.SHROOMLIGHT);
        Material petal = random.nextBoolean() ? Material.PINK_WOOL : Material.MAGENTA_WOOL;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 3 && !(dx == 0 && dz == 0)) {
                    setIfAir(region, x + dx, fy, z + dz, petal);
                }
            }
        }
    }

    private void placeCrookedTree(LimitedRegion region,
                                  int x,
                                  int y,
                                  int z,
                                  int height,
                                  Material log,
                                  Material leaves,
                                  SplittableRandom random) {
        int tx = x;
        int tz = z;
        for (int dy = 0; dy < height; dy++) {
            set(region, tx, y + dy, tz, log);
            if (dy > 3 && dy < height - 3 && dy % 4 == 0) {
                if (random.nextBoolean()) {
                    tx += random.nextBoolean() ? 1 : -1;
                } else {
                    tz += random.nextBoolean() ? 1 : -1;
                }
            }
        }
        placeLeafCloud(region, tx, y + height - 2, tz, 4, leaves, random);
        for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            if (random.nextDouble() < 0.72) {
                for (int step = 1; step <= 4; step++) {
                    set(region, x + dir[0] * step, y - step / 2, z + dir[1] * step,
                        step <= 2 ? log : Material.MOSS_BLOCK);
                }
            }
        }
    }

    private void placeDisc(LimitedRegion region,
                           int x,
                           int y,
                           int z,
                           int radius,
                           Material material) {
        if (radius <= 0) {
            set(region, x, y, z, material);
            return;
        }
        double limit = radius * radius + 0.65;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= limit) {
                    set(region, x + dx, y, z + dz, material);
                }
            }
        }
    }

    private void placeLeafCloud(LimitedRegion region,
                                int x,
                                int y,
                                int z,
                                int radius,
                                Material leaves,
                                SplittableRandom random) {
        for (int dy = -2; dy <= 2; dy++) {
            int layerRadius = Math.max(2, radius - Math.max(0, Math.abs(dy) - 1));
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    if (dx * dx + dz * dz + dy * dy * 1.6 <= layerRadius * layerRadius + 2.0
                        && random.nextDouble() > 0.10) {
                        setIfAir(region, x + dx, y + dy, z + dz, leaves);
                    }
                }
            }
        }
    }

    private int findSurface(WorldInfo info,
                            LimitedRegion region,
                            int x,
                            int z,
                            FaeRealmBiome biome) {
        for (int y = info.getMaxHeight() - 2; y >= info.getMinHeight(); y--) {
            if (region.isInRegion(x, y, z) && region.getType(x, y, z) == biome.surface()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private boolean isAir(LimitedRegion region, int x, int y, int z) {
        return region.isInRegion(x, y, z) && region.getType(x, y, z).isAir();
    }

    private void set(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) {
            region.setType(x, y, z, material);
        }
    }

    private void setIfAir(LimitedRegion region, int x, int y, int z, Material material) {
        if (isAir(region, x, y, z)) {
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
