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
 * <p>No BetterStructures classes are linked at compile time. Newer compatible builds can
 * disable BetterStructures scanning for the Fae Realm up front; older builds fall back to
 * a cancellable BuildPlaceEvent guard so generic structure packs still cannot overwrite
 * the realm's own structure identity.</p>
 */
public final class BetterStructuresIntegration {

    private final AetherLegacyPlugin plugin;
    private boolean detected;
    private boolean earlyWorldExclusion;
    private boolean guardRegistered;

    public BetterStructuresIntegration(AetherLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        Plugin betterStructures = plugin.getServer().getPluginManager().getPlugin("BetterStructures");
        detected = betterStructures != null && betterStructures.isEnabled();
        if (!detected) {
            return;
        }

        boolean allowGeneric = plugin.getConfig().getBoolean(
            "integrations.betterstructures.allow-generic-structures-in-fae-realm", false);

        if (allowGeneric) {
            plugin.getLogger().info("BetterStructures detected; generic BetterStructures generation is allowed in Fae Realm.");
            return;
        }

        earlyWorldExclusion = tryExcludeWorldBeforeScans(betterStructures);
        registerPlacementGuard(betterStructures);

        if (earlyWorldExclusion) {
            plugin.getLogger().info("BetterStructures detected; Fae Realm is excluded before BetterStructures chunk scans.");
        } else if (guardRegistered) {
            plugin.getLogger().info("BetterStructures detected; using placement guard fallback for Fae Realm.");
        }
    }

    private boolean tryExcludeWorldBeforeScans(Plugin betterStructures) {
        try {
            Class<?> validWorldsConfig = Class.forName(
                "com.magmaguy.betterstructures.config.ValidWorldsConfig",
                false,
                betterStructures.getClass().getClassLoader());
            Method setWorldValidity = validWorldsConfig.getMethod("setWorldValidity", World.class, boolean.class);
            Object result = setWorldValidity.invoke(null, plugin.getAetherWorld(), false);
            return result instanceof Boolean applied && applied;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Older BetterStructures builds do not expose the integration API. The event
            // guard below remains a safe compatibility fallback.
            return false;
        }
    }

    private void registerPlacementGuard(Plugin betterStructures) {
        try {
            Class<?> rawEventClass = Class.forName(
                "com.magmaguy.betterstructures.api.BuildPlaceEvent",
                false,
                betterStructures.getClass().getClassLoader());

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
        if (plugin.getConfig().getBoolean(
            "integrations.betterstructures.allow-generic-structures-in-fae-realm", false)) {
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
