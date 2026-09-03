package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.SplittableRandom;

/** Rare oversized deterministic landmarks for long-range exploration. */
public final class FaeLandmarkPopulator {
    private static final long LANDMARK_SALT = 0x3BD39E10CB0EF593L;

    public boolean populate(@NotNull WorldInfo info, int chunkX, int chunkZ,
                            @NotNull LimitedRegion region, @NotNull FaeGeneratorSettings settings) {
        if (Math.abs(chunkX) <= 6 && Math.abs(chunkZ) <= 6) return false;

        int spacing = settings.landmarkSpacingChunks();
        int cellX = Math.floorDiv(chunkX, spacing);
        int cellZ = Math.floorDiv(chunkZ, spacing);
        SplittableRandom random = new SplittableRandom(mixSeed(info.getSeed() ^ LANDMARK_SALT, cellX, cellZ));
        int anchorX = cellX * spacing + random.nextInt(2, spacing - 2);
        int anchorZ = cellZ * spacing + random.nextInt(2, spacing - 2);
        if (chunkX != anchorX || chunkZ != anchorZ) return false;

        int x = (chunkX << 4) + 8;
        int z = (chunkZ << 4) + 8;
        int y = FaeSurfaceLocator.find(info, region, x, z);
        if (y == Integer.MIN_VALUE || y + 32 >= info.getMaxHeight()) return false;

        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
        if (!stable(info, region, x, y, z, 7)) return false;

        switch (biome) {
            case ANCIENT_FAE_FOREST -> placeGreatTree(info, region, random, x, y + 1, z,
                Material.DARK_OAK_LOG, Material.DARK_OAK_WOOD, Material.DARK_OAK_LEAVES, Material.AZALEA_LEAVES);
            case CRYSTAL_WOODS -> {
                if (random.nextBoolean()) placeCrystalCrown(region, random, x, y + 1, z);
                else placeGreatTree(info, region, random, x, y + 1, z,
                    Material.CHERRY_LOG, Material.CHERRY_WOOD, Material.CHERRY_LEAVES, Material.FLOWERING_AZALEA_LEAVES);
            }
            case MIST_GARDENS -> placeHangingGarden(region, random, x, y + 1, z);
            case SKY_HIGHLANDS -> placeSkyArch(region, x, y + 1, z);
            case GOLDEN_MEADOWS -> {
                if (random.nextBoolean()) placeSunCauseway(region, random, x, y + 1, z);
                else placeGreatTree(info, region, random, x, y + 1, z,
                    Material.OAK_LOG, Material.OAK_WOOD, Material.OAK_LEAVES, Material.FLOWERING_AZALEA_LEAVES);
            }
        }
        return true;
    }

