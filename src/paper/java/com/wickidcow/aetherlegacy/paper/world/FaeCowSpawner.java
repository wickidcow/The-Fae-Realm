package com.wickidcow.aetherlegacy.paper.world;

import com.wickidcow.aetherlegacy.paper.AetherLegacyPlugin;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Maintains deliberately dense cow herds around active players in the Fae Realm.
 *
 * <p>The system is world-local, capped, and self-cleaning. Only cows tagged by
 * this class are eligible for distance cleanup, so player-owned, naturally
 * spawned, named, or leashed cows are not removed.</p>
 */
public final class FaeCowSpawner {

    private static final String GENERATED_COW_TAG = "faerealm_bonus_cow";
    private static final Set<Material> VALID_GROUND = Set.of(
        Material.GRASS_BLOCK,
        Material.MOSS_BLOCK,
        Material.PODZOL,
        Material.MYCELIUM,
        Material.DIRT,
        Material.COARSE_DIRT,
        Material.ROOTED_DIRT
    );

    private final AetherLegacyPlugin plugin;
    private final World world;

    private BukkitTask task;
    private boolean enabled;
    private int targetNearPlayer;
    private int nearbyRadius;
    private int worldCap;
    private long checkIntervalTicks;
    private int maxSpawnsPerCheck;
    private int minSpawnRadius;
    private int maxSpawnRadius;
    private int cleanupDistance;

    public FaeCowSpawner(AetherLegacyPlugin plugin, World world) {
        this.plugin = plugin;
        this.world = world;
    }

    public void start() {
        reload();
    }

    public void reload() {
        stop();

        enabled = plugin.getConfig().getBoolean("fauna.cows.enabled", true);
        targetNearPlayer = clamp(plugin.getConfig().getInt("fauna.cows.target-near-player", 48), 0, 128);
        nearbyRadius = clamp(plugin.getConfig().getInt("fauna.cows.nearby-radius", 56), 16, 128);
        worldCap = clamp(plugin.getConfig().getInt("fauna.cows.world-cap", 240), 0, 600);
        checkIntervalTicks = clamp(plugin.getConfig().getLong("fauna.cows.check-interval-ticks", 80L), 20L, 1200L);
        maxSpawnsPerCheck = clamp(plugin.getConfig().getInt("fauna.cows.max-spawns-per-check", 12), 1, 64);
        minSpawnRadius = clamp(plugin.getConfig().getInt("fauna.cows.min-spawn-radius", 12), 4, 96);
        maxSpawnRadius = clamp(plugin.getConfig().getInt("fauna.cows.max-spawn-radius", 48), minSpawnRadius, 128);
        cleanupDistance = clamp(plugin.getConfig().getInt("fauna.cows.cleanup-distance", 112), nearbyRadius, 256);

        if (!enabled || targetNearPlayer <= 0 || worldCap <= 0) {
            plugin.getLogger().info("Fae cow herds are disabled.");
            return;
        }

        task = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            this::tick,
            checkIntervalTicks,
            checkIntervalTicks
        );

        plugin.getLogger().info(
            "Fae cow herds enabled: target " + targetNearPlayer
                + " per player, realm cap " + worldCap
                + ", refill up to " + maxSpawnsPerCheck
                + " every " + checkIntervalTicks + " ticks."
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (!enabled || !plugin.getConfig().getBoolean("world.mob-spawning", true)) {
            return;
        }

        List<Player> players = world.getPlayers().stream()
            .filter(player -> player.isValid() && !player.isDead())
            .toList();

        cleanupGeneratedCows(players);

        if (players.isEmpty()) {
            return;
        }

        int totalCows = world.getEntitiesByClass(Cow.class).size();
        if (totalCows >= worldCap) {
            return;
        }

        int remainingGlobalBudget = worldCap - totalCows;

        for (Player player : players) {
            if (remainingGlobalBudget <= 0) {
                break;
            }

            long nearbyCows = world.getNearbyEntities(
                    player.getLocation(),
                    nearbyRadius,
                    nearbyRadius,
                    nearbyRadius
                ).stream()
                .filter(entity -> entity.getType() == EntityType.COW)
                .count();

            int deficit = targetNearPlayer - (int) nearbyCows;
            if (deficit <= 0) {
                continue;
            }

            int budget = Math.min(
                Math.min(deficit, maxSpawnsPerCheck),
                remainingGlobalBudget
            );

            int spawned = spawnHerdsNear(player, budget);
            remainingGlobalBudget -= spawned;
        }
    }

    private int spawnHerdsNear(Player player, int budget) {
        if (budget <= 0) {
            return 0;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int spawned = 0;
        int attempts = Math.max(12, budget * 4);

        while (spawned < budget && attempts-- > 0) {
            double angle = random.nextDouble(Math.PI * 2.0);
            double radius = random.nextDouble(minSpawnRadius, maxSpawnRadius + 1.0);

            int centerX = player.getLocation().getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int centerZ = player.getLocation().getBlockZ() + (int) Math.round(Math.sin(angle) * radius);

            int herdSize = Math.min(budget - spawned, random.nextInt(3, 7));
            for (int i = 0; i < herdSize && spawned < budget; i++) {
                int x = centerX + random.nextInt(-4, 5);
                int z = centerZ + random.nextInt(-4, 5);

                Location spawn = findSpawnLocation(x, z);
                if (spawn == null) {
                    continue;
                }

                Entity entity = world.spawnEntity(
                    spawn,
                    EntityType.COW,
                    CreatureSpawnEvent.SpawnReason.CUSTOM
                );

                if (entity instanceof Cow cow && cow.isValid()) {
                    cow.addScoreboardTag(GENERATED_COW_TAG);
                    spawned++;
                }
            }
        }

        return spawned;
    }

    private Location findSpawnLocation(int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return null;
        }

        Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (!VALID_GROUND.contains(ground.getType())) {
            return null;
        }

        int y = ground.getY() + 1;
        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) {
            return null;
        }

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        if (!feet.isPassable() || !head.isPassable()) {
            return null;
        }

        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private void cleanupGeneratedCows(List<Player> players) {
        double maxDistanceSquared = (double) cleanupDistance * cleanupDistance;

        for (Cow cow : world.getEntitiesByClass(Cow.class)) {
            if (!cow.getScoreboardTags().contains(GENERATED_COW_TAG)) {
                continue;
            }
            if (cow.customName() != null || cow.isLeashed()) {
                continue;
            }

            boolean nearPlayer = false;
            for (Player player : players) {
                if (cow.getLocation().distanceSquared(player.getLocation()) <= maxDistanceSquared) {
                    nearPlayer = true;
                    break;
                }
            }

            if (!nearPlayer) {
                cow.remove();
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
