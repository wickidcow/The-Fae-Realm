package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

/**
 * Deterministic floating-continent generator for the Fae Realm.
 *
 * <p>The generator is fully server-side and uses only vanilla block states. A broad
 * macro-noise field groups islands into denser archipelagos and quieter open-sky
 * regions, while per-island noise controls irregular coastlines, vertical relief,
 * caverns and regional palettes. Optional island profiles add plateaus, spires,
 * terraces and hollow landmasses without requiring client-side content.</p>
 */
public final class AetherChunkGenerator extends ChunkGenerator {

    private static final int CELL_SIZE = 128;
    private static final int MAX_RADIUS = 76;
    private static final int MIN_ISLAND_Y = 92;
    private static final int MAX_ISLAND_Y = 176;
    private static final int WORLD_MIN_ISLAND_Y = 78;
    private static final int WORLD_MAX_ISLAND_Y = 208;
    private static final long PROFILE_SALT = 0xD1310BA698DFB5ACL;

    private final FaeGeneratorSettings settings;

    public AetherChunkGenerator() {
        this(FaeGeneratorSettings.defaults());
    }

    public AetherChunkGenerator(@NotNull FaeGeneratorSettings settings) {
        this.settings = settings;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo,
                              @NotNull Random random,
                              int chunkX,
                              int chunkZ,
                              @NotNull ChunkData chunkData) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        long seed = worldInfo.getSeed();
        List<Island> islands = collectNearbyIslands(seed, minX, maxX, minZ, maxZ);
        for (Island island : islands) {
            carveIsland(chunkData, seed, island, minX, minZ);
        }

