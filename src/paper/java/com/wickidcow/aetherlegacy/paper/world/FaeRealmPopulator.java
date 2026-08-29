package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe decoration pass for the Fae worlds.
 * Uses LimitedRegion only, so it is safe for Paper's asynchronous chunk generation.
 */
public final class FaeRealmPopulator extends BlockPopulator {

    private static final FaeStructurePopulator STRUCTURES = new FaeStructurePopulator();
    private static final FaeResourcePopulator RESOURCES = new FaeResourcePopulator();
    private static final FaeFeaturePopulator FEATURES = new FaeFeaturePopulator();
    private static final FaeFloraPopulator FLORA = new FaeFloraPopulator();
    private static final FaePlaneFeaturePopulator PLANE_FEATURES = new FaePlaneFeaturePopulator();
    private static final FaeRiftPopulator RIFTS = new FaeRiftPopulator();
    private static final FaeUndersideGenerator UNDERSIDE = new FaeUndersideGenerator();
    private static final AtomicBoolean POPULATOR_SAFETY_HOOK_REGISTERED = new AtomicBoolean();

    private final FaeGeneratorSettings settings;

    public FaeRealmPopulator() {
        this(FaeGeneratorSettings.defaults());
    }

    public FaeRealmPopulator(@NotNull FaeGeneratorSettings settings) {
        this.settings = settings;
        ensurePopulatorSafetyHook();
    }

    @Override
    public void populate(@NotNull WorldInfo worldInfo,
                         @NotNull Random random,
                         int chunkX,
                         int chunkZ,
                         @NotNull LimitedRegion region) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        if (settings.resources() && settings.resourceDensity() > 0.0) {
            RESOURCES.populate(worldInfo, chunkX, chunkZ, region, settings.resourceDensity());
        }

        if (settings.decorations() && settings.decorationDensity() > 0.0) {
            UNDERSIDE.populate(worldInfo, chunkX, chunkZ, region, settings);

            FLORA.populate(
                worldInfo,
                chunkX,
                chunkZ,
                region,
                settings.decorationDensity());

            if (random.nextDouble() < scaledChance(1.0 / 9.0, settings.decorationDensity())) {
                placeCrystalOutcrop(worldInfo, region, random, baseX, baseZ);
            }

            if (random.nextDouble() < scaledChance(1.0 / 96.0, settings.decorationDensity())) {
                placeFaeRuin(worldInfo, region, random, baseX, baseZ);
            }

            FEATURES.populate(
                worldInfo,
                random,
                chunkX,
                chunkZ,
                region,
                settings.decorationDensity());

            PLANE_FEATURES.populate(
                worldInfo,
                chunkX,
                chunkZ,
                region,
                settings.decorationDensity());

            RIFTS.populate(
                worldInfo,
                chunkX,
                chunkZ,
                region,
                settings.decorationDensity());
        }

