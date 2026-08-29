package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/**
 * Immutable generator settings captured when a Fae world is created.
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
    double decorationDensity,
    double resourceDensity,
    int structureSpacingChunks,
    double dungeonChance,
    boolean clouds,
    boolean decorations,
    boolean structures,
    boolean resources
) {
    public static FaeGeneratorSettings defaults() {
        TerrainPreset preset = TerrainPreset.BALANCED;
        return new FaeGeneratorSettings(
            preset,
            preset.islandDensity(),
            preset.verticalScale(),
            preset.caveDensity(),
            preset.cloudLevel(),
            true,
            1.0,
            1.0,
            10,
            0.12,
            true,
            true,
            true,
            true
        );
    }

    public static FaeGeneratorSettings from(FileConfiguration config) {
        TerrainPreset preset = TerrainPreset.parse(config.getString("worldgen.preset", "balanced"));

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
        double decorationDensity = clamp(
            numericOverride(config, "worldgen.decoration-density", 1.0),
            0.0,
            2.5);
        double resourceDensity = clamp(
            numericOverride(config, "worldgen.resource-density", 1.0),
            0.0,
            2.5);
        int structureSpacing = Math.max(6, Math.min(24,
            integerOverride(config, "worldgen.structure-spacing-chunks", 10)));
        double dungeonChance = clamp(
            numericOverride(config, "worldgen.dungeon-chance", 0.12),
            0.0,
            0.50);

        return new FaeGeneratorSettings(
            preset,
            density,
            vertical,
            caves,
            cloudLevel,
            config.getBoolean("worldgen.terrain-profiles", true),
            decorationDensity,
            resourceDensity,
            structureSpacing,
            dungeonChance,
            config.getBoolean("worldgen.clouds", true),
            config.getBoolean("worldgen.decorations", true),
            config.getBoolean("worldgen.structures", true),
            config.getBoolean("worldgen.resources", true)
        );
    }

    /**
     * Creates a tuned settings snapshot for a linked Fae plane while preserving the
     * administrator's base generator configuration and layer toggles.
     */
    public FaeGeneratorSettings forPlane(FaePlane plane) {
        if (plane == null || plane == FaePlane.REALM) {
            return this;
        }

        return switch (plane) {
            case REALM -> this;
            case WILDBLOOM -> new FaeGeneratorSettings(
                preset,
                clamp(islandDensity * 1.22, 0.25, 1.75),
                clamp(verticalScale * 0.90, 0.50, 1.80),
                clamp(caveDensity * 0.88, 0.20, 1.80),
                Math.max(66, Math.min(92, cloudLevel + 5)),
                terrainProfiles,
                clamp(decorationDensity * 1.70, 0.0, 2.5),
                clamp(resourceDensity * 1.08, 0.0, 2.5),
                Math.max(8, Math.min(24, structureSpacingChunks + 2)),
                clamp(dungeonChance * 0.75, 0.0, 0.50),
                clouds,
                decorations,
                structures,
                resources
            );
            case GLOAM -> new FaeGeneratorSettings(
                preset,
                clamp(islandDensity * 0.90, 0.25, 1.75),
                clamp(verticalScale * 0.84, 0.50, 1.80),
                clamp(caveDensity * 1.52, 0.20, 1.80),
                Math.max(48, Math.min(76, cloudLevel - 10)),
                terrainProfiles,
                clamp(decorationDensity * 1.18, 0.0, 2.5),
                clamp(resourceDensity * 1.24, 0.0, 2.5),
                Math.max(6, structureSpacingChunks - 1),
                clamp(dungeonChance + 0.10, 0.0, 0.50),
                clouds,
                decorations,
                structures,
                resources
            );
            case STARFALL -> new FaeGeneratorSettings(
                preset,
                clamp(islandDensity * 0.82, 0.25, 1.75),
                clamp(verticalScale * 1.42, 0.50, 1.80),
                clamp(caveDensity * 1.12, 0.20, 1.80),
                Math.max(48, Math.min(82, cloudLevel - 4)),
                terrainProfiles,
                clamp(decorationDensity * 0.88, 0.0, 2.5),
                clamp(resourceDensity * 1.62, 0.0, 2.5),
                Math.max(6, structureSpacingChunks - 1),
                clamp(dungeonChance + 0.04, 0.0, 0.50),
                clouds,
                decorations,
                structures,
                resources
            );
        };
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
                return BALANCED;
            }
            try {
                return valueOf(configured.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return BALANCED;
            }
        }
    }
}
