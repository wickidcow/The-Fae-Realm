package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/**
 * Immutable generator settings captured when the Fae Realm world is created.
 * World-generation settings require a restart because Minecraft binds a generator
 * instance to the world for the lifetime of that server session.
 */
public record FaeGeneratorSettings(
    TerrainPreset preset,
    double islandDensity,
    double verticalScale,
    double caveDensity,
    int cloudLevel,
    boolean terrainProfiles,
    boolean radiantEndLayout,
    double growthDensity,
    double decorationDensity,
    double resourceDensity,
    int structureSpacingChunks,
    int landmarkSpacingChunks,
    double dungeonChance,
    boolean clouds,
    boolean decorations,
    boolean structures,
    boolean resources
) {
    public static FaeGeneratorSettings defaults() {
        TerrainPreset preset = TerrainPreset.RADIANT_END;
        return new FaeGeneratorSettings(
            preset,
            preset.islandDensity(),
            preset.verticalScale(),
            preset.caveDensity(),
            preset.cloudLevel(),
            true,
            true,
            2.05,
            1.65,
            1.80,
            8,
            22,
            0.16,
            true,
            true,
            true,
            true
        );
    }

    public static FaeGeneratorSettings from(FileConfiguration config) {
        TerrainPreset preset = TerrainPreset.parse(config.getString("worldgen.preset", "radiant_end"));

        double density = clamp(
            numericOverride(config, "worldgen.island-density", preset.islandDensity()),
            0.25,
            1.75);
        double vertical = clamp(
            numericOverride(config, "worldgen.vertical-scale", preset.verticalScale()),
            0.50,
            1.80);
        double caves = clamp(
            numericOverride(config, "worldgen.cave-density", preset.caveDensity()),
            0.20,
            1.80);
        int cloudLevel = Math.max(48, Math.min(120,
            integerOverride(config, "worldgen.cloud-level", preset.cloudLevel())));

        boolean radiantEndLayout = config.getBoolean("worldgen.radiant-end-layout", true);
        if (radiantEndLayout && preset != TerrainPreset.RADIANT_END) {
            density = clamp(density * 0.82, 0.25, 1.75);
            vertical = clamp(vertical * 1.12, 0.50, 1.80);
            caves = clamp(caves * 0.96, 0.20, 1.80);
            cloudLevel = Math.min(cloudLevel, 70);
        }

        double growthDensity = clamp(
            numericOverride(config, "worldgen.growth-density", 2.05),
            0.0,
            3.0);
        double decorationDensity = clamp(
            numericOverride(config, "worldgen.decoration-density", 1.65),
            0.0,
            2.5);
        double resourceDensity = clamp(
            numericOverride(config, "worldgen.resource-density", 1.80),
            0.0,
            2.5);
        int structureSpacing = Math.max(6, Math.min(24,
            integerOverride(config, "worldgen.structure-spacing-chunks", 8)));
        int landmarkSpacing = Math.max(18, Math.min(48,
            integerOverride(config, "worldgen.landmark-spacing-chunks", 22)));
        double dungeonChance = clamp(
            numericOverride(config, "worldgen.dungeon-chance", 0.16),
            0.0,
            0.50);

        return new FaeGeneratorSettings(
            preset,
            density,
            vertical,
            caves,
            cloudLevel,
            config.getBoolean("worldgen.terrain-profiles", true),
            radiantEndLayout,
            growthDensity,
            decorationDensity,
            resourceDensity,
            structureSpacing,
            landmarkSpacing,
            dungeonChance,
            config.getBoolean("worldgen.clouds", true),
            config.getBoolean("worldgen.decorations", true),
            config.getBoolean("worldgen.structures", true),
            config.getBoolean("worldgen.resources", true)
        );
    }

    private static double numericOverride(FileConfiguration config, String path, double fallback) {
        Object value = config.get(path);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static int integerOverride(FileConfiguration config, String path, int fallback) {
        Object value = config.get(path);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum TerrainPreset {
        RADIANT_END(0.76, 1.28, 0.92, 68),
        BALANCED(1.00, 1.00, 1.00, 74),
        ETHEREAL(0.72, 1.24, 0.82, 70),
        LUSH(1.28, 0.88, 1.08, 78),
        WILD(0.98, 1.52, 1.36, 68);

        private final double islandDensity;
        private final double verticalScale;
        private final double caveDensity;
        private final int cloudLevel;

        TerrainPreset(double islandDensity, double verticalScale, double caveDensity, int cloudLevel) {
            this.islandDensity = islandDensity;
            this.verticalScale = verticalScale;
            this.caveDensity = caveDensity;
            this.cloudLevel = cloudLevel;
        }

        public double islandDensity() {
            return islandDensity;
        }

        public double verticalScale() {
            return verticalScale;
        }

        public double caveDensity() {
            return caveDensity;
        }

        public int cloudLevel() {
            return cloudLevel;
        }

        public static TerrainPreset parse(String configured) {
            if (configured == null) {
                return RADIANT_END;
            }
            try {
                return valueOf(configured.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException ignored) {
                return RADIANT_END;
            }
        }
    }
}
