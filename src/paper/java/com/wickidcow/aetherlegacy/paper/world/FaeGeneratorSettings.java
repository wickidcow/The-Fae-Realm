package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable generator settings captured when the Fae Realm world is created.
 * World-generation settings require a restart because Minecraft binds a generator
 * instance to the world for the lifetime of that server session.
 */
public record FaeGeneratorSettings(
    double islandDensity,
    boolean clouds,
    boolean decorations,
    boolean structures,
    boolean resources
) {
    public static FaeGeneratorSettings defaults() {
        return new FaeGeneratorSettings(1.0, true, true, true, true);
    }

    public static FaeGeneratorSettings from(FileConfiguration config) {
        double density = config.getDouble("worldgen.island-density", 1.0);
        density = Math.max(0.25, Math.min(1.75, density));
        return new FaeGeneratorSettings(
            density,
            config.getBoolean("worldgen.clouds", true),
            config.getBoolean("worldgen.decorations", true),
            config.getBoolean("worldgen.structures", true),
            config.getBoolean("worldgen.resources", true)
        );
    }
}
