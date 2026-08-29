package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.SplittableRandom;

/** Generates rare dormant rift shrines in the central Fae Realm. */
public final class FaeRiftPopulator {

    private static final long RIFT_SALT = 0x082EFA98EC4E6C89L;

    public void populate(WorldInfo info,
                         int chunkX,
                         int chunkZ,
                         LimitedRegion region,
                         double density) {
        if (FaePlane.fromWorldName(info.getName()) != FaePlane.REALM || density <= 0.0) {
            return;
        }

        SplittableRandom random = new SplittableRandom(mixSeed(info.getSeed() ^ RIFT_SALT, chunkX, chunkZ));
        double chance = Math.min(0.035, (1.0 / 96.0) * Math.max(0.5, density));
        if (random.nextDouble() >= chance) {
            return;
        }

        int x = (chunkX << 4) + 5 + random.nextInt(6);
        int z = (chunkZ << 4) + 5 + random.nextInt(6);
        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
        int y = findSurface(info, region, x, z, biome);
        if (y == Integer.MIN_VALUE || y + 7 >= info.getMaxHeight() || !stable(region, x, y, z)) {
            return;
        }

        FaePlane target = switch (random.nextInt(3)) {
            case 0 -> FaePlane.WILDBLOOM;
            case 1 -> FaePlane.GLOAM;
            default -> FaePlane.STARFALL;
        };
        placeShrine(region, x, y, z, target, random);
    }

    private void placeShrine(LimitedRegion region,
                             int x,
                             int y,
                             int z,
                             FaePlane target,
                             SplittableRandom random) {
        Material floor = switch (target) {
            case WILDBLOOM -> Material.MOSSY_STONE_BRICKS;
            case GLOAM -> Material.POLISHED_TUFF;
            case STARFALL -> Material.CALCITE;
            case REALM -> Material.STONE_BRICKS;
        };
        Material marker = marker(target);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2 || random.nextDouble() < 0.55) {
                    set(region, x + dx, y, z + dz, floor);
                }
            }
        }

        set(region, x, y + 1, z, Material.LODESTONE);
        set(region, x + 1, y + 1, z, marker);
        set(region, x - 1, y + 1, z, marker);
        set(region, x, y + 1, z + 1, marker);
        set(region, x, y + 1, z - 1, marker);

        Material pillar = switch (target) {
            case WILDBLOOM -> Material.CHERRY_LOG;
            case GLOAM -> Material.DARK_OAK_LOG;
            case STARFALL -> Material.AMETHYST_BLOCK;
            case REALM -> Material.STONE_BRICKS;
        };
        Material light = switch (target) {
            case WILDBLOOM -> Material.SHROOMLIGHT;
            case GLOAM -> Material.SOUL_LANTERN;
            case STARFALL -> Material.SEA_LANTERN;
            case REALM -> Material.GLOWSTONE;
        };

        for (int[] corner : new int[][]{{-2, -2}, {-2, 2}, {2, -2}, {2, 2}}) {
            int height = 2 + random.nextInt(3);
            for (int dy = 1; dy <= height; dy++) {
                set(region, x + corner[0], y + dy, z + corner[1], pillar);
            }
            setIfAir(region, x + corner[0], y + height + 1, z + corner[1], light);
        }
    }

    public static Material marker(FaePlane plane) {
        return switch (plane) {
            case REALM -> Material.GLOWSTONE;
            case WILDBLOOM -> Material.MOSS_BLOCK;
            case GLOAM -> Material.SCULK;
            case STARFALL -> Material.AMETHYST_BLOCK;
        };
    }

    private boolean stable(LimitedRegion region, int x, int y, int z) {
        int supported = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (region.isInRegion(x + dx, y, z + dz)
                    && !region.getType(x + dx, y, z + dz).isAir()) {
                    supported++;
                }
            }
        }
        return supported >= 7;
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

    private void set(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) {
            region.setType(x, y, z, material);
        }
    }

    private void setIfAir(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z) && region.getType(x, y, z).isAir()) {
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
