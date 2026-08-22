package com.wickidcow.aetherlegacy.paper;

import com.wickidcow.aetherlegacy.paper.integration.BetterStructuresIntegration;
import com.wickidcow.aetherlegacy.paper.item.FaeItems;
import com.wickidcow.aetherlegacy.paper.loot.FaeDungeonLootListener;
import com.wickidcow.aetherlegacy.paper.portal.AetherPortalListener;
import com.wickidcow.aetherlegacy.paper.progression.FaeProgressionListener;
import com.wickidcow.aetherlegacy.paper.world.AetherChunkGenerator;
import com.wickidcow.aetherlegacy.paper.world.FaeGeneratorSettings;
import com.wickidcow.aetherlegacy.paper.world.FaeVoidListener;
import com.wickidcow.aetherlegacy.paper.world.FaeWorldMetadata;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Paper entry point for The Fae Realm.
 *
 * <p>The legacy package/class name is retained temporarily so existing development
 * branches and plugin data remain compatible while the public project identity is
 * The Fae Realm.</p>
 */
public final class AetherLegacyPlugin extends JavaPlugin {

    private FaeGeneratorSettings generatorSettings;
    private AetherChunkGenerator generator;
    private AetherPortalListener portalListener;
    private BetterStructuresIntegration betterStructuresIntegration;
    private World aetherWorld;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        generatorSettings = FaeGeneratorSettings.from(getConfig());
        generator = new AetherChunkGenerator(generatorSettings);

        String worldName = getConfig().getString("world.name", "fae_realm");
        betterStructuresIntegration = new BetterStructuresIntegration(this);
        betterStructuresIntegration.prepareWorldExclusion(worldName);

        aetherWorld = loadAetherWorld(worldName);
        if (aetherWorld == null) {
            getLogger().severe("Unable to create or load the Fae Realm. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configureRealm(aetherWorld);
        prepareArrivalArea(aetherWorld);
        FaeWorldMetadata.record(this, aetherWorld, generatorSettings);

        FaeItems faeItems = new FaeItems(this);
        portalListener = new AetherPortalListener(this);
        getServer().getPluginManager().registerEvents(portalListener, this);
        getServer().getPluginManager().registerEvents(new FaeVoidListener(this), this);
        getServer().getPluginManager().registerEvents(new FaeDungeonLootListener(this), this);
        getServer().getPluginManager().registerEvents(new FaeProgressionListener(this, faeItems), this);

        betterStructuresIntegration.enable();

        PluginCommand command = Objects.requireNonNull(getCommand("fae"), "fae command missing from plugin.yml");
        command.setExecutor((sender, ignored, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command must be run by a player.");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("return")) {
                player.teleport(portalListener.getReturnLocation(player));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
                player.sendMessage(Component.text("The Fae Realm ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(getPluginMeta().getVersion(), NamedTextColor.AQUA)));
                player.sendMessage(Component.text("Realm: " + getRealmDisplayName(), NamedTextColor.LIGHT_PURPLE));
                player.sendMessage(Component.text("World folder: " + aetherWorld.getName(), NamedTextColor.GRAY));
                player.sendMessage(Component.text("Seed: " + aetherWorld.getSeed(), NamedTextColor.GRAY));
                player.sendMessage(Component.text(
                    "Generator: v" + getConfig().getInt("worldgen.version", 5)
                        + " / " + generatorSettings.preset().name().toLowerCase(),
                    NamedTextColor.GRAY));
                player.sendMessage(Component.text(
                    "Terrain: density " + generatorSettings.islandDensity()
                        + ", vertical " + generatorSettings.verticalScale()
                        + ", caves " + generatorSettings.caveDensity(),
                    NamedTextColor.GRAY));
                player.sendMessage(Component.text("BetterStructures: " + betterStructuresIntegration.status(), NamedTextColor.GRAY));
                player.sendMessage(Component.text("Paper target: 26.2 / Java 25", NamedTextColor.GRAY));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("biome")) {
                if (!player.getWorld().equals(aetherWorld)) {
                    player.sendMessage(Component.text("Enter the Fae Realm to inspect its region.", NamedTextColor.YELLOW));
                    return true;
                }
                Location location = player.getLocation();
                var biome = AetherChunkGenerator.biomeAt(
                    aetherWorld.getSeed(), location.getBlockX(), location.getBlockZ());
                player.sendMessage(Component.text(
                    "Fae region: " + prettyName(biome.name()), NamedTextColor.AQUA));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("faerealm.admin") && !player.hasPermission("aetherlegacy.admin")) {
                    player.sendMessage(Component.text("You do not have permission to reload The Fae Realm.", NamedTextColor.RED));
                    return true;
                }
                reloadConfig();
                configureRealm(aetherWorld);
                player.sendMessage(Component.text("Fae Realm configuration reloaded. Generator, world-name, and integration-mode changes require a restart.", NamedTextColor.GREEN));
                return true;
            }

            portalListener.rememberReturn(player, player.getLocation());
            player.teleport(getAetherArrivalLocation());
            return true;
        });

        getLogger().info("The Fae Realm " + getPluginMeta().getVersion() + " enabled on Minecraft " + Bukkit.getMinecraftVersion());
        getLogger().info(getRealmDisplayName() + " active: generator v"
            + getConfig().getInt("worldgen.version", 5) + " / "
            + generatorSettings.preset().name().toLowerCase()
            + ", floating continents, regional biomes, resources, Fae structures, vaults, progression, portals, and /fae travel.");
    }

    private World loadAetherWorld(String worldName) {
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            return existing;
        }

        WorldCreator creator = new WorldCreator(worldName)
            .environment(World.Environment.NORMAL)
            .generator(generator)
            .generateStructures(false);

        long configuredSeed = getConfig().getLong("world.seed", 0L);
        if (configuredSeed != 0L) {
            creator.seed(configuredSeed);
        }

        return creator.createWorld();
    }

