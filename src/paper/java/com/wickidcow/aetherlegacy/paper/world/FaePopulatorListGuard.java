package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Protects Paper's mutable Bukkit BlockPopulator list from concurrent modification
 * while tools such as Chunky request many chunks at once.
 *
 * <p>Paper 26.2 stores CraftWorld populators in an ArrayList and iterates that list
 * directly from async world-generation workers. If another world-management or
 * generation plugin mutates the list while a FEATURES task is iterating it, the
 * server can terminate with ConcurrentModificationException. The Fae Realm arms
 * this guard before its WorldInitEvent and swaps only Fae worlds to a
 * CopyOnWriteArrayList after all default populators have been attached.</p>
 */
final class FaePopulatorListGuard {

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final String POPULATORS_FIELD = "populators";

    private FaePopulatorListGuard() {
    }

    static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(FaePopulatorListGuard.class);
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onWorldInit(WorldInitEvent event) {
                World world = event.getWorld();
                if (!(world.getGenerator() instanceof AetherChunkGenerator)) {
                    return;
                }
                harden(plugin, world);
            }
        }, plugin);
    }

    @SuppressWarnings("unchecked")
    private static void harden(JavaPlugin plugin, World world) {
        try {
            Field field = findPopulatorsField(world.getClass());
            if (field == null) {
                plugin.getLogger().warning(
                    "Could not locate Paper's world populator list for " + world.getName()
                        + "; Chunky concurrency guard was not installed.");
                return;
            }

            field.setAccessible(true);
            Object current = field.get(world);
            if (current instanceof CopyOnWriteArrayList<?>) {
                return;
            }
            if (!(current instanceof List<?> currentList)) {
                plugin.getLogger().warning(
                    "Unexpected world populator storage type for " + world.getName() + ": "
                        + (current == null ? "null" : current.getClass().getName()));
                return;
            }

            CopyOnWriteArrayList<BlockPopulator> safe =
                new CopyOnWriteArrayList<>((List<BlockPopulator>) currentList);
            field.set(world, safe);
            plugin.getLogger().info(
                "Installed concurrent BlockPopulator guard for " + world.getName()
                    + " (" + safe.size() + " populator(s)); Chunky pregeneration is protected.");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning(
                "Could not install Chunky concurrency guard for " + world.getName() + ": "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static Field findPopulatorsField(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(POPULATORS_FIELD);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
