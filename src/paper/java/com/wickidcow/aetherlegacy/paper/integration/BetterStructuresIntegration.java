package com.wickidcow.aetherlegacy.paper.integration;

import com.wickidcow.aetherlegacy.paper.AetherLegacyPlugin;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Optional runtime bridge for BetterStructures.
 *
 * <p>No BetterStructures classes are linked at compile time. When the plugin is present,
 * this bridge registers against its cancellable BuildPlaceEvent by reflection so generic
 * structure packs can be kept out of the Fae Realm unless explicitly enabled.</p>
 */
public final class BetterStructuresIntegration {

    private final AetherLegacyPlugin plugin;
    private boolean detected;
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
            plugin.getLogger().info("BetterStructures detected; generic structure pastes are guarded from Fae Realm.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("BetterStructures detected, but compatibility guard could not be registered: "
                + exception.getMessage());
        }
    }

    public boolean isDetected() {
        return detected;
    }

    public boolean isGuardRegistered() {
        return guardRegistered;
    }

    public String status() {
        if (!detected) {
            return "not detected";
        }
        if (guardRegistered) {
            return "detected (generic Fae Realm structures blocked)";
        }
        if (plugin.getConfig().getBoolean(
            "integrations.betterstructures.allow-generic-structures-in-fae-realm", false)) {
            return "detected (generic Fae Realm structures allowed)";
        }
        return "detected (compatibility guard unavailable)";
    }
}