        if (settings.clouds()) {
            generateCloudShelf(chunkData, seed, minX, minZ);
        }
    }

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo,
                                @NotNull Random random,
                                int chunkX,
                                int chunkZ,
                                @NotNull ChunkData chunkData) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        long seed = worldInfo.getSeed();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minX + localX;
                int worldZ = minZ + localZ;
                int top = highestSolid(chunkData, localX, localZ);
                if (top < chunkData.getMinHeight()) {
                    continue;
                }

                Material topType = chunkData.getType(localX, top, localZ);
                if (topType == Material.WHITE_WOOL || topType == Material.SNOW_BLOCK) {
                    continue;
                }

                FaeRealmBiome biome = biomeAt(seed, worldX, worldZ);
                chunkData.setBlock(localX, top, localZ, biome.surface());
                for (int depth = 1; depth <= 3; depth++) {
                    int y = top - depth;
                    if (y >= chunkData.getMinHeight() && chunkData.getType(localX, y, localZ).isSolid()) {
                        chunkData.setBlock(localX, y, localZ, biome.subsurface());
                    }
                }

                if (settings.decorations()) {
                    placeSurfaceDetails(chunkData, seed, biome, worldX, top, worldZ, localX, localZ);
                }
            }
        }
    }

    @Override
    public void generateBedrock(@NotNull WorldInfo worldInfo,
                                @NotNull Random random,
                                int chunkX,
                                int chunkZ,
                                @NotNull ChunkData chunkData) {
        // Intentionally empty: the Fae Realm has open void beneath the islands.
    }

    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return List.of(new FaeRealmPopulator(settings));
    }

    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new FaeBiomeProvider();
    }

    private List<Island> collectNearbyIslands(long worldSeed, int minX, int maxX, int minZ, int maxZ) {
        List<Island> islands = new ArrayList<>();

        if (intersects(minX, maxX, minZ, maxZ, 0, 0, 58)) {
            // The origin deliberately stays broad and calm so first arrival is always safe.
            islands.add(new Island(0, 0, 132, 58, 24, 1.0, IslandProfile.PLATEAU));
        }

        int minCellX = Math.floorDiv(minX - MAX_RADIUS, CELL_SIZE);
        int maxCellX = Math.floorDiv(maxX + MAX_RADIUS, CELL_SIZE);
        int minCellZ = Math.floorDiv(minZ - MAX_RADIUS, CELL_SIZE);
        int maxCellZ = Math.floorDiv(maxZ + MAX_RADIUS, CELL_SIZE);
        double baseIslandChance = clamp(0.20, 0.98, 0.86 * settings.islandDensity());
        double baseSatelliteChance = clamp(0.08, 0.70, 0.34 * settings.islandDensity());

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                if (Math.abs(cellX) <= 1 && Math.abs(cellZ) <= 1) {
                    continue;
                }

                int macroX = cellX * CELL_SIZE + CELL_SIZE / 2;
                int macroZ = cellZ * CELL_SIZE + CELL_SIZE / 2;
                double macro = FaeNoise.fractal(
                    worldSeed ^ 0xCBBB9D5DC1059ED8L,
                    macroX * 0.0018,
                    macroZ * 0.0018,
                    3,
                    2.0,
                    0.52);
                double macro01 = clamp(0.0, 1.0, (macro + 1.0) * 0.5);

                SplittableRandom cellRandom = new SplittableRandom(mixSeed(worldSeed, cellX, cellZ));
                double localIslandChance = clamp(0.12, 0.99,
                    baseIslandChance * (0.68 + macro01 * 0.62));
                if (cellRandom.nextDouble() > localIslandChance) {
                    continue;
                }

                int centerX = cellX * CELL_SIZE + cellRandom.nextInt(20, CELL_SIZE - 20);
                int centerZ = cellZ * CELL_SIZE + cellRandom.nextInt(20, CELL_SIZE - 20);
                IslandProfile profile = chooseProfile(worldSeed, cellX, cellZ);

                int rawCenterY = cellRandom.nextInt(MIN_ISLAND_Y, MAX_ISLAND_Y + 1);
                int midpoint = (MIN_ISLAND_Y + MAX_ISLAND_Y) / 2;
                int scaledY = midpoint + (int) Math.round((rawCenterY - midpoint) * settings.verticalScale());
                int macroLift = (int) Math.round(macro * 13.0 * settings.verticalScale());
                int centerY = clampInt(WORLD_MIN_ISLAND_Y, WORLD_MAX_ISLAND_Y, scaledY + macroLift);

                int radius = clampInt(22, MAX_RADIUS,
                    cellRandom.nextInt(26, 65) + (int) Math.round(macro * 12.0));
                int thickness = clampInt(12, 34,
                    cellRandom.nextInt(14, 30) + (int) Math.round(macro01 * 5.0));
                double warp = 0.78 + cellRandom.nextDouble() * 0.48;

                switch (profile) {
                    case PLATEAU -> {
                        radius = clampInt(28, MAX_RADIUS, radius + 9);
                        thickness = clampInt(14, 36, thickness + 2);
                    }
                    case SPIRE -> {
                        radius = clampInt(18, 58, radius - 7);
                        thickness = clampInt(17, 40, thickness + 6);
                        centerY = clampInt(WORLD_MIN_ISLAND_Y, WORLD_MAX_ISLAND_Y, centerY + 8);
                    }
                    case TERRACED -> radius = clampInt(26, MAX_RADIUS, radius + 4);
                    case HOLLOW -> {
                        radius = clampInt(26, MAX_RADIUS, radius + 6);
                        thickness = clampInt(18, 40, thickness + 5);
                    }
                    case BALANCED -> {
                        // Base dimensions already represent the balanced profile.
                    }
                }

                if (intersects(minX, maxX, minZ, maxZ, centerX, centerZ, radius)) {
                    islands.add(new Island(centerX, centerZ, centerY, radius, thickness, warp, profile));
                }

                double localSatelliteChance = clamp(0.05, 0.78,
                    baseSatelliteChance * (0.72 + macro01 * 0.58));
                int satelliteCount = cellRandom.nextDouble() < localSatelliteChance ? 1 : 0;
                if (satelliteCount > 0 && macro01 > 0.72 && cellRandom.nextDouble() < 0.28) {
                    satelliteCount++;
                }

                for (int satellite = 0; satellite < satelliteCount; satellite++) {
                    int satelliteX = centerX + cellRandom.nextInt(-72, 73);
                    int satelliteZ = centerZ + cellRandom.nextInt(-72, 73);
                    int satelliteY = centerY + cellRandom.nextInt(-18, 19);
                    int satelliteRadius = cellRandom.nextInt(10, 24);
                    IslandProfile satelliteProfile = settings.terrainProfiles() && cellRandom.nextInt(5) == 0
                        ? IslandProfile.SPIRE
                        : IslandProfile.BALANCED;
                    if (intersects(minX, maxX, minZ, maxZ, satelliteX, satelliteZ, satelliteRadius)) {
                        islands.add(new Island(
                            satelliteX,
                            satelliteZ,
                            clampInt(WORLD_MIN_ISLAND_Y, WORLD_MAX_ISLAND_Y, satelliteY),
                            satelliteRadius,
                            cellRandom.nextInt(8, 16),
                            0.7 + cellRandom.nextDouble() * 0.4,
                            satelliteProfile));
                    }
                }
            }
        }

        return islands;
    }

    private IslandProfile chooseProfile(long worldSeed, int cellX, int cellZ) {
        if (!settings.terrainProfiles()) {
            return IslandProfile.BALANCED;
        }
        SplittableRandom profileRandom = new SplittableRandom(
            mixSeed(worldSeed ^ PROFILE_SALT, cellX, cellZ));
        int roll = profileRandom.nextInt(100);
        if (roll < 34) {
            return IslandProfile.BALANCED;
        }
        if (roll < 53) {
            return IslandProfile.PLATEAU;
        }
        if (roll < 69) {
            return IslandProfile.SPIRE;
        }
        if (roll < 85) {
            return IslandProfile.TERRACED;
        }
        return IslandProfile.HOLLOW;
    }

    private void carveIsland(ChunkData chunkData, long seed, Island island, int chunkMinX, int chunkMinZ) {
        int minHeight = chunkData.getMinHeight();
        int maxHeight = chunkData.getMaxHeight() - 1;

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkMinX + localX;
            double dx = worldX - island.x();

            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkMinZ + localZ;
                double dz = worldZ - island.z();

                double warpNoise = FaeNoise.fractal(seed ^ 0x4F9939F508L,
                    worldX * 0.018, worldZ * 0.018, 3, 2.1, 0.52);
                double warpedDx = dx * (1.0 + warpNoise * 0.16 * island.warp());
                double warpedDz = dz * (1.0 - warpNoise * 0.12 * island.warp());
                double distance = Math.sqrt(warpedDx * warpedDx + warpedDz * warpedDz);

                double edgeNoise = FaeNoise.fractal(seed ^ 0xA4B1C39D2EL,
                    worldX * 0.031, worldZ * 0.031, 3, 2.0, 0.5) * 5.0;
                double effectiveRadius = island.radius() + edgeNoise;
                if (distance > effectiveRadius) {
                    continue;
                }

                double edge = 1.0 - (distance / Math.max(1.0, effectiveRadius));
                double terrainNoise = FaeNoise.fractal(seed ^ 0x1D872B41L,
                    worldX * 0.024, worldZ * 0.024, 4, 2.05, 0.5);
                double ridge = Math.abs(FaeNoise.fractal(seed ^ 0x7F4A7C15L,
                    worldX * 0.011, worldZ * 0.011, 3, 2.0, 0.5));

                int topY = island.y() + profileRelief(island.profile(), edge, terrainNoise, ridge);
                double depthMultiplier = switch (island.profile()) {
                    case SPIRE -> 1.20;
                    case HOLLOW -> 1.14;
                    case PLATEAU -> 1.05;
                    case TERRACED -> 0.96;
                    case BALANCED -> 1.0;
                };
                int depth = Math.max(4, (int) Math.round(
                    island.thickness() * (0.16 + edge * edge * 0.9) * depthMultiplier));
                int bottomY = topY - depth;

                topY = Math.min(topY, maxHeight);
                bottomY = Math.max(bottomY, minHeight);

                FaeRealmBiome biome = biomeAt(seed, worldX, worldZ);
                for (int y = bottomY; y <= topY; y++) {
                    if (isCavern(seed, worldX, y, worldZ, topY, bottomY, edge, island.profile())) {
                        continue;
                    }

                    Material material;
                    if (y == topY) {
                        material = biome.surface();
                    } else if (y >= topY - 3) {
                        material = biome.subsurface();
                    } else if (y <= bottomY + 1 && edge < 0.5) {
                        material = Material.CALCITE;
                    } else {
                        material = biome.core();
                    }
                    chunkData.setBlock(localX, y, localZ, material);
                }
            }
        }
    }

    private int profileRelief(IslandProfile profile, double edge, double terrainNoise, double ridge) {
        double vertical = settings.verticalScale();
        double relief = switch (profile) {
            case BALANCED -> edge * 7.0 * Math.min(1.35, vertical)
                + terrainNoise * 7.0 * vertical
                + ridge * edge * 5.0 * vertical;
            case PLATEAU -> edge * 4.5 * Math.min(1.15, vertical)
                + terrainNoise * 3.2 * vertical
                + ridge * edge * 1.8 * vertical;
            case SPIRE -> Math.pow(edge, 1.65) * 18.0 * Math.min(1.55, vertical)
                + terrainNoise * 9.0 * vertical
                + ridge * edge * 8.0 * vertical;
            case TERRACED -> {
                double raw = edge * 9.0 * Math.min(1.35, vertical)
                    + terrainNoise * 5.0 * vertical
                    + ridge * edge * 3.2 * vertical;
                yield Math.rint(raw / 4.0) * 4.0;
            }
            case HOLLOW -> edge * 6.0 * Math.min(1.30, vertical)
                + terrainNoise * 6.2 * vertical
                + ridge * edge * 4.0 * vertical;
        };
        return (int) Math.round(relief);
    }

    private boolean isCavern(long seed, int worldX, int y, int worldZ,
                             int topY, int bottomY, double edge, IslandProfile profile) {
        double minimumEdge = profile == IslandProfile.HOLLOW ? 0.20 : 0.28;
        if (edge < minimumEdge || y > topY - 5 || y < bottomY + 3) {
            return false;
        }

        double vertical = (y - bottomY) / (double) Math.max(1, topY - bottomY);
        double cave = FaeNoise.fractal(seed ^ 0x6A09E667F3BCC909L,
            worldX * 0.055, (worldZ + y * 2.7) * 0.055, 3, 2.0, 0.55);
        double threshold = clamp(0.40, 0.68,
            0.53 - ((settings.caveDensity() - 1.0) * 0.12));
        threshold += switch (profile) {
            case HOLLOW -> -0.08;
            case SPIRE -> 0.03;
            case PLATEAU -> 0.02;
            case TERRACED -> 0.01;
            case BALANCED -> 0.0;
        };
        threshold = clamp(0.36, 0.72, threshold);
        return cave > threshold && vertical > 0.22 && vertical < 0.78;
    }

    private void generateCloudShelf(ChunkData chunkData, long seed, int chunkMinX, int chunkMinZ) {
        int cloudY = settings.cloudLevel();
        if (cloudY < chunkData.getMinHeight() || cloudY + 1 >= chunkData.getMaxHeight()) {
            return;
        }

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkMinX + localX;
                int worldZ = chunkMinZ + localZ;
                double cloud = FaeNoise.fractal(seed ^ 0xBB67AE8584CAA73BL,
                    worldX * 0.021, worldZ * 0.021, 3, 2.0, 0.55);
                if (cloud > 0.64) {
                    chunkData.setBlock(localX, cloudY, localZ, Material.WHITE_WOOL);
                    if (cloud > 0.75) {
                        chunkData.setBlock(localX, cloudY + 1, localZ, Material.SNOW_BLOCK);
                    }
                }
            }
        }
    }

    private void placeSurfaceDetails(ChunkData chunkData, long seed, FaeRealmBiome biome,
                                     int worldX, int topY, int worldZ, int localX, int localZ) {
        int detailY = topY + 1;
        if (detailY >= chunkData.getMaxHeight()) {
            return;
        }

        long hash = mixSeed(seed ^ 0x3C6EF372FE94F82BL, worldX, worldZ);
        int roll = (int) Math.floorMod(hash, 1000L);

        if (roll < 11) {
            chunkData.setBlock(localX, detailY, localZ, biome.accent());
        } else if (roll < 15 && biome == FaeRealmBiome.GOLDEN_MEADOWS) {
            chunkData.setBlock(localX, detailY, localZ, Material.SUNFLOWER);
        } else if (roll < 18 && biome == FaeRealmBiome.CRYSTAL_WOODS) {
            chunkData.setBlock(localX, detailY, localZ, Material.AMETHYST_CLUSTER);
        } else if (roll < 22 && biome == FaeRealmBiome.MIST_GARDENS) {
            chunkData.setBlock(localX, detailY, localZ, Material.PALE_MOSS_CARPET);
        }
    }

    public static FaeRealmBiome biomeAt(long seed, int worldX, int worldZ) {
        double broad = FaeNoise.fractal(seed ^ 0x510E527FADE682D1L,
            worldX * 0.0042, worldZ * 0.0042, 3, 2.0, 0.5);
        double secondary = FaeNoise.fractal(seed ^ 0x9B05688C2B3E6C1FL,
            worldX * 0.0067, worldZ * 0.0067, 2, 2.0, 0.5);

        if (broad < -0.42) {
            return FaeRealmBiome.MIST_GARDENS;
        }
        if (broad < -0.08) {
            return secondary > 0.18 ? FaeRealmBiome.CRYSTAL_WOODS : FaeRealmBiome.GOLDEN_MEADOWS;
        }
        if (broad < 0.33) {
            return secondary < -0.16 ? FaeRealmBiome.ANCIENT_FAE_FOREST : FaeRealmBiome.CRYSTAL_WOODS;
        }
        return secondary > 0.15 ? FaeRealmBiome.SKY_HIGHLANDS : FaeRealmBiome.ANCIENT_FAE_FOREST;
    }

    private int highestSolid(ChunkData chunkData, int localX, int localZ) {
        for (int y = chunkData.getMaxHeight() - 1; y >= chunkData.getMinHeight(); y--) {
            if (chunkData.getType(localX, y, localZ).isSolid()) {
                return y;
            }
        }
        return chunkData.getMinHeight() - 1;
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return true;
    }

    private static boolean intersects(int minX, int maxX, int minZ, int maxZ,
                                      int centerX, int centerZ, int radius) {
        return centerX + radius >= minX
            && centerX - radius <= maxX
            && centerZ + radius >= minZ
            && centerZ - radius <= maxZ;
    }

    private static double clamp(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int min, int max, int value) {
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

    private enum IslandProfile {
        BALANCED,
        PLATEAU,
        SPIRE,
        TERRACED,
        HOLLOW
    }

    private record Island(
        int x,
        int z,
        int y,
        int radius,
        int thickness,
        double warp,
        IslandProfile profile
    ) {
    }
}
