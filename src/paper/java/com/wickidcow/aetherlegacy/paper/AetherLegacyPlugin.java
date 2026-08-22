package com.wickidcow.aetherlegacy.paper;

import com.wickidcow.aetherlegacy.paper.portal.AetherPortalListener;
import com.wickidcow.aetherlegacy.paper.world.AetherChunkGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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

public final class AetherLegacyPlugin extends JavaPlugin {

    private AetherChunkGenerator generator;
    private AetherPortalListener portalListener;
    private World aetherWorld;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        generator = new AetherChunkGenerator();
        aetherWorld = loadAetherWorld();

        if (aetherWorld == null) {
            getLogger().severe("Unable to create or load the Aether world. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        prepareArrivalArea(aetherWorld);

        portalListener = new AetherPortalListener(this);
        getServer().getPluginManager().registerEvents(portalListener, this);

        PluginCommand command = Objects.requireNonNull(getCommand("aether"), "aether command missing from plugin.yml");
        command.setExecutor((sender, ignored, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command must be run by a player.");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("return")) {
                player.teleport(getDefaultReturnLocation());
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
                player.sendMessage(Component.text("Aether Legacy for Paper ", NamedTextColor.GOLD)
                    .append(Component.text(getPluginMeta().getVersion(), NamedTextColor.YELLOW)));
                player.sendMessage(Component.text("World: " + aetherWorld.getName(), NamedTextColor.GRAY));
                player.sendMessage(Component.text("Paper target: 26.2 / Java 25", NamedTextColor.GRAY));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("aetherlegacy.admin")) {
                    player.sendMessage(Component.text("You do not have permission to reload Aether Legacy.", NamedTextColor.RED));
                    return true;
                }
                reloadConfig();
                player.sendMessage(Component.text("Aether Legacy configuration reloaded. World-name changes require a restart.", NamedTextColor.GREEN));
                return true;
            }

            portalListener.rememberReturn(player, player.getLocation());
            player.teleport(getAetherArrivalLocation());
            return true;
        });

        getLogger().info("Aether Legacy for Paper " + getPluginMeta().getVersion() + " enabled on " + Bukkit.getMinecraftVersion());
        getLogger().info("Paper port foundation active: custom floating-island world, Glowstone-water portals, and /aether travel.");
    }

    private World loadAetherWorld() {
        String worldName = getConfig().getString("world.name", "aether");
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

        // Classic Aether-style 4x5 Glowstone frame. The interior remains water.
        for (int x = -1; x <= 2; x++) {
            for (int py = y; py <= y + 4; py++) {
                boolean border = x == -1 || x == 2 || py == y || py == y + 4;
                world.getBlockAt(x, py, 0).setType(border ? Material.GLOWSTONE : Material.WATER, false);
            }
        }

        world.setSpawnLocation(new Location(world, 0.5, y + 1, 3.5));
    }

    public @NotNull World getAetherWorld() {
        return Objects.requireNonNull(aetherWorld, "Aether world is not loaded");
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
