package com.wickidcow.aetherlegacy.paper.progression;

import com.wickidcow.aetherlegacy.paper.AetherLegacyPlugin;
import com.wickidcow.aetherlegacy.paper.item.FaeItems;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Lightweight realm progression hooks that remain entirely server-side. */
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
    private final NamespacedKey placedSourceKey;

    public FaeProgressionListener(AetherLegacyPlugin plugin, FaeItems items) {
        this.plugin = plugin;
        this.items = items;
        this.placedSourceKey = new NamespacedKey(plugin, "player_placed_essence_sources");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRealmResourcePlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!block.getWorld().equals(plugin.getAetherWorld())
            || !ESSENCE_SOURCES.contains(block.getType())) {
            return;
        }
        markPlayerPlaced(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRealmResourceBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!plugin.getConfig().getBoolean("progression.enabled", true)
            || !block.getWorld().equals(plugin.getAetherWorld())
            || !ESSENCE_SOURCES.contains(block.getType())) {
            return;
        }

        if (consumePlayerPlacedMarker(block)) {
            return;
        }

        double chance = plugin.getConfig().getDouble("progression.essence-drop-chance", 0.22);
        chance = Math.max(0.0, Math.min(1.0, chance));
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }

        int amount = ThreadLocalRandom.current().nextDouble() < 0.15 ? 2 : 1;
        block.getWorld().dropItemNaturally(block.getLocation(), items.essence(amount));
    }

    private void markPlayerPlaced(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        int packed = pack(block);
        int[] existing = pdc.getOrDefault(
            placedSourceKey, PersistentDataType.INTEGER_ARRAY, new int[0]);

        for (int value : existing) {
            if (value == packed) {
                return;
            }
        }

        int[] updated = Arrays.copyOf(existing, existing.length + 1);
        updated[existing.length] = packed;
        pdc.set(placedSourceKey, PersistentDataType.INTEGER_ARRAY, updated);
    }

    private boolean consumePlayerPlacedMarker(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        int[] existing = pdc.getOrDefault(
            placedSourceKey, PersistentDataType.INTEGER_ARRAY, new int[0]);
        if (existing.length == 0) {
            return false;
        }

        int packed = pack(block);
        int found = -1;
        for (int i = 0; i < existing.length; i++) {
            if (existing[i] == packed) {
                found = i;
                break;
            }
        }
        if (found < 0) {
            return false;
        }

        if (existing.length == 1) {
            pdc.remove(placedSourceKey);
            return true;
        }

        int[] updated = new int[existing.length - 1];
        System.arraycopy(existing, 0, updated, 0, found);
        System.arraycopy(existing, found + 1, updated, found, existing.length - found - 1);
        pdc.set(placedSourceKey, PersistentDataType.INTEGER_ARRAY, updated);
        return true;
    }

    /** Packs chunk-local X/Z and absolute Y into one integer. */
    private int pack(Block block) {
        int localX = block.getX() & 15;
        int localZ = block.getZ() & 15;
        int shiftedY = block.getY() + 2048;
        return (shiftedY << 8) | (localX << 4) | localZ;
    }
}
