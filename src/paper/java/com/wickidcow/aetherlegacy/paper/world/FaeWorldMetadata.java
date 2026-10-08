package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/** Persists lightweight generator provenance inside the Fae Realm world folder. */
public final class FaeWorldMetadata {

    private static final String FILE_NAME = "fae-realm-generator.yml";

    private FaeWorldMetadata() {
    }

    public static boolean exists(World world) {
        return new File(world.getWorldFolder(), FILE_NAME).isFile();
    }

    public static void record(JavaPlugin plugin, World world, FaeGeneratorSettings settings) {
        File file = new File(world.getWorldFolder(), FILE_NAME);
        YamlConfiguration metadata = YamlConfiguration.loadConfiguration(file);
        int currentVersion = FaeGeneratorVersion.CURRENT;
        int previousVersion = metadata.getInt("current-generator-version", -1);
        String currentFingerprint = fingerprint(settings);
        String previousFingerprint = metadata.getString("current-settings-fingerprint");

        if (previousVersion > 0 && previousVersion != currentVersion) {
            plugin.getLogger().warning(
                "Fae Realm generator changed from v" + previousVersion + " to v" + currentVersion
                    + ". Existing chunks are preserved; newly explored chunks will use the new generator.");
        }
        if (previousFingerprint != null && !previousFingerprint.equals(currentFingerprint)) {
            plugin.getLogger().warning(
                "Fae Realm generator settings changed since the previous boot. Existing chunks are preserved; "
                    + "newly explored chunks will use the current preset/density/growth/structure settings.");
        }

        if (!metadata.contains("first-generator-version")) {
            metadata.set("first-generator-version", currentVersion);
        }
        if (!metadata.contains("created-seed")) {
            metadata.set("created-seed", world.getSeed());
        }
        if (!metadata.contains("first-settings-fingerprint")) {
            metadata.set("first-settings-fingerprint", currentFingerprint);
            metadata.set("first-settings.preset", settings.preset().name().toLowerCase(Locale.ROOT));
            metadata.set("first-settings.island-density", settings.islandDensity());
            metadata.set("first-settings.vertical-scale", settings.verticalScale());
            metadata.set("first-settings.cave-density", settings.caveDensity());
            metadata.set("first-settings.terrain-profiles", settings.terrainProfiles());
            metadata.set("first-settings.radiant-end-layout", settings.radiantEndLayout());
            metadata.set("first-settings.growth-density", settings.growthDensity());
            metadata.set("first-settings.decoration-density", settings.decorationDensity());
            metadata.set("first-settings.resource-density", settings.resourceDensity());
            metadata.set("first-settings.structure-spacing-chunks", settings.structureSpacingChunks());
            metadata.set("first-settings.landmark-spacing-chunks", settings.landmarkSpacingChunks());
            metadata.set("first-settings.dungeon-chance", settings.dungeonChance());
        }

        metadata.set("current-generator-version", currentVersion);
        metadata.set("current-settings-fingerprint", currentFingerprint);
        metadata.set("current-preset", settings.preset().name().toLowerCase(Locale.ROOT));
        metadata.set("current-island-density", settings.islandDensity());
        metadata.set("current-vertical-scale", settings.verticalScale());
        metadata.set("current-cave-density", settings.caveDensity());
        metadata.set("terrain-profiles", settings.terrainProfiles());
        metadata.set("radiant-end-layout", settings.radiantEndLayout());
        metadata.set("growth-density", settings.growthDensity());
        metadata.set("decoration-density", settings.decorationDensity());
        metadata.set("resource-density", settings.resourceDensity());
        metadata.set("structure-spacing-chunks", settings.structureSpacingChunks());
        metadata.set("landmark-spacing-chunks", settings.landmarkSpacingChunks());
        metadata.set("dungeon-chance", settings.dungeonChance());
        metadata.set("plugin-version", plugin.getPluginMeta().getVersion());
        metadata.set("world-name", world.getName());

        try {
            metadata.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save " + FILE_NAME + ": " + exception.getMessage());
        }
    }

    private static String fingerprint(FaeGeneratorSettings settings) {
        return String.format(Locale.ROOT,
            "%s|%.5f|%.5f|%.5f|%d|%s|%s|%.5f|%.5f|%.5f|%d|%d|%.5f|%s|%s|%s|%s",
            settings.preset().name(),
            settings.islandDensity(),
            settings.verticalScale(),
            settings.caveDensity(),
            settings.cloudLevel(),
            settings.terrainProfiles(),
            settings.radiantEndLayout(),
            settings.growthDensity(),
            settings.decorationDensity(),
            settings.resourceDensity(),
            settings.structureSpacingChunks(),
            settings.landmarkSpacingChunks(),
            settings.dungeonChance(),
            settings.clouds(),
            settings.decorations(),
            settings.structures(),
            settings.resources());
    }
}
