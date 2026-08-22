package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.SplittableRandom;

/**
 * Chunk-local dungeon builder used by rare Fae Realm Dungeon Gates.
 *
 * <p>The dungeon is deliberately vertical and remains within the gate's own chunk.
 * This lets it run safely from Paper's LimitedRegion world-generation phase without
 * force-loading neighboring chunks.</p>
 */
public final class FaeDungeonGenerator {

    private static final long DUNGEON_SALT = 0x243F6A8885A308D3L;
    private static final NamespacedKey GENERATED_BARREL_KEY = Objects.requireNonNull(
        NamespacedKey.fromString("thefaerealm:generated_vault_barrel"));

    public void place(LimitedRegion region,
                      int x,
                      int entranceY,
                      int z,
                      FaeRealmBiome biome,
                      long worldSeed,
                      int chunkX,
                      int chunkZ) {
        SplittableRandom random = new SplittableRandom(
            mixSeed(worldSeed ^ DUNGEON_SALT, chunkX, chunkZ));
        Palette palette = Palette.forBiome(biome);
        DungeonPlan plan = DungeonPlan.values()[random.nextInt(DungeonPlan.values().length)];

        int mainFloor = entranceY - 6;
        int lowerFloor = mainFloor - 9;

        buildMainHall(region, x, mainFloor, z + 1, palette);
        buildLowerVault(region, x, lowerFloor, z + 1, palette, random);
        buildEntranceStair(region, x, entranceY, z, mainFloor, palette);
        buildSpiralDescent(region, x, mainFloor, lowerFloor, z + 1, palette);
        decorateMainHall(region, x, mainFloor, z + 1, palette, plan, random);
        decorateLowerVault(region, x, lowerFloor, z + 1, palette, plan);
    }

