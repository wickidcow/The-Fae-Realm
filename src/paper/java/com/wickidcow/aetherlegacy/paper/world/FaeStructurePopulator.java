package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.SplittableRandom;

/**
 * Deterministic structure layer for the Fae Realm.
 *
 * <p>One candidate structure chunk is selected per structure cell from the world seed.
 * Structures are kept inside the selected chunk so placement remains safe during
 * asynchronous Paper generation through {@link LimitedRegion}.</p>
 */
public final class FaeStructurePopulator {

    private static final int STRUCTURE_CELL_CHUNKS = 10;
    private static final long STRUCTURE_SALT = 0x6A09E667F3BCC909L;
    private final FaeDungeonGenerator dungeonGenerator = new FaeDungeonGenerator();

    public void populate(@NotNull WorldInfo worldInfo,
                         @NotNull Random random,
                         int chunkX,
                         int chunkZ,
                         @NotNull LimitedRegion region) {
        if (Math.abs(chunkX) <= 4 && Math.abs(chunkZ) <= 4) {
            return; // Keep the arrival area quiet and uncluttered.
        }

        int cellX = Math.floorDiv(chunkX, STRUCTURE_CELL_CHUNKS);
        int cellZ = Math.floorDiv(chunkZ, STRUCTURE_CELL_CHUNKS);
        SplittableRandom cellRandom = new SplittableRandom(mixSeed(worldInfo.getSeed() ^ STRUCTURE_SALT, cellX, cellZ));

        int anchorChunkX = cellX * STRUCTURE_CELL_CHUNKS + cellRandom.nextInt(1, STRUCTURE_CELL_CHUNKS - 1);
        int anchorChunkZ = cellZ * STRUCTURE_CELL_CHUNKS + cellRandom.nextInt(1, STRUCTURE_CELL_CHUNKS - 1);
        if (chunkX != anchorChunkX || chunkZ != anchorChunkZ) {
            return;
        }

        int x = (chunkX << 4) + 8;
        int z = (chunkZ << 4) + 8;
        int y = region.getHighestBlockYAt(x, z);
        if (y < worldInfo.getMinHeight() || y + 16 >= worldInfo.getMaxHeight()) {
            return;
        }

        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(worldInfo.getSeed(), x, z);
        if (region.getType(x, y, z) != biome.surface() || !siteIsStable(region, x, y, z, 6)) {
            return;
        }

        int roll = cellRandom.nextInt(100);
        if (roll < 12 && y - 18 > worldInfo.getMinHeight()) {
            int entranceY = y + 1;
            placeDungeonGate(region, x, entranceY, z, biome);
            dungeonGenerator.place(
                region,
                x,
                entranceY,
                z,
                biome,
                worldInfo.getSeed(),
                chunkX,
                chunkZ
            );
            return;
        }

        switch (biome) {
            case GOLDEN_MEADOWS -> placeSunCourtShrine(region, x, y + 1, z);
            case CRYSTAL_WOODS -> placeCrystalTemple(region, x, y + 1, z);
            case MIST_GARDENS -> placeMistSanctum(region, x, y + 1, z);
            case ANCIENT_FAE_FOREST -> placeAncientWatchtower(region, x, y + 1, z);
            case SKY_HIGHLANDS -> placeSkyGate(region, x, y + 1, z);
        }
    }

    private boolean siteIsStable(LimitedRegion region, int x, int y, int z, int radius) {
        int[][] samples = {
            {0, 0}, {-radius, -radius}, {-radius, radius}, {radius, -radius}, {radius, radius},
            {-radius, 0}, {radius, 0}, {0, -radius}, {0, radius}
        };
        for (int[] sample : samples) {
            int sx = x + sample[0];
            int sz = z + sample[1];
            if (!region.isInRegion(sx, y, sz)) {
                return false;
            }
            int sampleY = region.getHighestBlockYAt(sx, sz);
            if (Math.abs(sampleY - y) > 4 || sampleY < y - 5) {
                return false;
            }
        }
        return true;
    }

    private void placeSunCourtShrine(LimitedRegion region, int x, int y, int z) {
        fillFloor(region, x, y - 1, z, 4, Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE);
        placeCornerPillars(region, x, y, z, 4, 5, Material.SMOOTH_SANDSTONE, Material.CHISELED_SANDSTONE);
        ring(region, x, y, z, 4, Material.CUT_SANDSTONE);
        ring(region, x, y + 4, z, 4, Material.SMOOTH_SANDSTONE);
        set(region, x, y, z, Material.GOLD_BLOCK);
        set(region, x, y + 1, z, Material.GLOWSTONE);
        set(region, x, y + 2, z, Material.SEA_LANTERN);
        for (int dx = -2; dx <= 2; dx++) {
            set(region, x + dx, y, z, Material.QUARTZ_SLAB);
        }
    }

    private void placeCrystalTemple(LimitedRegion region, int x, int y, int z) {
        fillFloor(region, x, y - 1, z, 6, Material.CALCITE, Material.SMOOTH_QUARTZ);
        ring(region, x, y, z, 6, Material.QUARTZ_BRICKS);
        ring(region, x, y + 1, z, 5, Material.CALCITE);
        placeCornerPillars(region, x, y, z, 5, 7, Material.QUARTZ_PILLAR, Material.AMETHYST_BLOCK);

        for (int dy = 0; dy <= 7; dy++) {
            set(region, x, y + dy, z, dy < 5 ? Material.AMETHYST_BLOCK : Material.CALCITE);
        }
        set(region, x, y + 8, z, Material.AMETHYST_CLUSTER);
        set(region, x - 3, y + 1, z, Material.BUDDING_AMETHYST);
        set(region, x + 3, y + 1, z, Material.BUDDING_AMETHYST);
        set(region, x, y + 1, z - 3, Material.SEA_LANTERN);
        set(region, x, y + 1, z + 3, Material.SEA_LANTERN);
    }

