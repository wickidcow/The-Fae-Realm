package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.SplittableRandom;

/**
 * Authored-feeling landscape compositions layered above terrain and flora.
 *
 * <p>This pass deliberately creates larger readable scenes instead of isolated
 * decorations: fallen colossi, root arches, occasional gap-spanning roots and
 * hanging garden shelves. The goal is the same visual principle used by strong
 * fantasy generators: objects and terrain should compose into landmarks.</p>
 */
public final class FaeLandscapePopulator {

    private static final long LANDSCAPE_SALT = 0x452821E638D01377L;
    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };

    public void populate(WorldInfo info,
                         int chunkX,
                         int chunkZ,
                         LimitedRegion region,
                         double density) {
        if (density <= 0.0) {
            return;
        }

        SplittableRandom random = new SplittableRandom(
            mixSeed(info.getSeed() ^ LANDSCAPE_SALT, chunkX, chunkZ));
        double chance = Math.min(0.48, 0.16 * Math.max(0.45, density));
        if (random.nextDouble() >= chance) {
            return;
        }

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int x = baseX + 3 + random.nextInt(10);
        int z = baseZ + 3 + random.nextInt(10);
        FaeRegionProfile profile = AetherChunkGenerator.regionProfileAt(info.getSeed(), x, z);
        int surfaceY = findSurface(info, region, x, z, profile.biome());
        if (surfaceY == Integer.MIN_VALUE) {
            return;
        }

        int choice = random.nextInt(100);
        if (choice < 34) {
            placeFallenColossus(region, x, surfaceY + 1, z, profile, random);
        } else if (choice < 62) {
            placeRootArch(region, x, surfaceY + 1, z, profile, random);
        } else if (choice < 80) {
            if (!tryPlaceGapRootBridge(info, region, x, surfaceY, z, profile, random)) {
                placeRootArch(region, x, surfaceY + 1, z, profile, random);
            }
        } else {
            placeHangingGarden(info, region, x, surfaceY, z, profile, random);
        }
    }

    private void placeFallenColossus(LimitedRegion region,
                                     int x,
                                     int y,
                                     int z,
                                     FaeRegionProfile profile,
                                     SplittableRandom random) {
        int[] direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        int length = 18 + random.nextInt(15);
        Material log = logFor(profile);
        Material leaves = leavesFor(profile);

        int start = -(length / 3);
        for (int step = start; step < start + length; step++) {
            int px = x + direction[0] * step;
            int pz = z + direction[1] * step;
            int py = y + (int) Math.round(Math.sin((step - start) * 0.30) * 1.5);
            placeDisc(region, px, py, pz, 1, log);

            if (step % 5 == 0) {
                int sx = -direction[1];
                int sz = direction[0];
                int side = random.nextBoolean() ? 1 : -1;
                int branchLength = 3 + random.nextInt(5);
                for (int branch = 1; branch <= branchLength; branch++) {
                    int bx = px + sx * side * branch;
                    int bz = pz + sz * side * branch;
                    int by = py + branch / 3;
                    set(region, bx, by, bz, log);
                }
                placeLeafCloud(region,
                    px + sx * side * branchLength,
                    py + branchLength / 3 + 1,
                    pz + sz * side * branchLength,
                    3,
                    leaves,
                    random);
            }
        }

        int rootX = x + direction[0] * start;
        int rootZ = z + direction[1] * start;
        for (int[] rootDirection : DIRECTIONS) {
            if (random.nextDouble() < 0.78) {
                int rootLength = 4 + random.nextInt(5);
                int ry = y;
                for (int step = 1; step <= rootLength; step++) {
                    if ((step & 1) == 0) {
                        ry--;
                    }
                    set(region,
                        rootX + rootDirection[0] * step,
                        ry,
                        rootZ + rootDirection[1] * step,
                        step < 3 ? log : rootFor(profile));
                }
            }
        }

        for (int i = 0; i < 16; i++) {
            int mx = x + random.nextInt(-8, 9);
            int mz = z + random.nextInt(-8, 9);
            int my = y + random.nextInt(0, 3);
            if (isAir(region, mx, my, mz)) {
                set(region, mx, my, mz,
                    random.nextBoolean() ? Material.MOSS_CARPET : Material.BROWN_MUSHROOM);
            }
        }
    }

    private void placeRootArch(LimitedRegion region,
                               int x,
                               int y,
                               int z,
                               FaeRegionProfile profile,
                               SplittableRandom random) {
        int[] direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        int sideX = -direction[1];
        int sideZ = direction[0];
        int halfWidth = 5 + random.nextInt(4);
        int height = 7 + random.nextInt(6);
        Material log = logFor(profile);

        for (int step = -halfWidth; step <= halfWidth; step++) {
            double normalized = Math.abs(step) / (double) halfWidth;
            int py = y + (int) Math.round((1.0 - normalized * normalized) * height);
            int px = x + sideX * step;
            int pz = z + sideZ * step;
            placeDisc(region, px, py, pz, step == -halfWidth || step == halfWidth ? 2 : 1, log);

            if ((step & 1) == 0 && random.nextDouble() < 0.72) {
                int vineLength = 3 + random.nextInt(8);
                for (int drop = 1; drop <= vineLength; drop++) {
                    if (!isAir(region, px, py - drop, pz)) {
                        break;
                    }
                    set(region, px, py - drop, pz, Material.VINE);
                }
            }
        }

        int leftX = x - sideX * halfWidth;
        int leftZ = z - sideZ * halfWidth;
        int rightX = x + sideX * halfWidth;
        int rightZ = z + sideZ * halfWidth;
        placeRootFan(region, leftX, y, leftZ, log, rootFor(profile), random);
        placeRootFan(region, rightX, y, rightZ, log, rootFor(profile), random);
        placeLeafCloud(region, x, y + height, z, 4, leavesFor(profile), random);
    }

    private boolean tryPlaceGapRootBridge(WorldInfo info,
                                          LimitedRegion region,
                                          int x,
                                          int surfaceY,
                                          int z,
                                          FaeRegionProfile profile,
                                          SplittableRandom random) {
        int[] direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        int targetDistance = 12 + random.nextInt(13);
        int targetX = x + direction[0] * targetDistance;
        int targetZ = z + direction[1] * targetDistance;
        int targetY = findSurface(info, region, targetX, targetZ, profile.biome());
        if (targetY == Integer.MIN_VALUE || Math.abs(targetY - surfaceY) > 7) {
            return false;
        }

        int gapColumns = 0;
        for (int step = 3; step <= targetDistance - 3; step++) {
            int px = x + direction[0] * step;
            int pz = z + direction[1] * step;
            int checkY = (int) Math.round(surfaceY + (targetY - surfaceY) * (step / (double) targetDistance));
            boolean open = true;
            for (int down = 0; down <= 5; down++) {
                if (region.isInRegion(px, checkY - down, pz)
                    && !region.getType(px, checkY - down, pz).isAir()) {
                    open = false;
                    break;
                }
            }
            if (open) {
                gapColumns++;
            }
        }
        if (gapColumns < 4) {
            return false;
        }

        Material log = logFor(profile);
        for (int step = 0; step <= targetDistance; step++) {
            double t = step / (double) targetDistance;
            double arch = Math.sin(Math.PI * t) * (3.0 + targetDistance * 0.10);
            int px = x + direction[0] * step;
            int pz = z + direction[1] * step;
            int py = (int) Math.round(surfaceY + 1 + (targetY - surfaceY) * t + arch);
            placeDisc(region, px, py, pz, step < 3 || step > targetDistance - 3 ? 1 : 0, log);

            if (step > 2 && step < targetDistance - 2 && step % 3 == 0) {
                int vineLength = 2 + random.nextInt(7);
                for (int drop = 1; drop <= vineLength; drop++) {
                    if (!isAir(region, px, py - drop, pz)) {
                        break;
                    }
                    set(region, px, py - drop, pz, Material.VINE);
                }
            }
        }
        return true;
    }

    private void placeHangingGarden(WorldInfo info,
                                    LimitedRegion region,
                                    int x,
                                    int surfaceY,
                                    int z,
                                    FaeRegionProfile profile,
                                    SplittableRandom random) {
        int gardenY = surfaceY - 4 - random.nextInt(5);
        int radius = 4 + random.nextInt(3);
        Material base = profile.biome() == FaeRealmBiome.MIST_GARDENS
            ? Material.PALE_MOSS_BLOCK : Material.MOSS_BLOCK;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius + 0.25) {
                    continue;
                }
                int py = gardenY - (int) Math.floor(distance * 0.35);
                if (isAir(region, x + dx, py, z + dz)) {
                    set(region, x + dx, py, z + dz, base);
                }
                if (distance > radius - 1.6 && random.nextDouble() < 0.45) {
                    int vineLength = 4 + random.nextInt(11);
                    for (int drop = 1; drop <= vineLength; drop++) {
                        if (py - drop <= info.getMinHeight() + 1
                            || !isAir(region, x + dx, py - drop, z + dz)) {
                            break;
                        }
                        set(region, x + dx, py - drop, z + dz, Material.VINE);
                    }
                }
            }
        }

        setIfAir(region, x, gardenY + 1, z,
            profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM ? Material.SOUL_LANTERN : Material.SHROOMLIGHT);
        for (int i = 0; i < 10; i++) {
            int px = x + random.nextInt(-radius + 1, radius);
            int pz = z + random.nextInt(-radius + 1, radius);
            int py = gardenY + 1;
            if (isAir(region, px, py, pz)) {
                Material plant = switch (profile.biome()) {
                    case GOLDEN_MEADOWS -> random.nextBoolean() ? Material.ALLIUM : Material.CORNFLOWER;
                    case CRYSTAL_WOODS -> random.nextBoolean() ? Material.FLOWERING_AZALEA : Material.PINK_PETALS;
                    case MIST_GARDENS -> random.nextBoolean() ? Material.BLUE_ORCHID : Material.RED_MUSHROOM;
                    case ANCIENT_FAE_FOREST -> random.nextBoolean() ? Material.FERN : Material.BROWN_MUSHROOM;
                    case SKY_HIGHLANDS -> random.nextBoolean() ? Material.AZURE_BLUET : Material.FERN;
                };
                set(region, px, py, pz, plant);
            }
        }
    }

    private void placeRootFan(LimitedRegion region,
                              int x,
                              int y,
                              int z,
                              Material log,
                              Material root,
                              SplittableRandom random) {
        for (int[] direction : DIRECTIONS) {
            if (random.nextDouble() > 0.82) {
                continue;
            }
            int length = 3 + random.nextInt(5);
            int ry = y;
            for (int step = 1; step <= length; step++) {
                if ((step & 1) == 0) {
                    ry--;
                }
                set(region,
                    x + direction[0] * step,
                    ry,
                    z + direction[1] * step,
                    step < 3 ? log : root);
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
                    if (dx * dx + dz * dz + dy * dy * 1.7 <= layerRadius * layerRadius + 2.0
                        && random.nextDouble() > 0.11) {
                        setIfAir(region, x + dx, y + dy, z + dz, leaves);
                    }
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

    private Material logFor(FaeRegionProfile profile) {
        if (profile.anomaly() == FaeRegionProfile.Anomaly.WILDBLOOM) {
            return Material.CHERRY_LOG;
        }
        return switch (profile.biome()) {
            case GOLDEN_MEADOWS -> Material.OAK_LOG;
            case CRYSTAL_WOODS -> Material.CHERRY_LOG;
            case MIST_GARDENS -> Material.PALE_OAK_LOG;
            case ANCIENT_FAE_FOREST -> Material.DARK_OAK_LOG;
            case SKY_HIGHLANDS -> Material.BIRCH_LOG;
        };
    }

    private Material leavesFor(FaeRegionProfile profile) {
        if (profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM) {
            return Material.DARK_OAK_LEAVES;
        }
        if (profile.anomaly() == FaeRegionProfile.Anomaly.WILDBLOOM) {
            return Material.FLOWERING_AZALEA_LEAVES;
        }
        return switch (profile.biome()) {
            case GOLDEN_MEADOWS -> Material.FLOWERING_AZALEA_LEAVES;
            case CRYSTAL_WOODS -> Material.CHERRY_LEAVES;
            case MIST_GARDENS -> Material.PALE_OAK_LEAVES;
            case ANCIENT_FAE_FOREST -> Material.DARK_OAK_LEAVES;
            case SKY_HIGHLANDS -> Material.AZALEA_LEAVES;
        };
    }

    private Material rootFor(FaeRegionProfile profile) {
        if (profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM) {
            return Material.SCULK;
        }
        return profile.biome() == FaeRealmBiome.MIST_GARDENS
            ? Material.PALE_MOSS_BLOCK
            : Material.MANGROVE_ROOTS;
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
