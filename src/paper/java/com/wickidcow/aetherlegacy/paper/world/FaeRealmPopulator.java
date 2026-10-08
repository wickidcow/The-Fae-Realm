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

    static {
        // Paper's CraftWorld populator list is a mutable ArrayList. Arm the guard
        // before WorldInitEvent so concurrent tools such as Chunky cannot race
        // another plugin mutating that list while FEATURES workers iterate it.
        FaePopulatorListGuard.register();
    }

    private static final FaeStructurePopulator STRUCTURES = new FaeStructurePopulator();
    private static final FaeLandmarkPopulator LANDMARKS = new FaeLandmarkPopulator();
    private static final FaeResourcePopulator RESOURCES = new FaeResourcePopulator();
    private static final FaeFeaturePopulator FEATURES = new FaeFeaturePopulator();
    private static final FaeUndersideGenerator UNDERSIDE = new FaeUndersideGenerator();
    private static final FaeFloraPopulator FLORA = new FaeFloraPopulator();
    private static final FaeGrowthPopulator GROWTH = new FaeGrowthPopulator();

    private final FaeGeneratorSettings settings;

    public FaeRealmPopulator() {
        this(FaeGeneratorSettings.defaults());
    }

    public FaeRealmPopulator(@NotNull FaeGeneratorSettings settings) {
        this.settings = settings;
    }

    @Override
    public void populate(@NotNull WorldInfo worldInfo,
                         @NotNull Random random,
                         int chunkX,
                         int chunkZ,
                         @NotNull LimitedRegion region) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        if (settings.resources() && settings.resourceDensity() > 0.0) {
            RESOURCES.populate(worldInfo, chunkX, chunkZ, region, settings.resourceDensity());
        }

        boolean landmarkPlaced = false;
        if (settings.decorations()) {
            if (settings.decorationDensity() > 0.0) {
                UNDERSIDE.populate(worldInfo, chunkX, chunkZ, region, settings);
                FLORA.populate(worldInfo, chunkX, chunkZ, region, settings);

                if (random.nextDouble() < scaledChance(1.0 / 9.0, settings.decorationDensity())) {
                    placeCrystalOutcrop(worldInfo, region, random, baseX, baseZ);
                }

                if (random.nextDouble() < scaledChance(1.0 / 96.0, settings.decorationDensity())) {
                    placeFaeRuin(worldInfo, region, random, baseX, baseZ);
                }
            }

            if (settings.growthDensity() > 0.0) {
                GROWTH.populate(worldInfo, chunkX, chunkZ, region, settings);
            }

            landmarkPlaced = LANDMARKS.populate(worldInfo, chunkX, chunkZ, region, settings);

            if (!landmarkPlaced && settings.decorationDensity() > 0.0) {
                FEATURES.populate(
                    worldInfo,
                    random,
                    chunkX,
                    chunkZ,
                    region,
                    settings.decorationDensity());
            }
        }

        if (settings.structures() && !landmarkPlaced) {
            STRUCTURES.populate(worldInfo, random, chunkX, chunkZ, region, settings);
        }
    }

    private double scaledChance(double baseChance, double density) {
        return Math.min(1.0, Math.max(0.0, baseChance * density));
    }

    private void placeCrystalOutcrop(WorldInfo info, LimitedRegion region, Random random, int baseX, int baseZ) {
        int x = baseX + 3 + random.nextInt(10);
        int z = baseZ + 3 + random.nextInt(10);
        int y = FaeSurfaceLocator.find(info, region, x, z);
        if (y == Integer.MIN_VALUE || y + 4 >= info.getMaxHeight()) {
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
        int y = FaeSurfaceLocator.find(info, region, x, z);
        if (y == Integer.MIN_VALUE || y + 6 >= info.getMaxHeight()) {
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
