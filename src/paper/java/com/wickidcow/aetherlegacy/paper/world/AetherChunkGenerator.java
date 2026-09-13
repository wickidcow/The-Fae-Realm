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
    private static final int MAX_SEARCH_RADIUS = 100;
    private static final int MIN_ISLAND_Y = 92;
    private static final int MAX_ISLAND_Y = 176;
    private static final int WORLD_MIN_ISLAND_Y = 78;
    private static final int WORLD_MAX_ISLAND_Y = 208;
    private static final long PROFILE_SALT = 0xD1310BA698DFB5ACL;
    private static final long COAST_SALT = 0x8C3C010CB4754C9DL;
    private static final long HILL_SALT = 0x4E6D8A77D17E5B73L;
    private static final long CLEARING_SALT = 0xA8F0D7458F6503A9L;
    private static final long UNDERSIDE_SALT = 0xC5B4A8133E0D3A4FL;

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

        if (intersects(minX, maxX, minZ, maxZ, 0, 0, 70)) {
            // The arrival island keeps a calm center, but the outer ring now rolls into hills and cliffs.
            islands.add(new Island(0, 0, 132, 60, 27, 0.78, 1.0, IslandProfile.ARRIVAL));
        }

        int minCellX = Math.floorDiv(minX - MAX_SEARCH_RADIUS, CELL_SIZE);
        int maxCellX = Math.floorDiv(maxX + MAX_SEARCH_RADIUS, CELL_SIZE);
        int minCellZ = Math.floorDiv(minZ - MAX_SEARCH_RADIUS, CELL_SIZE);
        int maxCellZ = Math.floorDiv(maxZ + MAX_SEARCH_RADIUS, CELL_SIZE);
        double baseIslandChance = clamp(0.20, 0.98, 0.86 * settings.islandDensity());
        double baseSatelliteChance = clamp(0.10, 0.76, 0.40 * settings.islandDensity());

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
                int thickness = clampInt(12, 36,
                    cellRandom.nextInt(15, 31) + (int) Math.round(macro01 * 6.0));
                double warp = 0.82 + cellRandom.nextDouble() * 0.52;
                double aspect = 0.88 + cellRandom.nextDouble() * 0.26;

                switch (profile) {
                    case PLATEAU -> {
                        radius = clampInt(28, MAX_RADIUS, radius + 9);
                        thickness = clampInt(15, 38, thickness + 3);
                    }
                    case SPIRE -> {
                        radius = clampInt(18, 60, radius - 6);
                        thickness = clampInt(19, 43, thickness + 8);
                        centerY = clampInt(WORLD_MIN_ISLAND_Y, WORLD_MAX_ISLAND_Y, centerY + 10);
                    }
                    case TERRACED -> {
                        radius = clampInt(27, MAX_RADIUS, radius + 5);
                        thickness = clampInt(15, 38, thickness + 2);
                    }
                    case HOLLOW -> {
                        radius = clampInt(26, MAX_RADIUS, radius + 6);
                        thickness = clampInt(20, 43, thickness + 7);
                    }
                    case BALANCED, ARRIVAL -> {
                        // Base dimensions already represent these profiles.
                    }
                }

                int boundsRadius = boundsRadius(radius, aspect);
                if (intersects(minX, maxX, minZ, maxZ, centerX, centerZ, boundsRadius)) {
                    islands.add(new Island(centerX, centerZ, centerY, radius, thickness, warp, aspect, profile));
                }

                double localSatelliteChance = clamp(0.07, 0.84,
                    baseSatelliteChance * (0.72 + macro01 * 0.64));
                int satelliteCount = cellRandom.nextDouble() < localSatelliteChance ? 1 : 0;
                if (satelliteCount > 0 && macro01 > 0.64 && cellRandom.nextDouble() < 0.38) {
                    satelliteCount++;
                }

                for (int satellite = 0; satellite < satelliteCount; satellite++) {
                    int satelliteX = centerX + cellRandom.nextInt(-78, 79);
                    int satelliteZ = centerZ + cellRandom.nextInt(-78, 79);
                    int satelliteY = centerY + cellRandom.nextInt(-27, 28);
                    int satelliteRadius = cellRandom.nextInt(9, 26);
                    double satelliteAspect = 0.86 + cellRandom.nextDouble() * 0.32;
                    IslandProfile satelliteProfile = settings.terrainProfiles() && cellRandom.nextInt(4) == 0
                        ? IslandProfile.SPIRE
                        : IslandProfile.BALANCED;
                    int satelliteBounds = boundsRadius(satelliteRadius, satelliteAspect);
                    if (intersects(minX, maxX, minZ, maxZ, satelliteX, satelliteZ, satelliteBounds)) {
                        islands.add(new Island(
                            satelliteX,
                            satelliteZ,
                            clampInt(WORLD_MIN_ISLAND_Y, WORLD_MAX_ISLAND_Y, satelliteY),
                            satelliteRadius,
                            cellRandom.nextInt(9, 18),
                            0.78 + cellRandom.nextDouble() * 0.48,
                            satelliteAspect,
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
        if (roll < 30) {
            return IslandProfile.BALANCED;
        }
        if (roll < 45) {
            return IslandProfile.PLATEAU;
        }
        if (roll < 65) {
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
        double phase = coastPhase(seed, island.x(), island.z());

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkMinX + localX;
            double dx = worldX - island.x();

            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkMinZ + localZ;
                double dz = worldZ - island.z();

                double warpNoise = FaeNoise.fractal(seed ^ 0x4F9939F508L,
                    worldX * 0.018, worldZ * 0.018, 3, 2.1, 0.52);
                double broadWarp = FaeNoise.fractal(seed ^ COAST_SALT,
                    worldX * 0.0085, worldZ * 0.0085, 3, 2.0, 0.54);
                double warpedDx = dx * (1.0 + warpNoise * 0.19 * island.warp()) / island.aspect();
                double warpedDz = dz * (1.0 - warpNoise * 0.15 * island.warp()) * island.aspect();
                double distance = Math.sqrt(warpedDx * warpedDx + warpedDz * warpedDz);
                double angle = Math.atan2(warpedDz, warpedDx);

                double edgeNoise = FaeNoise.fractal(seed ^ 0xA4B1C39D2EL,
                    worldX * 0.033, worldZ * 0.033, 3, 2.0, 0.5) * 4.5;
                double lobeStrength = Math.min(6.5, island.radius() * 0.10);
                double lobes = (Math.sin(angle * 3.0 + phase) * 0.62
                    + Math.cos(angle * 5.0 - phase * 0.73) * 0.38) * lobeStrength;
                double broadCoast = broadWarp * Math.min(8.0, island.radius() * 0.12);
                double coastScale = island.profile() == IslandProfile.ARRIVAL ? 0.55 : 1.0;
                double effectiveRadius = island.radius() + (edgeNoise + lobes + broadCoast) * coastScale;
                if (distance > effectiveRadius) {
                    continue;
                }

                double edge = clamp(0.0, 1.0,
                    1.0 - (distance / Math.max(1.0, effectiveRadius)));
                double terrainNoise = FaeNoise.fractal(seed ^ 0x1D872B41L,
                    worldX * 0.023, worldZ * 0.023, 4, 2.05, 0.5);
                double hillNoise = FaeNoise.fractal(seed ^ HILL_SALT,
                    worldX * 0.0088, worldZ * 0.0088, 3, 2.0, 0.54);
                double fineNoise = FaeNoise.fractal(seed ^ 0x72D17C3E5B9A4F11L,
                    worldX * 0.052, worldZ * 0.052, 2, 2.0, 0.5);
                double ridgeField = FaeNoise.fractal(seed ^ 0x7F4A7C15L,
                    worldX * 0.012, worldZ * 0.012, 3, 2.0, 0.5);
                double ridge = Math.pow(1.0 - Math.abs(ridgeField), 2.0);
                double valley = FaeNoise.fractal(seed ^ 0x35D9965A7F319E4BL,
                    worldX * 0.014, worldZ * 0.014, 3, 2.0, 0.52);

                int relief = profileRelief(
                    island.profile(), edge, terrainNoise, hillNoise, fineNoise, ridge, valley);
                relief = softenForNaturalClearings(seed, island.profile(), worldX, worldZ, edge, relief);
                int topY = island.y() + relief;

                double depthMultiplier = switch (island.profile()) {
                    case ARRIVAL -> 1.06;
                    case SPIRE -> 1.28;
                    case HOLLOW -> 1.18;
                    case PLATEAU -> 1.08;
                    case TERRACED -> 1.02;
                    case BALANCED -> 1.06;
                };
                double underside = FaeNoise.fractal(seed ^ UNDERSIDE_SALT,
                    worldX * 0.016, worldZ * 0.016, 3, 2.0, 0.55);
                double body = 0.18 + Math.pow(edge, 1.72) * 1.06;
                int depth = Math.max(5, (int) Math.round(
                    island.thickness() * body * depthMultiplier
                        + Math.max(0.0, underside) * 7.0 * settings.verticalScale()
                        + ridge * edge * 3.0));
                depth += hangingUndersideDepth(island.profile(), edge, underside, ridge);
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
                    } else if (y <= bottomY + 2 && (edge < 0.52 || underside > 0.48)) {
                        material = Material.CALCITE;
                    } else {
                        material = biome.core();
                    }
                    chunkData.setBlock(localX, y, localZ, material);
                }
            }
        }
    }

    private int profileRelief(IslandProfile profile,
                              double edge,
                              double terrainNoise,
                              double hillNoise,
                              double fineNoise,
                              double ridge,
                              double valley) {
        double vertical = settings.verticalScale();
        double valleyCut = Math.min(0.0, valley);
        double relief = switch (profile) {
            case ARRIVAL -> {
                double centerNoise = clamp(0.0, 1.0, (1.0 - edge) / 0.30);
                yield edge * 6.0
                    + terrainNoise * 3.0 * vertical * centerNoise
                    + hillNoise * 4.2 * vertical * centerNoise
                    + ridge * edge * 2.2 * vertical
                    + valleyCut * 2.0 * vertical;
            }
            case BALANCED -> edge * 9.0 * Math.min(1.40, vertical)
                + terrainNoise * 7.5 * vertical
                + hillNoise * 8.5 * vertical
                + ridge * edge * 7.0 * vertical
                + valleyCut * 5.0 * vertical
                + fineNoise * 1.4 * vertical;
            case PLATEAU -> edge * 6.0 * Math.min(1.25, vertical)
                + terrainNoise * 4.8 * vertical
                + hillNoise * 7.0 * vertical
                + ridge * edge * 4.0 * vertical
                + valleyCut * 4.6 * vertical
                + fineNoise * 1.2 * vertical;
            case SPIRE -> Math.pow(edge, 1.48) * 24.0 * Math.min(1.60, vertical)
                + terrainNoise * 10.0 * vertical
                + hillNoise * 11.0 * vertical
                + ridge * edge * 10.0 * vertical
                + valleyCut * 4.0 * vertical
                + fineNoise * 2.0 * vertical;
            case TERRACED -> {
                double raw = edge * 12.0 * Math.min(1.42, vertical)
                    + terrainNoise * 6.0 * vertical
                    + hillNoise * 7.5 * vertical
                    + ridge * edge * 4.6 * vertical
                    + valleyCut * 4.2 * vertical;
                double step = edge > 0.58 ? 3.0 : 4.0;
                yield Math.rint(raw / step) * step + fineNoise * 1.4 * vertical;
            }
            case HOLLOW -> edge * 8.0 * Math.min(1.38, vertical)
                + terrainNoise * 7.2 * vertical
                + hillNoise * 8.0 * vertical
                + ridge * edge * 5.2 * vertical
                + valleyCut * 5.5 * vertical
                + fineNoise * 1.3 * vertical;
        };
        return clampInt(-16, 52, (int) Math.round(relief));
    }

    private int softenForNaturalClearings(long seed,
                                          IslandProfile profile,
                                          int worldX,
                                          int worldZ,
                                          double edge,
                                          int relief) {
        if (profile == IslandProfile.SPIRE || edge < 0.34) {
            return relief;
        }

        double clearing = FaeNoise.fractal(seed ^ CLEARING_SALT,
            worldX * 0.0064, worldZ * 0.0064, 2, 2.0, 0.56);
        if (clearing < 0.54) {
            return relief;
        }

        double strength = clamp(0.0, 0.68, (clearing - 0.54) * 1.75);
        double calmLift = switch (profile) {
            case ARRIVAL -> edge * 6.0;
            case PLATEAU -> edge * 6.0;
            case TERRACED -> Math.rint((edge * 10.0) / 3.0) * 3.0;
            case HOLLOW -> edge * 7.0;
            case BALANCED -> edge * 8.0;
            case SPIRE -> relief;
        };
        return (int) Math.round(relief * (1.0 - strength) + calmLift * strength);
    }

    private int hangingUndersideDepth(IslandProfile profile,
                                      double edge,
                                      double underside,
                                      double ridge) {
        if (edge < 0.10 || underside < 0.46) {
            return 0;
        }
        double profileScale = switch (profile) {
            case SPIRE -> 1.75;
            case HOLLOW -> 1.45;
            case TERRACED -> 1.15;
            case BALANCED -> 1.05;
            case PLATEAU -> 0.90;
            case ARRIVAL -> 0.65;
        };
        double taper = Math.sin(Math.PI * clamp(0.0, 1.0, edge));
        return Math.max(0, (int) Math.round(
            (underside - 0.46) * 24.0 * profileScale * taper + ridge * 2.5));
    }

    private boolean isCavern(long seed, int worldX, int y, int worldZ,
                             int topY, int bottomY, double edge, IslandProfile profile) {
        if (profile == IslandProfile.ARRIVAL) {
            return false;
        }

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
            case ARRIVAL -> 0.20;
        };
        threshold = clamp(0.36, 0.76, threshold);
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

    private static int boundsRadius(int radius, double aspect) {
        double stretch = Math.max(aspect, 1.0 / aspect);
        return (int) Math.ceil(radius * stretch + 12.0);
    }

    private static double coastPhase(long seed, int x, int z) {
        long mixed = mixSeed(seed ^ COAST_SALT, x, z);
        double normalized = (mixed & 0xFFFFL) / 65535.0;
        return normalized * Math.PI * 2.0;
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
        ARRIVAL,
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
        double aspect,
        IslandProfile profile
    ) {
    }
}