    private void placeMistSanctum(LimitedRegion region, int x, int y, int z) {
        fillFloor(region, x, y - 1, z, 5, Material.TUFF_BRICKS, Material.PALE_MOSS_BLOCK);
        ring(region, x, y, z, 5, Material.TUFF_BRICK_WALL);
        placeCornerPillars(region, x, y, z, 4, 6, Material.POLISHED_TUFF, Material.CHISELED_TUFF);
        for (int dz = -3; dz <= 3; dz++) {
            set(region, x, y, z + dz, Material.PALE_MOSS_BLOCK);
        }
        set(region, x, y + 1, z, Material.SOUL_LANTERN);
        set(region, x - 3, y + 1, z - 3, Material.SOUL_LANTERN);
        set(region, x + 3, y + 1, z + 3, Material.SOUL_LANTERN);
    }

    private void placeAncientWatchtower(LimitedRegion region, int x, int y, int z) {
        fillFloor(region, x, y - 1, z, 5, Material.MOSSY_STONE_BRICKS, Material.STONE_BRICKS);
        for (int dy = 0; dy <= 9; dy++) {
            int radius = dy < 7 ? 3 : 4;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean wall = Math.abs(dx) == radius || Math.abs(dz) == radius;
                    if (wall && ((dx + dz + dy) & 1) == 0) {
                        set(region, x + dx, y + dy, z + dz,
                            dy < 7 ? Material.MOSSY_STONE_BRICKS : Material.DARK_OAK_PLANKS);
                    }
                }
            }
        }
        for (int dy = 0; dy <= 10; dy++) {
            set(region, x, y + dy, z, Material.DARK_OAK_LOG);
        }
        set(region, x, y + 11, z, Material.SOUL_LANTERN);
    }

    private void placeSkyGate(LimitedRegion region, int x, int y, int z) {
        fillFloor(region, x, y - 1, z, 5, Material.POLISHED_ANDESITE, Material.SMOOTH_STONE);
        for (int dx : new int[]{-4, 4}) {
            for (int dy = 0; dy <= 8; dy++) {
                set(region, x + dx, y + dy, z, Material.QUARTZ_PILLAR);
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            set(region, x + dx, y + 8, z, Material.SMOOTH_QUARTZ);
        }
        set(region, x - 4, y + 9, z, Material.SEA_LANTERN);
        set(region, x + 4, y + 9, z, Material.SEA_LANTERN);
        set(region, x, y, z, Material.LODESTONE);
    }

    private void placeDungeonGate(LimitedRegion region, int x, int y, int z, FaeRealmBiome biome) {
        Material primary = switch (biome) {
            case GOLDEN_MEADOWS -> Material.CUT_SANDSTONE;
            case CRYSTAL_WOODS -> Material.QUARTZ_BRICKS;
            case MIST_GARDENS -> Material.TUFF_BRICKS;
            case ANCIENT_FAE_FOREST -> Material.MOSSY_STONE_BRICKS;
            case SKY_HIGHLANDS -> Material.POLISHED_ANDESITE;
        };

        fillFloor(region, x, y - 1, z, 6, primary, Material.DEEPSLATE_TILES);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = 0; dy <= 7; dy++) {
                boolean frame = Math.abs(dx) >= 4 || dy >= 6;
                boolean doorway = Math.abs(dx) <= 2 && dy <= 4;
                if (frame && !doorway) {
                    set(region, x + dx, y + dy, z, primary);
                }
            }
        }
        for (int dz = 1; dz <= 5; dz++) {
            set(region, x - 2, y, z + dz, Material.DEEPSLATE_BRICKS);
            set(region, x + 2, y, z + dz, Material.DEEPSLATE_BRICKS);
            set(region, x, y - 1, z + dz, Material.DEEPSLATE_TILES);
        }
        set(region, x, y + 6, z, Material.CRYING_OBSIDIAN);
        set(region, x, y + 7, z, Material.SOUL_LANTERN);
    }

    private void fillFloor(LimitedRegion region, int x, int y, int z, int radius,
                           Material primary, Material secondary) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Material material = ((dx * 31 + dz * 17) & 3) == 0 ? secondary : primary;
                set(region, x + dx, y, z + dz, material);
            }
        }
    }

    private void placeCornerPillars(LimitedRegion region, int x, int y, int z,
                                    int radius, int height, Material shaft, Material cap) {
        for (int dx : new int[]{-radius, radius}) {
            for (int dz : new int[]{-radius, radius}) {
                for (int dy = 0; dy < height; dy++) {
                    set(region, x + dx, y + dy, z + dz, shaft);
                }
                set(region, x + dx, y + height, z + dz, cap);
            }
        }
    }

    private void ring(LimitedRegion region, int x, int y, int z, int radius, Material material) {
        for (int offset = -radius; offset <= radius; offset++) {
            set(region, x + offset, y, z - radius, material);
            set(region, x + offset, y, z + radius, material);
            set(region, x - radius, y, z + offset, material);
            set(region, x + radius, y, z + offset, material);
        }
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