    private void placeGreatTree(WorldInfo info, LimitedRegion region, SplittableRandom random,
                                int x, int y, int z,
                                Material log, Material wood, Material leaves, Material accentLeaves) {
        int height = 18 + random.nextInt(9);
        int trunkX = x;
        int trunkZ = z;
        int bendX = random.nextInt(-1, 2);
        int bendZ = bendX == 0 ? (random.nextBoolean() ? 1 : -1) : random.nextInt(-1, 2);
        for (int dy = 0; dy < height; dy++) {
            if (dy > 5 && dy % 5 == 0 && random.nextDouble() < 0.72) { trunkX += bendX; trunkZ += bendZ; }
            set(region, trunkX, y + dy, trunkZ, log);
            set(region, trunkX + 1, y + dy, trunkZ, wood);
            set(region, trunkX, y + dy, trunkZ + 1, wood);
            if (dy < height / 2 && (dy & 1) == 0) set(region, trunkX + 1, y + dy, trunkZ + 1, log);
        }

        int crownY = y + height - 3;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1}};
        for (int branch = 0; branch < 9; branch++) {
            int[] d = dirs[branch % dirs.length];
            int length = 4 + random.nextInt(5);
            int by = crownY - random.nextInt(0, 5);
            for (int step = 1; step <= length; step++) {
                set(region, trunkX + d[0] * step, by + step / 3, trunkZ + d[1] * step, wood);
            }
            placeCanopy(region, random, trunkX + d[0] * length, by + length / 3 + 1,
                trunkZ + d[1] * length, leaves, accentLeaves, 4, 2);
        }
        placeCanopy(region, random, trunkX, y + height, trunkZ, leaves, accentLeaves, 7, 3);

        for (int root = 0; root < 8; root++) {
            int[] d = dirs[root];
            int length = 4 + random.nextInt(5);
            for (int step = 1; step <= length; step++) {
                int px = x + d[0] * step;
                int pz = z + d[1] * step;
                int surfaceY = FaeSurfaceLocator.find(info, region, px, pz);
                if (surfaceY == Integer.MIN_VALUE) continue;
                int py = surfaceY + 1;
                set(region, px, py, pz, wood);
                if (step <= 3) set(region, px, py + 1, pz, log);
            }
        }
        set(region, trunkX, y + height + 2, trunkZ, Material.SHROOMLIGHT);
    }

    private void placeCanopy(LimitedRegion region, SplittableRandom random, int x, int y, int z,
                             Material primary, Material accent, int radius, int verticalRadius) {
        for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
            double scale = 1.0 - Math.abs(dy) / (double) (verticalRadius + 1);
            int layerRadius = Math.max(1, (int) Math.round(radius * scale));
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    if (dx * dx + dz * dz > layerRadius * layerRadius + 2 || random.nextDouble() < 0.09) continue;
                    setIfAir(region, x + dx, y + dy, z + dz, random.nextDouble() < 0.14 ? accent : primary);
                }
            }
        }
    }

    private void placeCrystalCrown(LimitedRegion region, SplittableRandom random, int x, int y, int z) {
        int[][] offsets = {{0,0},{-4,0},{4,0},{0,-4},{0,4},{-3,-3},{3,3},{-3,3},{3,-3}};
        for (int i = 0; i < offsets.length; i++) {
            int height = i == 0 ? 15 : 6 + random.nextInt(7);
            int px = x + offsets[i][0];
            int pz = z + offsets[i][1];
            for (int dy = 0; dy < height; dy++) {
                Material m = dy < 2 ? Material.CALCITE : dy == height - 1 ? Material.AMETHYST_CLUSTER
                    : (dy % 4 == 0 ? Material.BUDDING_AMETHYST : Material.AMETHYST_BLOCK);
                set(region, px, y + dy, pz, m);
            }
        }
        ring(region, x, y, z, 6, Material.SMOOTH_QUARTZ);
        set(region, x, y, z, Material.SEA_LANTERN);
    }

    private void placeHangingGarden(LimitedRegion region, SplittableRandom random, int x, int y, int z) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 6.3) continue;
                set(region, x + dx, y - 1, z + dz, distance < 3.0 ? Material.PALE_MOSS_BLOCK : Material.MOSS_BLOCK);
                if (random.nextDouble() < 0.28) setIfAir(region, x + dx, y, z + dz,
                    random.nextBoolean() ? Material.FERN : Material.FLOWERING_AZALEA);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(region, x + dx, y, z + dz, Material.WATER);
                set(region, x + dx, y - 1, z + dz, Material.GLOWSTONE);
            }
        }
        int direction = random.nextInt(4);
        for (int step = 2; step <= 6; step++) {
            int px = x + (direction == 0 ? step : direction == 1 ? -step : 0);
            int pz = z + (direction == 2 ? step : direction == 3 ? -step : 0);
            set(region, px, y - 1, pz, Material.MOSS_BLOCK);
            set(region, px, y, pz, Material.WATER);
        }
    }

    private void placeSkyArch(LimitedRegion region, int x, int y, int z) {
        int radius = 7;
        for (int dx = -radius; dx <= radius; dx++) {
            double normalized = dx / (double) radius;
            int archY = y + (int) Math.round(Math.sqrt(Math.max(0.0, 1.0 - normalized * normalized)) * 10.0);
            for (int thickness = -1; thickness <= 1; thickness++) {
                set(region, x + dx, archY + thickness, z,
                    ((dx + thickness) & 2) == 0 ? Material.CALCITE : Material.SMOOTH_QUARTZ);
            }
        }
        for (int dy = 0; dy <= 4; dy++) {
            set(region, x - radius, y + dy, z, Material.QUARTZ_PILLAR);
            set(region, x + radius, y + dy, z, Material.QUARTZ_PILLAR);
        }
        set(region, x, y + 10, z, Material.SEA_LANTERN);
        set(region, x, y, z, Material.LODESTONE);
    }

    private void placeSunCauseway(LimitedRegion region, SplittableRandom random, int x, int y, int z) {
        boolean alongX = random.nextBoolean();
        for (int step = -8; step <= 8; step++) {
            if (Math.abs(step) > 3 && random.nextDouble() < 0.22) continue;
            int px = alongX ? x + step : x;
            int pz = alongX ? z : z + step;
            set(region, px, y, pz, (step & 1) == 0 ? Material.CUT_SANDSTONE_SLAB : Material.QUARTZ_SLAB);
            if (Math.abs(step) == 8 || Math.abs(step) == 4) set(region, px, y + 1, pz, Material.GLOWSTONE);
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) if (dx * dx + dz * dz <= 10)
                set(region, x + dx, y - 1, z + dz, Material.SMOOTH_SANDSTONE);
        }
        set(region, x, y, z, Material.GOLD_BLOCK);
        set(region, x, y + 1, z, Material.SEA_LANTERN);
    }

    private boolean stable(WorldInfo info, LimitedRegion region, int x, int y, int z, int radius) {
        int[][] samples = {{0,0},{-radius,0},{radius,0},{0,-radius},{0,radius},{-radius,-radius},{-radius,radius},{radius,-radius},{radius,radius}};
        for (int[] s : samples) {
            int sx = x + s[0], sz = z + s[1];
            int surfaceY = FaeSurfaceLocator.find(info, region, sx, sz);
            if (!region.isInRegion(sx, y, sz)
                || surfaceY == Integer.MIN_VALUE
                || Math.abs(surfaceY - y) > 4) return false;
        }
        return true;
    }

    private void ring(LimitedRegion region, int x, int y, int z, int radius, Material material) {
        for (int offset = -radius; offset <= radius; offset++) {
            set(region, x + offset, y, z - radius, material);
            set(region, x + offset, y, z + radius, material);
            set(region, x - radius, y, z + offset, material);
            set(region, x + radius, y, z + offset, material);
        }
    }

    private boolean setIfAir(LimitedRegion region, int x, int y, int z, Material material) {
        if (!region.isInRegion(x, y, z) || !region.getType(x, y, z).isAir()) return false;
        region.setType(x, y, z, material);
        return true;
    }

    private void set(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) region.setType(x, y, z, material);
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
