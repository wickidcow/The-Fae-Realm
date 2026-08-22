package com.wickidcow.aetherlegacy.paper.progression;

import com.wickidcow.aetherlegacy.paper.AetherLegacyPlugin;
import com.wickidcow.aetherlegacy.paper.item.FaeItems;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lightweight realm progression hooks that remain entirely server-side.
 */
public final class FaeProgressionListener implements Listener {

    private static final Set<Material> ESSENCE_SOURCES = Set.of(
        Material.AMETHYST_BLOCK,
        Material.GOLD_ORE,
        Material.COPPER_ORE,
        Material.IRON_ORE,
        Material.LAPIS_ORE,
        Material.EMERALD_ORE,
        Material.DIAMOND_ORE,
        Material.GLOWSTONE
    );

    private final AetherLegacyPlugin plugin;
    private final FaeItems items;

    public FaeProgressionListener(AetherLegacyPlugin plugin, FaeItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRealmResourceBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("progression.enabled", true)
            || !event.getBlock().getWorld().equals(plugin.getAetherWorld())
            || !ESSENCE_SOURCES.contains(event.getBlock().getType())) {
            return;
        }

        double chance = plugin.getConfig().getDouble("progression.essence-drop-chance", 0.22);
        chance = Math.max(0.0, Math.min(1.0, chance));
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }

        int amount = ThreadLocalRandom.current().nextDouble() < 0.15 ? 2 : 1;
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), items.essence(amount));
    }
}
