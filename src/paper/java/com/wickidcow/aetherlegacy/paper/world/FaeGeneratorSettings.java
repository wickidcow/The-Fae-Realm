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
            true,
            true,
            true,
            true
        );
    }

    public static FaeGeneratorSettings from(FileConfiguration config) {
        TerrainPreset preset = TerrainPreset.parse(config.getString("worldgen.preset", "balanced"));

        double density = clamp(
            config.getDouble("worldgen.island-density", preset.islandDensity()),
            0.25,
            1.75);
        double vertical = clamp(
            config.getDouble("worldgen.vertical-scale", preset.verticalScale()),
            0.50,
            1.80);
        double caves = clamp(
            config.getDouble("worldgen.cave-density", preset.caveDensity()),
            0.20,
            1.80);
        int cloudLevel = Math.max(48, Math.min(120,
            config.getInt("worldgen.cloud-level", preset.cloudLevel())));

        return new FaeGeneratorSettings(
            preset,
            density,
            vertical,
            caves,
            cloudLevel,
            config.getBoolean("worldgen.terrain-profiles", true),
            config.getBoolean("worldgen.clouds", true),
            config.getBoolean("worldgen.decorations", true),
            config.getBoolean("worldgen.structures", true),
            config.getBoolean("worldgen.resources", true)
        );
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
