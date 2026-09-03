package com.wickidcow.aetherlegacy.paper;

import com.wickidcow.aetherlegacy.paper.integration.BetterStructuresIntegration;
import com.wickidcow.aetherlegacy.paper.item.FaeItems;
import com.wickidcow.aetherlegacy.paper.loot.FaeDungeonLootListener;
import com.wickidcow.aetherlegacy.paper.portal.AetherPortalListener;
import com.wickidcow.aetherlegacy.paper.progression.FaeProgressionListener;
import com.wickidcow.aetherlegacy.paper.world.AetherChunkGenerator;
import com.wickidcow.aetherlegacy.paper.world.FaeGeneratorSettings;
import com.wickidcow.aetherlegacy.paper.world.FaeGeneratorVersion;
import com.wickidcow.aetherlegacy.paper.world.FaeRegionLocator;
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
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Paper entry point for the server-side Fae Realm generator. */
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

        boolean firstInitialization = !FaeWorldMetadata.exists(aetherWorld);
        configureRealm(aetherWorld);
        if (firstInitialization) {
            prepareArrivalArea(aetherWorld);
            getLogger().info("Initialized the Fae Realm arrival island and return portal.");
        } else {
            getLogger().info("Existing Fae Realm detected; preserving player changes around realm spawn.");
        }
        FaeWorldMetadata.record(this, aetherWorld, generatorSettings);

        FaeItems faeItems = new FaeItems(this);
        portalListener = new AetherPortalListener(this);
        getServer().getPluginManager().registerEvents(portalListener, this);
        getServer().getPluginManager().registerEvents(new FaeVoidListener(this, portalListener), this);
        getServer().getPluginManager().registerEvents(new FaeDungeonLootListener(this), this);
        getServer().getPluginManager().registerEvents(new FaeProgressionListener(this, faeItems), this);
        betterStructuresIntegration.enable();

        PluginCommand command = Objects.requireNonNull(getCommand("fae"), "fae command missing from plugin.yml");
        command.setExecutor((sender, ignored, label, args) -> handleCommand(sender, args));
        command.setTabCompleter((sender, ignored, label, args) -> tabComplete(sender, args));

        getLogger().info("The Fae Realm " + getPluginMeta().getVersion()
            + " enabled on Minecraft " + Bukkit.getMinecraftVersion());
        getLogger().info(getRealmDisplayName() + " active: generator v"
            + FaeGeneratorVersion.CURRENT + " / "
            + generatorSettings.preset().name().toLowerCase(Locale.ROOT)
            + ", floating continents, terrain profiles, regional biomes, resources, Fae structures, vaults, progression, portals, and /fae travel.");
    }

    private boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Use /fae info or /fae help from the console.");
                return true;
            }
            portalListener.rememberReturn(player, player.getLocation());
            player.teleport(getAetherArrivalLocation());
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> {
                sendInfo(sender);
                yield true;
            }
            case "help" -> {
                sendHelp(sender);
                yield true;
            }
            case "reload" -> {
                if (!isAdmin(sender)) {
                    sender.sendMessage(Component.text(
                        "You do not have permission to reload The Fae Realm.", NamedTextColor.RED));
                    yield true;
                }
                reloadConfig();
                configureRealm(aetherWorld);
                sender.sendMessage(Component.text(
                    "Fae Realm configuration reloaded. Generator, world-name, and integration-mode changes require a restart.",
                    NamedTextColor.GREEN));
                yield true;
            }
            case "locate" -> handleLocate(sender, args);
            case "return" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command must be run by a player.");
                    yield true;
                }
                player.teleport(portalListener.getReturnLocation(player));
                yield true;
            }
            case "biome" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command must be run by a player.");
                    yield true;
                }
                if (!player.getWorld().equals(aetherWorld)) {
                    player.sendMessage(Component.text(
                        "Enter the Fae Realm to inspect its region.", NamedTextColor.YELLOW));
                    yield true;
                }
                Location location = player.getLocation();
                var biome = AetherChunkGenerator.biomeAt(
                    aetherWorld.getSeed(), location.getBlockX(), location.getBlockZ());
                player.sendMessage(Component.text(
                    "Fae region: " + prettyName(biome.name()), NamedTextColor.AQUA));
                yield true;
            }
            default -> {
                sender.sendMessage(Component.text(
                    "Unknown /fae subcommand. Use /fae help.", NamedTextColor.YELLOW));
                yield true;
            }
        };
    }

    private boolean handleLocate(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) {
            sender.sendMessage(Component.text(
                "You do not have permission to use the Fae region locator.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text(
                "Usage: /fae locate <golden_meadows|crystal_woods|mist_gardens|ancient_fae_forest|sky_highlands>",
                NamedTextColor.YELLOW));
            return true;
        }

        var target = FaeRegionLocator.parseRegion(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Unknown Fae region: " + args[1], NamedTextColor.RED));
            return true;
        }

        int originX = 0;
        int originZ = 0;
        if (sender instanceof Player player && player.getWorld().equals(aetherWorld)) {
            originX = player.getLocation().getBlockX();
            originZ = player.getLocation().getBlockZ();
        }

        var result = FaeRegionLocator.findNearest(
            aetherWorld.getSeed(), originX, originZ, target, 4096);
        if (result == null) {
            sender.sendMessage(Component.text(
                "No " + prettyName(target.name()) + " sample was found within 4096 blocks.",
                NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text(
            "Nearest " + prettyName(target.name()) + " region sample: X " + result.x()
                + ", Z " + result.z() + " (~" + result.distance() + " blocks).",
            NamedTextColor.AQUA));
        sender.sendMessage(Component.text(
            "The locator does not generate chunks; the exact sample can be open sky even though the surrounding region identity is correct.",
            NamedTextColor.GRAY));
        return true;
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(Component.text("The Fae Realm ", NamedTextColor.LIGHT_PURPLE)
            .append(Component.text(getPluginMeta().getVersion(), NamedTextColor.AQUA)));
        sender.sendMessage(Component.text("Realm: " + getRealmDisplayName(), NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("World folder: " + aetherWorld.getName(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Seed: " + aetherWorld.getSeed(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(
            "Generator: v" + FaeGeneratorVersion.CURRENT + " / "
                + generatorSettings.preset().name().toLowerCase(Locale.ROOT),
            NamedTextColor.GRAY));
        sender.sendMessage(Component.text(
            "Terrain: density " + generatorSettings.islandDensity()
                + ", vertical " + generatorSettings.verticalScale()
                + ", caves " + generatorSettings.caveDensity()
                + ", profiles " + (generatorSettings.terrainProfiles() ? "on" : "off"),
            NamedTextColor.GRAY));
        sender.sendMessage(Component.text(
            "Layers: decorations x" + generatorSettings.decorationDensity()
                + ", resources x" + generatorSettings.resourceDensity()
                + ", structures every " + generatorSettings.structureSpacingChunks() + " chunks"
                + ", vault chance "
                + String.format(Locale.ROOT, "%.0f%%", generatorSettings.dungeonChance() * 100.0),
            NamedTextColor.GRAY));
        sender.sendMessage(Component.text(
            "BetterStructures: " + betterStructuresIntegration.status(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Paper target: 26.2 / Java 25", NamedTextColor.GRAY));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("The Fae Realm commands", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/fae — enter the Fae Realm", NamedTextColor.AQUA));
        sender.sendMessage(Component.text(
            "/fae return — return to your saved portal location", NamedTextColor.AQUA));
        sender.sendMessage(Component.text(
            "/fae biome — show the current Fae region", NamedTextColor.AQUA));
        sender.sendMessage(Component.text(
            "/fae info — show generator settings and integration status", NamedTextColor.AQUA));
        if (isAdmin(sender)) {
            sender.sendMessage(Component.text(
                "/fae locate <region> — locate a region without generating chunks", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text(
                "/fae reload — reload non-generator settings", NamedTextColor.YELLOW));
        }
    }

    private List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> choices = isAdmin(sender)
                ? List.of("return", "info", "biome", "locate", "reload", "help")
                : List.of("return", "info", "biome", "help");
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return choices.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("locate") && isAdmin(sender)) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return FaeRegionLocator.regionNames().stream()
                .filter(value -> value.startsWith(prefix))
                .toList();
        }
        return List.of();
    }

    private boolean isAdmin(CommandSender sender) {
        return sender.hasPermission("faerealm.admin") || sender.hasPermission("aetherlegacy.admin");
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

        for (int x = -1; x <= 2; x++) {
            for (int py = y; py <= y + 4; py++) {
                boolean border = x == -1 || x == 2 || py == y || py == y + 4;
                world.getBlockAt(x, py, 0).setType(border ? Material.GLOWSTONE : Material.WATER, false);
            }
        }
        world.setSpawnLocation(new Location(world, 0.5, y + 1, 3.5, 180.0f, 0.0f));
    }

    private String prettyName(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
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

    /**
     * Uses the persisted world spawn after first initialization so later config
     * edits cannot teleport players to an unbuilt Y level.
     */
    public @NotNull Location getAetherArrivalLocation() {
        Location spawn = getAetherWorld().getSpawnLocation().clone();
        spawn.setYaw(180.0f);
        spawn.setPitch(0.0f);
        return spawn;
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
