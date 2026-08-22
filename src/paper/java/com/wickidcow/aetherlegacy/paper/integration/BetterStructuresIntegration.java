package com.wickidcow.aetherlegacy.paper.integration;

import com.wickidcow.aetherlegacy.paper.AetherLegacyPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Optional runtime bridge for BetterStructures.
 *
 * <p>No BetterStructures classes are linked at compile time. Compatible builds can
 * exclude the configured Fae world by name before WorldCreator begins generating its
 * first chunks. Older builds fall back to a cancellable BuildPlaceEvent guard.</p>
 */
public final class BetterStructuresIntegration {

    private final AetherLegacyPlugin plugin;
    private Plugin betterStructures;
    private boolean detected;
    private boolean earlyWorldExclusion;
    private boolean guardRegistered;

    public BetterStructuresIntegration(AetherLegacyPlugin plugin) {
        this.plugin = plugin;
        refreshDetection();
    }

    /**
     * Called before the Fae Realm exists so compatible BetterStructures builds can
     * reject the world name before any spawn chunks are generated.
     */
    public void prepareWorldExclusion(String worldName) {
        refreshDetection();
        if (!detected || allowGenericStructures()) {
            return;
        }

        earlyWorldExclusion = trySetWorldValidity(String.class, worldName, false);
        if (earlyWorldExclusion) {
            plugin.getLogger().info("BetterStructures detected; pre-excluded world '" + worldName
                + "' before Fae Realm creation.");
        }
    }

    /** Finalizes compatibility after the Fae world is available. */
    public void enable() {
        refreshDetection();
        if (!detected) {
            return;
        }

        if (allowGenericStructures()) {
            plugin.getLogger().info("BetterStructures detected; generic BetterStructures generation is allowed in Fae Realm.");
            return;
        }

        if (!earlyWorldExclusion) {
            earlyWorldExclusion = trySetWorldValidity(World.class, plugin.getAetherWorld(), false);
        }
        if (!earlyWorldExclusion) {
            registerPlacementGuard(betterStructures);
        }

        if (earlyWorldExclusion) {
            plugin.getLogger().info("BetterStructures detected; Fae Realm is excluded before BetterStructures chunk scans.");
        } else if (guardRegistered) {
            plugin.getLogger().info("BetterStructures detected; using placement guard fallback for Fae Realm.");
        }
    }

    private void refreshDetection() {
        betterStructures = plugin.getServer().getPluginManager().getPlugin("BetterStructures");
        detected = betterStructures != null && betterStructures.isEnabled();
    }

    private boolean allowGenericStructures() {
        return plugin.getConfig().getBoolean(
            "integrations.betterstructures.allow-generic-structures-in-fae-realm", false);
    }

    private boolean trySetWorldValidity(Class<?> worldArgumentType, Object worldArgument, boolean valid) {
        if (betterStructures == null) {
            return false;
        }
        try {
            Class<?> validWorldsConfig = Class.forName(
                "com.magmaguy.betterstructures.config.ValidWorldsConfig",
                false,
                betterStructures.getClass().getClassLoader());
            Method setWorldValidity = validWorldsConfig.getMethod(
                "setWorldValidity", worldArgumentType, boolean.class);
            setWorldValidity.invoke(null, worldArgument, valid);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private void registerPlacementGuard(Plugin betterStructuresPlugin) {
        if (guardRegistered || betterStructuresPlugin == null) {
            return;
        }
        try {
            Class<?> rawEventClass = Class.forName(
                "com.magmaguy.betterstructures.api.BuildPlaceEvent",
                false,
                betterStructuresPlugin.getClass().getClassLoader());

            if (!Event.class.isAssignableFrom(rawEventClass)) {
                plugin.getLogger().warning("BetterStructures BuildPlaceEvent was found but is not a Bukkit Event; compatibility guard skipped.");
                return;
            }

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass;
            Method getFitAnything = rawEventClass.getMethod("getFitAnything");

            Listener listener = new Listener() { };
            plugin.getServer().getPluginManager().registerEvent(
                eventClass,
                listener,
                EventPriority.LOWEST,
                (ignored, event) -> {
                    if (!(event instanceof Cancellable cancellable) || cancellable.isCancelled()) {
                        return;
                    }
                    try {
                        Object fitAnything = getFitAnything.invoke(event);
                        if (fitAnything == null) {
                            return;
                        }
                        Method getLocation = fitAnything.getClass().getMethod("getLocation");
                        Object rawLocation = getLocation.invoke(fitAnything);
                        if (!(rawLocation instanceof Location location) || location.getWorld() == null) {
                            return;
                        }
                        if (location.getWorld().equals(plugin.getAetherWorld())) {
                            cancellable.setCancelled(true);
                        }
                    } catch (ReflectiveOperationException exception) {
                        plugin.getLogger().fine("Could not inspect BetterStructures placement event: " + exception.getMessage());
                    }
                },
                plugin,
                true
            );

            guardRegistered = true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("BetterStructures detected, but compatibility guard could not be registered: "
                + exception.getMessage());
        }
    }

    public boolean isDetected() {
        return detected;
    }

    public boolean isEarlyWorldExclusionActive() {
        return earlyWorldExclusion;
    }

    public boolean isGuardRegistered() {
        return guardRegistered;
    }

    public String status() {
        if (!detected) {
            return "not detected";
        }
        if (allowGenericStructures()) {
            return "detected (generic Fae Realm structures allowed)";
        }
        if (earlyWorldExclusion) {
            return "detected (Fae Realm excluded before scans)";
        }
        if (guardRegistered) {
            return "detected (placement guard fallback active)";
        }
        return "detected (compatibility guard unavailable)";
    }
}
