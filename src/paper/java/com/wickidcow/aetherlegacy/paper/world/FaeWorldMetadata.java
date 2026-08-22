package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Persists lightweight generator provenance inside the Fae Realm world folder.
 */
public final class FaeWorldMetadata {

    private static final String FILE_NAME = "fae-realm-generator.yml";

    private FaeWorldMetadata() {
    }

    public static void record(JavaPlugin plugin, World world, FaeGeneratorSettings settings) {
        File file = new File(world.getWorldFolder(), FILE_NAME);
        YamlConfiguration metadata = YamlConfiguration.loadConfiguration(file);
        int currentVersion = plugin.getConfig().getInt("worldgen.version", 5);
        int previousVersion = metadata.getInt("current-generator-version", -1);

        if (previousVersion > 0 && previousVersion != currentVersion) {
            plugin.getLogger().warning(
                "Fae Realm generator changed from v" + previousVersion + " to v" + currentVersion
                    + ". Existing chunks are preserved; newly explored chunks will use the new generator.");
        }

        if (!metadata.contains("first-generator-version")) {
            metadata.set("first-generator-version", currentVersion);
        }
        if (!metadata.contains("created-seed")) {
            metadata.set("created-seed", world.getSeed());
        }

        metadata.set("current-generator-version", currentVersion);
        metadata.set("current-preset", settings.preset().name().toLowerCase());
        metadata.set("current-island-density", settings.islandDensity());
        metadata.set("current-vertical-scale", settings.verticalScale());
        metadata.set("current-cave-density", settings.caveDensity());
        metadata.set("plugin-version", plugin.getPluginMeta().getVersion());
        metadata.set("world-name", world.getName());

        try {
            metadata.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save " + FILE_NAME + ": " + exception.getMessage());
        }
    }
}
