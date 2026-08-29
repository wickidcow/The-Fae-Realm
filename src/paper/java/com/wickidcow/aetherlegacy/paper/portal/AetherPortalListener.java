package com.wickidcow.aetherlegacy.paper.portal;

import com.wickidcow.aetherlegacy.paper.AetherLegacyPlugin;
import com.wickidcow.aetherlegacy.paper.world.FaePlane;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AetherPortalListener implements Listener {

    private final AetherLegacyPlugin plugin;
    private final NamespacedKey returnLocationKey;
    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public AetherPortalListener(AetherLegacyPlugin plugin) {
        this.plugin = plugin;
        this.returnLocationKey = new NamespacedKey(plugin, "portal_return_location");
    }

    /**
     * Stores both a fast in-memory copy and a persistent player-data copy so the
     * return point survives disconnects and clean server restarts.
     */
    public void rememberReturn(Player player, Location location) {
        Location copy = location.clone();
        returnLocations.put(player.getUniqueId(), copy);
        player.getPersistentDataContainer().set(
            returnLocationKey,
            PersistentDataType.STRING,
            serializeLocation(copy)
        );
    }

    public Location getReturnLocation(Player player) {
        Location cached = returnLocations.get(player.getUniqueId());
        if (cached != null && cached.getWorld() != null && !plugin.isFaeWorld(cached.getWorld())) {
            return cached.clone();
        }

        String serialized = player.getPersistentDataContainer().get(
            returnLocationKey, PersistentDataType.STRING);
        Location persisted = deserializeLocation(serialized);
        if (persisted != null) {
            returnLocations.put(player.getUniqueId(), persisted.clone());
            return persisted;
        }
        return plugin.getDefaultReturnLocation();
    }

    /**
     * Classic Aether-style activation: one water bucket used inside a complete
     * Glowstone frame fills the entire 2x3 interior with water.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWaterBucket(PlayerBucketEmptyEvent event) {
        if (!plugin.getConfig().getBoolean("portal.enabled", true)
            || !plugin.getConfig().getBoolean("portal.auto-activate", true)
            || event.getBucket() != Material.WATER_BUCKET) {
            return;
        }

        Block target = event.getBlock();
        PortalFrame frame = findFrame(target);
        if (frame == null) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            frame.fillInterior();
            frame.world().playSound(frame.center(), Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 1.35f);
            event.getPlayer().sendMessage(Component.text("The way to the ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(plugin.getRealmDisplayName(), NamedTextColor.AQUA))
                .append(Component.text(" opens.", NamedTextColor.LIGHT_PURPLE)));
        });
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
        if (frame == null || !frame.hasWaterInterior()) {
            return;
        }

        int cooldownSeconds = Math.max(1, plugin.getConfig().getInt("portal.cooldown-seconds", 3));
        cooldownUntil.put(player.getUniqueId(), now + cooldownSeconds * 1000L);

        Location destination;
        if (plugin.isFaeWorld(player.getWorld())) {
            FaePlane plane = plugin.getFaePlane(player.getWorld());
            destination = plane == FaePlane.REALM
                ? getReturnLocation(player)
                : plugin.getAetherArrivalLocation();
        } else {
            rememberReturn(player, frame.safeExit());
            destination = plugin.getAetherArrivalLocation();
        }

        player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldownUntil.remove(event.getPlayer().getUniqueId());
        returnLocations.remove(event.getPlayer().getUniqueId());
    }

    private String serializeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return "";
        }
        return world.getUID() + ";"
            + location.getX() + ";"
            + location.getY() + ";"
            + location.getZ() + ";"
            + location.getYaw() + ";"
            + location.getPitch();
    }

    private @Nullable Location deserializeLocation(@Nullable String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return null;
        }

        String[] parts = serialized.split(";", -1);
        if (parts.length != 6) {
            return null;
        }

        try {
            World world = Bukkit.getWorld(UUID.fromString(parts[0]));
            if (world == null || plugin.isFaeWorld(world)) {
                return null;
            }
            return new Location(
                world,
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private @Nullable PortalFrame findFrame(Block candidate) {
        World world = candidate.getWorld();
        int x = candidate.getX();
        int y = candidate.getY();
        int z = candidate.getZ();

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
        private void fillInterior() {
            for (int y = bottomY + 1; y <= bottomY + 3; y++) {
                if (orientation == Orientation.X) {
                    world.getBlockAt(horizontalBase, y, plane).setType(Material.WATER, false);
                    world.getBlockAt(horizontalBase + 1, y, plane).setType(Material.WATER, false);
                } else {
                    world.getBlockAt(horizontalBase, y, plane).setType(Material.WATER, false);
                    world.getBlockAt(horizontalBase, y, plane + 1).setType(Material.WATER, false);
                }
            }
        }

        private boolean hasWaterInterior() {
            for (int y = bottomY + 1; y <= bottomY + 3; y++) {
                if (orientation == Orientation.X) {
                    if (world.getBlockAt(horizontalBase, y, plane).getType() != Material.WATER
                        || world.getBlockAt(horizontalBase + 1, y, plane).getType() != Material.WATER) {
                        return false;
                    }
                } else if (world.getBlockAt(horizontalBase, y, plane).getType() != Material.WATER
                    || world.getBlockAt(horizontalBase, y, plane + 1).getType() != Material.WATER) {
                    return false;
                }
            }
            return true;
        }

        private Location center() {
            if (orientation == Orientation.X) {
                return new Location(world, horizontalBase + 1.0, bottomY + 2.5, plane + 0.5);
            }
            return new Location(world, horizontalBase + 0.5, bottomY + 2.5, plane + 1.0);
        }

        private Location safeExit() {
            if (orientation == Orientation.X) {
                return new Location(world, horizontalBase + 1.0, bottomY + 1.0, plane + 2.5, 180.0f, 0.0f);
            }
            return new Location(world, horizontalBase + 2.5, bottomY + 1.0, plane + 1.0, 90.0f, 0.0f);
        }
    }
}
