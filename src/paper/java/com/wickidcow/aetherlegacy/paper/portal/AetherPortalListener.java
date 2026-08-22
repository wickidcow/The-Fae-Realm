package com.wickidcow.aetherlegacy.paper.portal;

import com.wickidcow.aetherlegacy.paper.AetherLegacyPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AetherPortalListener implements Listener {

    private final AetherLegacyPlugin plugin;
    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public AetherPortalListener(AetherLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    public void rememberReturn(Player player, Location location) {
        returnLocations.put(player.getUniqueId(), location.clone());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("portal.enabled", true)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld().equals(to.getWorld())
            && from.getBlockX() == to.getBlockX()
            && from.getBlockY() == to.getBlockY()
            && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (cooldownUntil.getOrDefault(player.getUniqueId(), 0L) > now) {
            return;
        }

        Block block = to.getBlock();
        if (block.getType() != Material.WATER) {
            return;
        }

        PortalFrame frame = findFrame(block);
        if (frame == null) {
            return;
        }

        int cooldownSeconds = Math.max(1, plugin.getConfig().getInt("portal.cooldown-seconds", 3));
        cooldownUntil.put(player.getUniqueId(), now + cooldownSeconds * 1000L);

        Location destination;
        if (player.getWorld().equals(plugin.getAetherWorld())) {
            destination = returnLocations.getOrDefault(player.getUniqueId(), plugin.getDefaultReturnLocation()).clone();
        } else {
            returnLocations.put(player.getUniqueId(), frame.safeExit());
            destination = plugin.getAetherArrivalLocation();
        }

        player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldownUntil.remove(event.getPlayer().getUniqueId());
        returnLocations.remove(event.getPlayer().getUniqueId());
    }

    private @Nullable PortalFrame findFrame(Block water) {
        World world = water.getWorld();
        int x = water.getX();
        int y = water.getY();
        int z = water.getZ();

        for (int bottomY = y - 3; bottomY <= y - 1; bottomY++) {
            for (int interiorMinX = x - 1; interiorMinX <= x; interiorMinX++) {
                if (isXFrame(world, interiorMinX, bottomY, z)) {
                    return new PortalFrame(world, interiorMinX, bottomY, z, Orientation.X);
                }
            }

            for (int interiorMinZ = z - 1; interiorMinZ <= z; interiorMinZ++) {
                if (isZFrame(world, x, bottomY, interiorMinZ)) {
                    return new PortalFrame(world, x, bottomY, interiorMinZ, Orientation.Z);
                }
            }
        }

        return null;
    }

    private boolean isXFrame(World world, int interiorMinX, int bottomY, int z) {
        for (int x = interiorMinX - 1; x <= interiorMinX + 2; x++) {
            if (!isGlowstone(world, x, bottomY, z) || !isGlowstone(world, x, bottomY + 4, z)) {
                return false;
            }
        }
        for (int y = bottomY + 1; y <= bottomY + 3; y++) {
            if (!isGlowstone(world, interiorMinX - 1, y, z)
                || !isGlowstone(world, interiorMinX + 2, y, z)) {
                return false;
            }
        }
        return true;
    }

    private boolean isZFrame(World world, int x, int bottomY, int interiorMinZ) {
        for (int z = interiorMinZ - 1; z <= interiorMinZ + 2; z++) {
            if (!isGlowstone(world, x, bottomY, z) || !isGlowstone(world, x, bottomY + 4, z)) {
                return false;
            }
        }
        for (int y = bottomY + 1; y <= bottomY + 3; y++) {
            if (!isGlowstone(world, x, y, interiorMinZ - 1)
                || !isGlowstone(world, x, y, interiorMinZ + 2)) {
                return false;
            }
        }
        return true;
    }

    private boolean isGlowstone(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z).getType() == Material.GLOWSTONE;
    }

    private enum Orientation {
        X, Z
    }

    private record PortalFrame(World world, int horizontalBase, int bottomY, int plane, Orientation orientation) {
        private Location safeExit() {
            if (orientation == Orientation.X) {
                return new Location(world, horizontalBase + 1.0, bottomY + 1.0, plane + 2.5, 180.0f, 0.0f);
            }
            return new Location(world, horizontalBase + 2.5, bottomY + 1.0, plane + 1.0, 90.0f, 0.0f);
        }
    }
}
