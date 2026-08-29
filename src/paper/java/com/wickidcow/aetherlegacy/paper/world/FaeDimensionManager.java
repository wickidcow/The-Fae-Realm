package com.wickidcow.aetherlegacy.paper.world;

import com.wickidcow.aetherlegacy.paper.AetherLegacyPlugin;
import com.wickidcow.aetherlegacy.paper.integration.BetterStructuresIntegration;
import com.wickidcow.aetherlegacy.paper.portal.FaeRiftListener;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/** Owns the linked Fae planes while keeping them on one shared generator engine. */
public final class FaeDimensionManager {

    private final AetherLegacyPlugin plugin;
    private final BetterStructuresIntegration betterStructures;
    private final FaeGeneratorSettings baseSettings;
    private final String rootWorldName;
    private final Map<FaePlane, World> worlds = new EnumMap<>(FaePlane.class);

    public FaeDimensionManager(AetherLegacyPlugin plugin,
                               BetterStructuresIntegration betterStructures,
                               FaeGeneratorSettings baseSettings,
                               String rootWorldName) {
        this.plugin = plugin;
        this.betterStructures = betterStructures;
        this.baseSettings = baseSettings;
        this.rootWorldName = rootWorldName;
    }

    public void registerMain(World world) {
        worlds.put(FaePlane.REALM, world);
    }

    public void loadSecondaryPlanes() {
        if (!plugin.getConfig().getBoolean("dimensions.enabled", true)) {
            plugin.getLogger().info("Linked Fae planes are disabled by configuration.");
            return;
        }

        for (FaePlane plane : new FaePlane[]{FaePlane.WILDBLOOM, FaePlane.GLOAM, FaePlane.STARFALL}) {
            if (!plugin.getConfig().getBoolean("dimensions." + plane.id() + ".enabled", true)) {
                continue;
            }
            World world = loadPlane(plane);
            if (world != null) {
                worlds.put(plane, world);
            }
        }

        plugin.getServer().getPluginManager().registerEvents(new FaeRiftListener(plugin), plugin);
    }

    private @Nullable World loadPlane(FaePlane plane) {
        String worldName = worldName(plane);
        betterStructures.prepareWorldExclusion(worldName);

        World existing = Bukkit.getWorld(worldName);
        FaeGeneratorSettings settings = baseSettings.forPlane(plane);
        World world = existing;
        if (world == null) {
            AetherChunkGenerator generator = new AetherChunkGenerator(settings);
            WorldCreator creator = new WorldCreator(worldName)
                .environment(World.Environment.NORMAL)
                .generator(generator)
                .generateStructures(false);

            long configuredSeed = plugin.getConfig().getLong("world.seed", 0L);
            if (configuredSeed != 0L) {
                creator.seed(planeSeed(configuredSeed, plane));
            }
            world = creator.createWorld();
        }

        if (world == null) {
            plugin.getLogger().warning("Could not create linked Fae plane " + plane.displayName() + ".");
            return null;
        }

        configureWorld(world);
        boolean firstInitialization = !FaeWorldMetadata.exists(world);
        if (firstInitialization) {
            prepareArrival(world, plane);
        }
        FaeWorldMetadata.record(plugin, world, settings);
        plugin.getLogger().info("Linked Fae plane active: " + plane.displayName()
            + " (" + world.getName() + ", generator v" + FaeGeneratorVersion.CURRENT + ").");
        return world;
    }

    private void configureWorld(World world) {
        world.setPVP(plugin.getConfig().getBoolean("world.pvp", true));
        world.setGameRule(GameRules.ADVANCE_TIME,
            plugin.getConfig().getBoolean("world.daylight-cycle", true));
        world.setGameRule(GameRules.ADVANCE_WEATHER,
            plugin.getConfig().getBoolean("world.weather-cycle", true));
        world.setGameRule(GameRules.SPAWN_MOBS,
            plugin.getConfig().getBoolean("world.mob-spawning", true));
    }

    private void prepareArrival(World world, FaePlane plane) {
        world.getChunkAt(0, 0).load();
        int surfaceY = world.getHighestBlockYAt(0, 0);
        int y = Math.max(world.getMinHeight() + 4, Math.min(world.getMaxHeight() - 8, surfaceY + 1));

        Material floor = switch (plane) {
            case REALM -> Material.GRASS_BLOCK;
            case WILDBLOOM -> Material.MOSS_BLOCK;
            case GLOAM -> Material.PALE_MOSS_BLOCK;
            case STARFALL -> Material.CALCITE;
        };
        Material edge = switch (plane) {
            case REALM -> Material.STONE_BRICKS;
            case WILDBLOOM -> Material.MOSSY_STONE_BRICKS;
            case GLOAM -> Material.POLISHED_TUFF;
            case STARFALL -> Material.AMETHYST_BLOCK;
        };
        Material light = switch (plane) {
            case REALM -> Material.GLOWSTONE;
            case WILDBLOOM -> Material.SHROOMLIGHT;
            case GLOAM -> Material.SOUL_LANTERN;
            case STARFALL -> Material.SEA_LANTERN;
        };

        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                boolean border = Math.abs(x) == 4 || Math.abs(z) == 4;
                world.getBlockAt(x, y - 1, z).setType(border ? edge : floor, false);
                world.getBlockAt(x, y, z).setType(Material.AIR, false);
                world.getBlockAt(x, y + 1, z).setType(Material.AIR, false);
            }
        }

        for (int[] corner : new int[][]{{-3, -3}, {-3, 3}, {3, -3}, {3, 3}}) {
            world.getBlockAt(corner[0], y, corner[1]).setType(edge, false);
            world.getBlockAt(corner[0], y + 1, corner[1]).setType(light, false);
        }

        Material marker = FaeRiftPopulator.marker(plane);
        world.getBlockAt(0, y, 0).setType(Material.LODESTONE, false);
        world.getBlockAt(1, y, 0).setType(marker, false);
        world.getBlockAt(-1, y, 0).setType(marker, false);
        world.getBlockAt(0, y, 1).setType(marker, false);
        world.getBlockAt(0, y, -1).setType(marker, false);

        world.setSpawnLocation(new Location(world, 0.5, y, 2.5, 180.0f, 0.0f));
    }

    public @Nullable World world(FaePlane plane) {
        return worlds.get(plane);
    }

    public Location arrival(FaePlane plane) {
        World world = worlds.get(plane);
        if (world == null) {
            world = worlds.get(FaePlane.REALM);
        }
        Location location = world.getSpawnLocation().clone();
        location.setYaw(180.0f);
        location.setPitch(0.0f);
        return location;
    }

    public boolean isFaeWorld(World world) {
        return worlds.containsValue(world);
    }

    public @Nullable FaePlane planeOf(World world) {
        for (Map.Entry<FaePlane, World> entry : worlds.entrySet()) {
            if (entry.getValue().equals(world)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String worldName(FaePlane plane) {
        if (plane == FaePlane.REALM) {
            return rootWorldName;
        }
        return plugin.getConfig().getString(
            "dimensions." + plane.id() + ".world-name",
            plane.defaultWorldName(rootWorldName));
    }

    private long planeSeed(long baseSeed, FaePlane plane) {
        long salt = switch (plane) {
            case REALM -> 0L;
            case WILDBLOOM -> 0x243F6A8885A308D3L;
            case GLOAM -> 0x13198A2E03707344L;
            case STARFALL -> 0xA4093822299F31D0L;
        };
        return baseSeed ^ salt;
    }
}
