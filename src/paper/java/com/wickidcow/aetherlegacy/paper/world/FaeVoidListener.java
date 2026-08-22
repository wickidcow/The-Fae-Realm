package com.wickidcow.aetherlegacy.paper.world;

import com.wickidcow.aetherlegacy.paper.AetherLegacyPlugin;
import com.wickidcow.aetherlegacy.paper.portal.AetherPortalListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Configurable handling for players who fall beneath Fae Realm islands. */
public final class FaeVoidListener implements Listener {

    private final AetherLegacyPlugin plugin;
    private final AetherPortalListener portalListener;

    /**
     * Compatibility constructor used by the plugin entry point. The lightweight
     * portal helper reads the same persistent return-location key as the registered
     * portal listener, so void rescue survives restarts and disconnects.
     */
    public FaeVoidListener(AetherLegacyPlugin plugin) {
        this(plugin, new AetherPortalListener(plugin));
    }

    public FaeVoidListener(AetherLegacyPlugin plugin, AetherPortalListener portalListener) {
        this.plugin = plugin;
        this.portalListener = portalListener;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVoidDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID
            || !(event.getEntity() instanceof Player player)
            || !player.getWorld().equals(plugin.getAetherWorld())) {
            return;
        }

        String behavior = plugin.getConfig().getString("world.void-behavior", "return-to-overworld");
        if (behavior == null || behavior.equalsIgnoreCase("death")) {
            return;
        }

        event.setCancelled(true);
        if (behavior.equalsIgnoreCase("fae-spawn")) {
            player.teleport(plugin.getAetherArrivalLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.sendMessage(Component.text(
                "The Fae winds carry you back to safety.", NamedTextColor.LIGHT_PURPLE));
            return;
        }

        player.teleport(portalListener.getReturnLocation(player), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.sendMessage(Component.text(
            "You fall through the veil and return to the mortal world.", NamedTextColor.LIGHT_PURPLE));
    }
}
