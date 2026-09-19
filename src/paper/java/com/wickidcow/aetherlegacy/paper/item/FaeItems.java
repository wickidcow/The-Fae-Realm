package com.wickidcow.aetherlegacy.paper.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

/** Vanilla-backed custom items used by Fae Realm progression. */
public final class FaeItems {

    private final NamespacedKey itemTypeKey;

    public FaeItems(JavaPlugin plugin) {
        // Keep the public item identity stable even if plugin.yml naming changes later.
        this.itemTypeKey = Objects.requireNonNull(
            NamespacedKey.fromString("thefaerealm:fae_item_type"));
    }

    public ItemStack essence(int amount) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Fae Essence", NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(
            Component.text("Condensed magic from the Fae Realm.", NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, "fae_essence");
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack vaultKey() {
        ItemStack item = new ItemStack(Material.TRIAL_KEY);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Fae Vault Key", NamedTextColor.AQUA));
        meta.lore(List.of(
            Component.text("An old key humming with veil-magic.", NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, "fae_vault_key");
        item.setItemMeta(meta);
        return item;
    }

    public boolean is(ItemStack item, String type) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        String stored = item.getItemMeta().getPersistentDataContainer()
            .get(itemTypeKey, PersistentDataType.STRING);
        return type.equals(stored);
    }
}
