package com.wickidcow.aetherlegacy.paper.portal;

import com.wickidcow.aetherlegacy.paper.AetherLegacyPlugin;
import com.wickidcow.aetherlegacy.paper.item.FaeItems;
import com.wickidcow.aetherlegacy.paper.world.FaePlane;
import com.wickidcow.aetherlegacy.paper.world.FaeRiftPopulator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class FaeRiftListener implements Listener {

    private final AetherLegacyPlugin plugin;
    private final FaeItems items;

    public FaeRiftListener(AetherLegacyPlugin plugin) {
        this.plugin = plugin;
        this.items = new FaeItems(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRiftUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
            || event.getAction() != Action.RIGHT_CLICK_BLOCK
            || event.getClickedBlock() == null
            || event.getClickedBlock().getType() != Material.LODESTONE) {
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.isFaeWorld(player.getWorld())) {
            return;
        }

        FaePlane encoded = detectPlane(event.getClickedBlock());
        if (encoded == null) {
            return;
        }

        FaePlane current = plugin.getFaePlane(player.getWorld());
        FaePlane destination;
        if (current == FaePlane.REALM) {
            if (!useEssence(player)) {
                player.sendMessage(Component.text(
                    "The dormant rift needs one Fae Essence.", NamedTextColor.LIGHT_PURPLE));
                return;
            }
            destination = encoded;
        } else {
            if (current != encoded) {
                return;
            }
            destination = FaePlane.REALM;
        }

        World target = plugin.getFaeWorld(destination);
        if (target == null) {
            player.sendMessage(Component.text(destination.displayName() + " is unavailable.", NamedTextColor.YELLOW));
            return;
        }

        event.setCancelled(true);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.9f, 0.75f);
        player.teleport(plugin.getFaeArrivalLocation(destination), PlayerTeleportEvent.TeleportCause.PLUGIN);
        target.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.15f);
        player.sendMessage(Component.text(
            destination == FaePlane.REALM
                ? "The rift folds back into the central Fae Realm."
                : "The veil opens into " + destination.displayName() + ".",
            NamedTextColor.LIGHT_PURPLE));
    }

    private boolean useEssence(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!items.is(held, "fae_essence")) {
            return false;
        }
        if (held.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            held.setAmount(held.getAmount() - 1);
        }
        return true;
    }

    private FaePlane detectPlane(Block lodestone) {
        for (FaePlane plane : new FaePlane[]{FaePlane.WILDBLOOM, FaePlane.GLOAM, FaePlane.STARFALL}) {
            Material marker = FaeRiftPopulator.marker(plane);
            int matches = 0;
            if (lodestone.getRelative(1, 0, 0).getType() == marker) matches++;
            if (lodestone.getRelative(-1, 0, 0).getType() == marker) matches++;
            if (lodestone.getRelative(0, 0, 1).getType() == marker) matches++;
            if (lodestone.getRelative(0, 0, -1).getType() == marker) matches++;
            if (matches >= 3) {
                return plane;
            }
        }
        return null;
    }
}
