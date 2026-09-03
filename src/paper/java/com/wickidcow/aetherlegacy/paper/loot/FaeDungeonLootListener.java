package com.wickidcow.aetherlegacy.paper.loot;

import com.wickidcow.aetherlegacy.paper.AetherLegacyPlugin;
import com.wickidcow.aetherlegacy.paper.item.FaeItems;
import com.wickidcow.aetherlegacy.paper.world.AetherChunkGenerator;
import com.wickidcow.aetherlegacy.paper.world.FaeRealmBiome;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Barrel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.SplittableRandom;

/** Lazy deterministic loot for explicitly tagged Fae Vault barrels. */
public final class FaeDungeonLootListener implements Listener {

    private final AetherLegacyPlugin plugin;
    private final FaeItems faeItems;
    private final NamespacedKey playerPlacedKey;
    private final NamespacedKey generatedVaultBarrelKey;
    private final NamespacedKey generatedLootKey;

    public FaeDungeonLootListener(AetherLegacyPlugin plugin) {
        this.plugin = plugin;
        this.faeItems = new FaeItems(plugin);
        this.playerPlacedKey = new NamespacedKey(plugin, "player_placed_barrel");
        this.generatedVaultBarrelKey = Objects.requireNonNull(
            NamespacedKey.fromString("thefaerealm:generated_vault_barrel"));
        this.generatedLootKey = new NamespacedKey(plugin, "generated_dungeon_loot");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBarrelPlaced(BlockPlaceEvent event) {
        if (!event.getBlockPlaced().getWorld().equals(plugin.getAetherWorld())
            || event.getBlockPlaced().getType() != Material.BARREL
            || !(event.getBlockPlaced().getState() instanceof Barrel barrel)) {
            return;
        }

        barrel.getPersistentDataContainer().set(playerPlacedKey, PersistentDataType.BYTE, (byte) 1);
        barrel.getPersistentDataContainer().remove(generatedVaultBarrelKey);
        barrel.update(true, false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Barrel barrel)
            || !barrel.getWorld().equals(plugin.getAetherWorld())) {
            return;
        }

        if (barrel.getPersistentDataContainer().has(playerPlacedKey, PersistentDataType.BYTE)
            || !barrel.getPersistentDataContainer().has(generatedVaultBarrelKey, PersistentDataType.BYTE)
            || barrel.getPersistentDataContainer().has(generatedLootKey, PersistentDataType.BYTE)) {
            return;
        }

        Inventory inventory = barrel.getInventory();
        if (!inventory.isEmpty()) {
            return;
        }

        long seed = mixSeed(
            barrel.getWorld().getSeed(),
            barrel.getX(),
            barrel.getY(),
            barrel.getZ());
        SplittableRandom random = new SplittableRandom(seed);
        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(
            barrel.getWorld().getSeed(), barrel.getX(), barrel.getZ());

        populate(inventory, biome, random);
        barrel.getPersistentDataContainer().set(generatedLootKey, PersistentDataType.BYTE, (byte) 1);
        barrel.update(true, false);
    }

    private void populate(Inventory inventory, FaeRealmBiome biome, SplittableRandom random) {
        addRandomSlot(inventory, new ItemStack(Material.EXPERIENCE_BOTTLE, 2 + random.nextInt(6)), random);
        addRandomSlot(inventory, new ItemStack(Material.GLOW_BERRIES, 3 + random.nextInt(8)), random);
        addRandomSlot(inventory, new ItemStack(Material.ENDER_PEARL, 1 + random.nextInt(3)), random);
        addRandomSlot(inventory, faeItems.essence(1 + random.nextInt(3)), random);

        switch (biome) {
            case GOLDEN_MEADOWS -> {
                addRandomSlot(inventory, new ItemStack(Material.GOLD_INGOT, 2 + random.nextInt(5)), random);
                addRandomSlot(inventory, new ItemStack(Material.HONEY_BOTTLE, 1 + random.nextInt(3)), random);
            }
            case CRYSTAL_WOODS -> {
                addRandomSlot(inventory, new ItemStack(Material.AMETHYST_SHARD, 4 + random.nextInt(9)), random);
                addRandomSlot(inventory, new ItemStack(Material.LAPIS_LAZULI, 3 + random.nextInt(7)), random);
            }
            case MIST_GARDENS -> {
                addRandomSlot(inventory, new ItemStack(Material.PRISMARINE_CRYSTALS, 3 + random.nextInt(7)), random);
                addRandomSlot(inventory, new ItemStack(Material.GLOW_INK_SAC, 2 + random.nextInt(5)), random);
            }
            case ANCIENT_FAE_FOREST -> {
                addRandomSlot(inventory, new ItemStack(Material.EMERALD, 1 + random.nextInt(4)), random);
                addRandomSlot(inventory, new ItemStack(Material.ECHO_SHARD, 1 + random.nextInt(2)), random);
            }
            case SKY_HIGHLANDS -> {
                addRandomSlot(inventory, new ItemStack(Material.WIND_CHARGE, 2 + random.nextInt(6)), random);
                addRandomSlot(inventory, new ItemStack(Material.IRON_INGOT, 2 + random.nextInt(5)), random);
            }
        }

        if (random.nextInt(7) == 0) {
            addRandomSlot(inventory, faeItems.vaultKey(), random);
        }
        if (random.nextInt(6) == 0) {
            addRandomSlot(inventory,
                namedRelic(Material.ECHO_SHARD, "Fae Echo", NamedTextColor.LIGHT_PURPLE), random);
        }
        if (random.nextInt(12) == 0) {
            addRandomSlot(inventory, new ItemStack(Material.DIAMOND, 1 + random.nextInt(2)), random);
        }
        if (random.nextInt(48) == 0) {
            addRandomSlot(inventory,
                namedRelic(Material.ENCHANTED_GOLDEN_APPLE, "Gift of the Veil", NamedTextColor.GOLD), random);
        }
    }

    private ItemStack namedRelic(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color));
        item.setItemMeta(meta);
        return item;
    }

    private void addRandomSlot(Inventory inventory, ItemStack item, SplittableRandom random) {
        if (inventory.firstEmpty() < 0) {
            return;
        }

        int slot;
        int attempts = 0;
        do {
            slot = random.nextInt(inventory.getSize());
            attempts++;
        } while (inventory.getItem(slot) != null && attempts < inventory.getSize() * 2);

        if (inventory.getItem(slot) == null) {
            inventory.setItem(slot, item);
        } else {
            inventory.addItem(item);
        }
    }

    private static long mixSeed(long seed, int x, int y, int z) {
        long mixed = seed;
        mixed ^= (long) x * 341873128712L;
        mixed ^= (long) y * 42317861L;
        mixed ^= (long) z * 132897987541L;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return mixed;
    }
}