    private void configureRealm(World world) {
        world.setPVP(getConfig().getBoolean("world.pvp", true));
        world.setGameRule(GameRules.ADVANCE_TIME,
            getConfig().getBoolean("world.daylight-cycle", true));
        world.setGameRule(GameRules.ADVANCE_WEATHER,
            getConfig().getBoolean("world.weather-cycle", true));
        world.setGameRule(GameRules.SPAWN_MOBS,
            getConfig().getBoolean("world.mob-spawning", true));
    }

    private void prepareArrivalArea(World world) {
        int y = getConfig().getInt("world.spawn-y", 136);
        world.getChunkAt(0, 0).load();

        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                world.getBlockAt(x, y - 1, z).setType(Material.STONE, false);
                if (Math.abs(x) <= 3 && Math.abs(z) <= 3) {
                    world.getBlockAt(x, y, z).setType(Material.GRASS_BLOCK, false);
                }
            }
        }

        // Classic fantasy-sky portal: a 4x5 Glowstone frame with a water interior.
        for (int x = -1; x <= 2; x++) {
            for (int py = y; py <= y + 4; py++) {
                boolean border = x == -1 || x == 2 || py == y || py == y + 4;
                world.getBlockAt(x, py, 0).setType(border ? Material.GLOWSTONE : Material.WATER, false);
            }
        }

        world.setSpawnLocation(new Location(world, 0.5, y + 1, 3.5));
    }

    private String prettyName(String enumName) {
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    public @NotNull World getAetherWorld() {
        return Objects.requireNonNull(aetherWorld, "Fae Realm is not loaded");
    }

    public @NotNull String getRealmDisplayName() {
        return getConfig().getString("world.display-name", "Fae Realm");
    }

    public @NotNull Location getAetherArrivalLocation() {
        int y = getConfig().getInt("world.spawn-y", 136);
        return new Location(getAetherWorld(), 0.5, y + 1, 3.5, 180.0f, 0.0f);
    }

    public @NotNull Location getDefaultReturnLocation() {
        for (World world : Bukkit.getWorlds()) {
            if (!world.equals(aetherWorld) && world.getEnvironment() == World.Environment.NORMAL) {
                return world.getSpawnLocation().clone().add(0.5, 0.0, 0.5);
            }
        }
        return Bukkit.getWorlds().getFirst().getSpawnLocation().clone().add(0.5, 0.0, 0.5);
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, String id) {
        return generator == null ? new AetherChunkGenerator() : generator;
    }
}