    private void buildMainHall(LimitedRegion region, int x, int floorY, int z, Palette palette) {
        int radius = 5;
        int height = 6;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= height; dy++) {
                    boolean floor = dy == 0;
                    boolean ceiling = dy == height;
                    boolean wall = Math.abs(dx) == radius || Math.abs(dz) == radius;
                    if (floor) {
                        set(region, x + dx, floorY, z + dz,
                            checker(dx, dz) ? palette.secondary() : palette.primary());
                    } else if (ceiling) {
                        set(region, x + dx, floorY + dy, z + dz, palette.secondary());
                    } else if (wall) {
                        set(region, x + dx, floorY + dy, z + dz,
                            ((dx + dz + dy) & 3) == 0 ? palette.accent() : palette.primary());
                    } else {
                        set(region, x + dx, floorY + dy, z + dz, Material.AIR);
                    }
                }
            }
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                set(region, x + dx, floorY + dy, z + radius, Material.AIR);
            }
        }
    }

    private void buildEntranceStair(LimitedRegion region,
                                    int x,
                                    int entranceY,
                                    int gateZ,
                                    int mainFloor,
                                    Palette palette) {
        for (int step = 0; step <= 5; step++) {
            int stepZ = gateZ + 1 + step;
            int floorY = entranceY - 1 - step;

            for (int dx = -1; dx <= 1; dx++) {
                set(region, x + dx, floorY, stepZ,
                    dx == 0 ? palette.secondary() : palette.primary());
                for (int clear = 1; clear <= 3; clear++) {
                    set(region, x + dx, floorY + clear, stepZ, Material.AIR);
                }
            }

            set(region, x - 2, floorY + 1, stepZ, palette.primary());
            set(region, x + 2, floorY + 1, stepZ, palette.primary());
            if ((step & 1) == 0) {
                set(region, x - 2, floorY + 2, stepZ, palette.light());
                set(region, x + 2, floorY + 2, stepZ, palette.light());
            }
        }

        set(region, x, mainFloor, gateZ + 6, palette.secondary());
    }

    private void buildLowerVault(LimitedRegion region,
                                 int x,
                                 int floorY,
                                 int z,
                                 Palette palette,
                                 SplittableRandom random) {
        int radius = 4;
        int height = 5;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= height; dy++) {
                    boolean floor = dy == 0;
                    boolean ceiling = dy == height;
                    boolean wall = Math.abs(dx) == radius || Math.abs(dz) == radius;
                    if (floor) {
                        set(region, x + dx, floorY, z + dz,
                            checker(dx + 1, dz - 1) ? palette.deep() : palette.secondary());
                    } else if (ceiling || wall) {
                        set(region, x + dx, floorY + dy, z + dz,
                            random.nextInt(6) == 0 ? palette.accent() : palette.deep());
                    } else {
                        set(region, x + dx, floorY + dy, z + dz, Material.AIR);
                    }
                }
            }
        }
    }

    private void buildSpiralDescent(LimitedRegion region,
                                    int x,
                                    int mainFloor,
                                    int lowerFloor,
                                    int z,
                                    Palette palette) {
        int[][] path = {
            {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2},
            {2, -1}, {2, 0}, {2, 1}, {2, 2},
            {1, 2}, {0, 2}, {-1, 2}, {-2, 2},
            {-2, 1}, {-2, 0}, {-2, -1}
        };

        for (int dy = lowerFloor + 1; dy <= mainFloor + 3; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    set(region, x + dx, dy, z + dz, Material.AIR);
                }
            }
        }

        int verticalDrop = mainFloor - lowerFloor;
        for (int i = 0; i < path.length; i++) {
            int drop = Math.min(verticalDrop, (i * verticalDrop) / (path.length - 1));
            int stepY = mainFloor - drop;
            int px = x + path[i][0];
            int pz = z + path[i][1];
            set(region, px, stepY, pz, i % 3 == 0 ? palette.accent() : palette.secondary());
            set(region, px, stepY + 1, pz, Material.AIR);
            set(region, px, stepY + 2, pz, Material.AIR);
        }

        for (int y = lowerFloor + 1; y <= mainFloor - 1; y++) {
            set(region, x, y, z, (y & 3) == 0 ? palette.light() : palette.deep());
        }
    }

    private void decorateMainHall(LimitedRegion region,
                                  int x,
                                  int floorY,
                                  int z,
                                  Palette palette,
                                  DungeonPlan plan,
                                  SplittableRandom random) {
        placePillar(region, x - 3, floorY + 1, z - 3, 4, palette);
        placePillar(region, x + 3, floorY + 1, z - 3, 4, palette);
        placePillar(region, x - 3, floorY + 1, z + 3, 4, palette);
        placePillar(region, x + 3, floorY + 1, z + 3, 4, palette);

        switch (plan) {
            case HALL_OF_ECHOES -> {
                for (int offset = -3; offset <= 3; offset += 2) {
                    set(region, x + offset, floorY + 1, z, palette.accent());
                    set(region, x, floorY + 1, z + offset, palette.accent());
                }
                set(region, x, floorY + 2, z, palette.light());
            }
            case TWIN_RELIQUARIES -> {
                buildAlcove(region, x - 4, floorY, z, palette);
                buildAlcove(region, x + 4, floorY, z, palette);
                set(region, x - 3, floorY + 1, z, Material.BARREL);
                set(region, x + 3, floorY + 1, z, Material.BARREL);
            }
            case FAERIE_CROSSING -> {
                for (int offset = -4; offset <= 4; offset++) {
                    if (Math.abs(offset) <= 1) {
                        continue;
                    }
                    set(region, x + offset, floorY + 1, z, Material.IRON_BARS);
                    set(region, x, floorY + 1, z + offset, Material.IRON_BARS);
                }
                set(region, x, floorY + 1, z, palette.relic());
            }
        }

        int cacheZ = random.nextBoolean() ? z - 3 : z + 3;
        set(region, x - 4, floorY + 1, cacheZ, Material.BARREL);
        set(region, x + 4, floorY + 1, cacheZ, Material.BARREL);
    }

    private void decorateLowerVault(LimitedRegion region,
                                    int x,
                                    int floorY,
                                    int z,
                                    Palette palette,
                                    DungeonPlan plan) {
        for (int dx : new int[]{-3, 3}) {
            for (int dz : new int[]{-3, 3}) {
                set(region, x + dx, floorY + 1, z + dz, palette.light());
            }
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(region, x + dx, floorY + 1, z + dz,
                    Math.abs(dx) + Math.abs(dz) == 0 ? palette.relic() : palette.accent());
            }
        }
        set(region, x, floorY + 2, z, palette.light());

        set(region, x - 3, floorY + 1, z, Material.BARREL);
        set(region, x + 3, floorY + 1, z, Material.BARREL);
        set(region, x, floorY + 1, z - 3, Material.BARREL);
        set(region, x, floorY + 1, z + 3, Material.BARREL);

        if (plan == DungeonPlan.FAERIE_CROSSING) {
            set(region, x, floorY + 1, z - 2, Material.CRYING_OBSIDIAN);
            set(region, x, floorY + 1, z + 2, Material.CRYING_OBSIDIAN);
        }
    }

    private void buildAlcove(LimitedRegion region, int x, int floorY, int z, Palette palette) {
        for (int dz = -1; dz <= 1; dz++) {
            set(region, x, floorY + 1, z + dz, palette.deep());
            set(region, x, floorY + 2, z + dz, palette.accent());
            set(region, x, floorY + 3, z + dz, palette.deep());
        }
    }

    private void placePillar(LimitedRegion region, int x, int y, int z, int height, Palette palette) {
        for (int dy = 0; dy < height; dy++) {
            set(region, x, y + dy, z, dy == height - 1 ? palette.accent() : palette.deep());
        }
        set(region, x, y + height, z, palette.light());
    }

    private boolean checker(int x, int z) {
        return ((x ^ z) & 1) == 0;
    }

    private void set(LimitedRegion region, int x, int y, int z, Material material) {
        if (!region.isInRegion(x, y, z)) {
            return;
        }

        region.setType(x, y, z, material);
        if (material != Material.BARREL) {
            return;
        }

        BlockState state = region.getBlockState(x, y, z);
        if (state instanceof Barrel barrel) {
            barrel.getPersistentDataContainer().set(
                GENERATED_BARREL_KEY, PersistentDataType.BYTE, (byte) 1);
            region.setBlockState(x, y, z, barrel);
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

    private enum DungeonPlan {
        HALL_OF_ECHOES,
        TWIN_RELIQUARIES,
        FAERIE_CROSSING
    }

    private record Palette(Material primary,
                           Material secondary,
                           Material deep,
                           Material accent,
                           Material light,
                           Material relic) {

        private static Palette forBiome(FaeRealmBiome biome) {
            return switch (biome) {
                case GOLDEN_MEADOWS -> new Palette(
                    Material.CUT_SANDSTONE,
                    Material.SMOOTH_SANDSTONE,
                    Material.DEEPSLATE_BRICKS,
                    Material.GOLD_BLOCK,
                    Material.GLOWSTONE,
                    Material.LODESTONE);
                case CRYSTAL_WOODS -> new Palette(
                    Material.QUARTZ_BRICKS,
                    Material.CALCITE,
                    Material.POLISHED_DEEPSLATE,
                    Material.AMETHYST_BLOCK,
                    Material.SEA_LANTERN,
                    Material.BUDDING_AMETHYST);
                case MIST_GARDENS -> new Palette(
                    Material.TUFF_BRICKS,
                    Material.POLISHED_TUFF,
                    Material.DEEPSLATE_TILES,
                    Material.CHISELED_TUFF,
                    Material.SOUL_LANTERN,
                    Material.CRYING_OBSIDIAN);
                case ANCIENT_FAE_FOREST -> new Palette(
                    Material.MOSSY_STONE_BRICKS,
                    Material.STONE_BRICKS,
                    Material.POLISHED_DEEPSLATE,
                    Material.CHISELED_STONE_BRICKS,
                    Material.SOUL_LANTERN,
                    Material.LODESTONE);
                case SKY_HIGHLANDS -> new Palette(
                    Material.POLISHED_ANDESITE,
                    Material.SMOOTH_STONE,
                    Material.DEEPSLATE_TILES,
                    Material.SMOOTH_QUARTZ,
                    Material.SEA_LANTERN,
                    Material.LODESTONE);
            };
        }
    }
}
