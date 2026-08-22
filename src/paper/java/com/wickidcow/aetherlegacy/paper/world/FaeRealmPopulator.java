package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Thread-safe decoration pass for the Fae Realm.
 * Uses LimitedRegion only, so it is safe for Paper's asynchronous chunk generation.
 */
public final class FaeRealmPopulator extends BlockPopulator {

    private static final FaeStructurePopulator STRUCTURES = new FaeStructurePopulator();

    @Override
    public void populate(@NotNull WorldInfo worldInfo,
                         @NotNull Random random,
                         int chunkX,
                         int chunkZ,
                         @NotNull LimitedRegion region) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        int treeAttempts = 1 + random.nextInt(3);
        for (int i = 0; i < treeAttempts; i++) {
            int x = baseX + random.nextInt(16);
            int z = baseZ + random.nextInt(16);
            int y = region.getHighestBlockYAt(x, z);
            if (y < worldInfo.getMinHeight() || y + 8 >= worldInfo.getMaxHeight()) {
                continue;
            }

            FaeRealmBiome biome = AetherChunkGenerator.biomeAt(worldInfo.getSeed(), x, z);
            Material surface = region.getType(x, y, z);
            if (surface != biome.surface()) {
                continue;
            }

            if (random.nextDouble() < treeChance(biome)) {
                placeTree(region, x, y + 1, z, biome, random);
            }
        }

        if (random.nextInt(9) == 0) {
            placeCrystalOutcrop(worldInfo, region, random, baseX, baseZ);
        }

        if (random.nextInt(96) == 0) {
            placeFaeRuin(worldInfo, region, random, baseX, baseZ);
        }

        STRUCTURES.populate(worldInfo, random, chunkX, chunkZ, region);
    }

    private double treeChance(FaeRealmBiome biome) {
        return switch (biome) {
            case GOLDEN_MEADOWS -> 0.22;
            case CRYSTAL_WOODS -> 0.72;
            case MIST_GARDENS -> 0.48;
            case ANCIENT_FAE_FOREST -> 0.82;
            case SKY_HIGHLANDS -> 0.35;
        };
    }

    private void placeTree(LimitedRegion region, int x, int y, int z,
                           FaeRealmBiome biome, Random random) {
        Material log = switch (biome) {
            case GOLDEN_MEADOWS -> Material.OAK_LOG;
            case CRYSTAL_WOODS -> Material.CHERRY_LOG;
            case MIST_GARDENS -> Material.PALE_OAK_LOG;
            case ANCIENT_FAE_FOREST -> Material.DARK_OAK_LOG;
            case SKY_HIGHLANDS -> Material.BIRCH_LOG;
        };
        Material leaves = switch (biome) {
            case GOLDEN_MEADOWS -> Material.OAK_LEAVES;
            case CRYSTAL_WOODS -> Material.CHERRY_LEAVES;
            case MIST_GARDENS -> Material.PALE_OAK_LEAVES;
            case ANCIENT_FAE_FOREST -> Material.DARK_OAK_LEAVES;
            case SKY_HIGHLANDS -> Material.BIRCH_LEAVES;
        };

        int height = 4 + random.nextInt(3);
        for (int dy = 0; dy < height; dy++) {
            setIfInside(region, x, y + dy, z, log);
        }

        int crownY = y + height - 2;
        for (int dy = 0; dy <= 3; dy++) {
            int radius = dy == 3 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && random.nextBoolean()) {
                        continue;
                    }
                    int py = crownY + dy;
                    if (region.isInRegion(x + dx, py, z + dz)
                        && region.getType(x + dx, py, z + dz).isAir()) {
                        region.setType(x + dx, py, z + dz, leaves);
                    }
                }
            }
        }
    }

    private void placeCrystalOutcrop(WorldInfo info, LimitedRegion region, Random random, int baseX, int baseZ) {
        int x = baseX + 3 + random.nextInt(10);
        int z = baseZ + 3 + random.nextInt(10);
        int y = region.getHighestBlockYAt(x, z);
        if (y < info.getMinHeight() || y + 4 >= info.getMaxHeight()) {
            return;
        }
        if (AetherChunkGenerator.biomeAt(info.getSeed(), x, z) != FaeRealmBiome.CRYSTAL_WOODS) {
            return;
        }

        setIfInside(region, x, y + 1, z, Material.CALCITE);
        setIfInside(region, x, y + 2, z, Material.AMETHYST_BLOCK);
        setIfInside(region, x, y + 3, z, Material.AMETHYST_CLUSTER);
        if (random.nextBoolean()) {
            setIfInside(region, x + 1, y + 1, z, Material.BUDDING_AMETHYST);
        }
        if (random.nextBoolean()) {
            setIfInside(region, x - 1, y + 1, z, Material.CALCITE);
        }
    }

    private void placeFaeRuin(WorldInfo info, LimitedRegion region, Random random, int baseX, int baseZ) {
        int x = baseX + 5 + random.nextInt(6);
        int z = baseZ + 5 + random.nextInt(6);
        int y = region.getHighestBlockYAt(x, z);
        if (y < info.getMinHeight() || y + 6 >= info.getMaxHeight()) {
            return;
        }

        Material surface = region.getType(x, y, z);
        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
        if (surface != biome.surface()) {
            return;
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    if (random.nextDouble() < 0.74) {
                        setIfInside(region, x + dx, y + 1, z + dz, Material.MOSSY_STONE_BRICKS);
                    }
                }
            }
        }

        int pillarHeight = 3 + random.nextInt(3);
        for (int dy = 1; dy <= pillarHeight; dy++) {
            setIfInside(region, x, y + dy, z, dy == pillarHeight ? Material.CHISELED_STONE_BRICKS : Material.STONE_BRICKS);
        }
        setIfInside(region, x, y + pillarHeight + 1, z, Material.SOUL_LANTERN);
    }

    private void setIfInside(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) {
            region.setType(x, y, z, material);
        }
    }
}