        if (settings.structures()) {
            STRUCTURES.populate(worldInfo, random, chunkX, chunkZ, region, settings);
        }
    }

    /**
     * Paper 26.2 executes BlockPopulators from async chunk-generation workers while
     * CraftWorld exposes the registry as a mutable ArrayList. If any plugin mutates
     * that list while Paper is iterating it, ArrayList throws ConcurrentModificationException
     * and Moonrise escalates the failed FEATURES task into a full server shutdown.
     *
     * <p>The hook is registered while Bukkit is collecting this generator's default
     * populators. WorldInitEvent fires after those defaults are installed but before
     * normal spawn-chunk generation, which gives us a safe main-thread point to replace
     * only Fae worlds' registry with a CopyOnWriteArrayList. Other plugins can keep using
     * World#getPopulators normally without invalidating active worldgen iterators.</p>
     */
    private static void ensurePopulatorSafetyHook() {
        if (!POPULATOR_SAFETY_HOOK_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        JavaPlugin plugin;
        try {
            plugin = JavaPlugin.getProvidingPlugin(FaeRealmPopulator.class);
        } catch (RuntimeException exception) {
            POPULATOR_SAFETY_HOOK_REGISTERED.set(false);
            return;
        }

        try {
            plugin.getServer().getPluginManager().registerEvents(new Listener() {
                @EventHandler(priority = EventPriority.LOWEST)
                public void onWorldInit(WorldInitEvent event) {
                    World world = event.getWorld();
                    if (world.getGenerator() instanceof AetherChunkGenerator) {
                        stabilizePopulatorRegistry(plugin, world);
                    }
                }
            }, plugin);
        } catch (RuntimeException exception) {
            POPULATOR_SAFETY_HOOK_REGISTERED.set(false);
            plugin.getLogger().warning(
                "Could not register Fae async-populator safety hook: " + exception.getMessage());
        }
    }

    private static void stabilizePopulatorRegistry(JavaPlugin plugin, World world) {
        List<BlockPopulator> current = world.getPopulators();
        if (current instanceof CopyOnWriteArrayList<?>) {
            plugin.getLogger().info(
                "Async-safe Fae populator registry active for " + world.getName()
                    + " (" + current.size() + " populator(s)).");
            return;
        }

        try {
            Field field = findPopulatorField(world, current);
            if (field == null) {
                plugin.getLogger().warning(
                    "Could not locate Paper's populator registry field for Fae world "
                        + world.getName() + "; async registry guard is unavailable.");
                return;
            }

            CopyOnWriteArrayList<BlockPopulator> replacement = new CopyOnWriteArrayList<>(current);
            field.set(world, replacement);

            if (world.getPopulators() != replacement) {
                plugin.getLogger().warning(
                    "Paper rejected the async-safe populator registry replacement for Fae world "
                        + world.getName() + ".");
                return;
            }

            plugin.getLogger().info(
                "Async-safe Fae populator registry active for " + world.getName()
                    + " (" + replacement.size() + " populator(s)).");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning(
                "Could not stabilize Paper's populator registry for Fae world "
                    + world.getName() + ": " + exception.getClass().getSimpleName()
                    + ": " + exception.getMessage());
        }
    }

    private static Field findPopulatorField(World world, List<BlockPopulator> current)
        throws IllegalAccessException {
        for (Class<?> type = world.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                    || !List.class.isAssignableFrom(field.getType())
                    || !field.trySetAccessible()) {
                    continue;
                }
                if (field.get(world) == current) {
                    return field;
                }
            }
        }
        return null;
    }

    private double scaledChance(double baseChance, double density) {
        return Math.min(1.0, Math.max(0.0, baseChance * density));
    }

    private void placeCrystalOutcrop(WorldInfo info,
                                     LimitedRegion region,
                                     Random random,
                                     int baseX,
                                     int baseZ) {
        int x = baseX + 3 + random.nextInt(10);
        int z = baseZ + 3 + random.nextInt(10);
        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
        int y = findFaeSurface(info, region, x, z, biome);
        if (y == Integer.MIN_VALUE || y + 4 >= info.getMaxHeight()) {
            return;
        }
        if (biome != FaeRealmBiome.CRYSTAL_WOODS) {
            return;
        }

        setIfInside(region, x, y + 1, z, Material.CALCITE);
        setIfInside(region, x, y + 2, z, Material.AMETHYST_BLOCK);
        setIfInside(region, x, y + 3, z, Material.AMETHYST_CLUSTER);
        if (random.nextBoolean()) {
            setIfInside(region, x + 1, y + 1, z, Material.BUDDING_AMETHYST);
        }
        if (random.nextBoolean()) {
            setIfInside(region, x - 1, y + 1, z, Material.CALCITE);
        }
    }

    private void placeFaeRuin(WorldInfo info,
                              LimitedRegion region,
                              Random random,
                              int baseX,
                              int baseZ) {
        int x = baseX + 5 + random.nextInt(6);
        int z = baseZ + 5 + random.nextInt(6);
        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
        int y = findFaeSurface(info, region, x, z, biome);
        if (y == Integer.MIN_VALUE || y + 6 >= info.getMaxHeight()) {
            return;
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    if (random.nextDouble() < 0.74) {
                        setIfInside(region, x + dx, y + 1, z + dz, Material.MOSSY_STONE_BRICKS);
                    }
                }
            }
        }

        int pillarHeight = 3 + random.nextInt(3);
        for (int dy = 1; dy <= pillarHeight; dy++) {
            setIfInside(region, x, y + dy, z,
                dy == pillarHeight ? Material.CHISELED_STONE_BRICKS : Material.STONE_BRICKS);
        }
        setIfInside(region, x, y + pillarHeight + 1, z, Material.SOUL_LANTERN);
    }

    private int findFaeSurface(WorldInfo info,
                               LimitedRegion region,
                               int x,
                               int z,
                               FaeRealmBiome biome) {
        for (int y = info.getMaxHeight() - 2; y >= info.getMinHeight(); y--) {
            if (!region.isInRegion(x, y, z)) {
                continue;
            }
            if (region.getType(x, y, z) == biome.surface()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private void setIfInside(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) {
            region.setType(x, y, z, material);
        }
    }
}
