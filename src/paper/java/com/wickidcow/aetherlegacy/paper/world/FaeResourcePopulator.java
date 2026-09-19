package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.SplittableRandom;

/**
 * Deterministic resource pass for Fae Realm islands.
 *
 * <p>Resources are intentionally biome-weighted so exploration matters. The pass
 * only replaces natural island core/subsurface materials and never force-loads
 * neighboring chunks.</p>
 */
public final class FaeResourcePopulator {

    private static final long RESOURCE_SALT = 0x13198A2E03707344L;

    public void populate(WorldInfo info,
                         int chunkX,
                         int chunkZ,
                         LimitedRegion region,
                         double density) {
        SplittableRandom random = new SplittableRandom(
            mixSeed(info.getSeed() ^ RESOURCE_SALT, chunkX, chunkZ));
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        int attempts = scaledAttempts(7 + random.nextInt(5), density, random);
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = baseX + 2 + random.nextInt(12);
            int z = baseZ + 2 + random.nextInt(12);
            int surfaceY = region.getHighestBlockYAt(x, z);
            if (surfaceY <= info.getMinHeight() + 6) {
                continue;
            }

            FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
            Resource resource = chooseResource(biome, random);
            int y = Math.max(info.getMinHeight() + 2,
                surfaceY - resource.minDepth() - random.nextInt(resource.depthRange()));

            Material target = region.getType(x, y, z);
            if (!replaceable(target)) {
                continue;
            }

            placeVein(region, x, y, z, resource.material(), resource.maxVein(), random);
        }
    }

    private int scaledAttempts(int baseAttempts, double density, SplittableRandom random) {
        double expected = Math.max(0.0, baseAttempts * density);
        int whole = (int) Math.floor(expected);
        double remainder = expected - whole;
        return whole + (random.nextDouble() < remainder ? 1 : 0);
    }

    private Resource chooseResource(FaeRealmBiome biome, SplittableRandom random) {
        int roll = random.nextInt(100);
        return switch (biome) {
            case GOLDEN_MEADOWS -> roll < 52
                ? new Resource(Material.GOLD_ORE, 4, 8, 11)
                : roll < 88
                    ? new Resource(Material.COPPER_ORE, 6, 5, 12)
                    : new Resource(Material.EMERALD_ORE, 2, 10, 9);
            case CRYSTAL_WOODS -> roll < 52
                ? new Resource(Material.AMETHYST_BLOCK, 5, 4, 10)
                : roll < 88
                    ? new Resource(Material.LAPIS_ORE, 4, 7, 10)
                    : new Resource(Material.DIAMOND_ORE, 2, 11, 8);
            case MIST_GARDENS -> roll < 52
                ? new Resource(Material.LAPIS_ORE, 5, 5, 12)
                : roll < 88
                    ? new Resource(Material.IRON_ORE, 5, 6, 11)
                    : new Resource(Material.GLOWSTONE, 3, 9, 8);
            case ANCIENT_FAE_FOREST -> roll < 52
                ? new Resource(Material.EMERALD_ORE, 3, 6, 10)
                : roll < 88
                    ? new Resource(Material.COAL_ORE, 6, 4, 11)
                    : new Resource(Material.DIAMOND_ORE, 2, 12, 7);
            case SKY_HIGHLANDS -> roll < 52
                ? new Resource(Material.IRON_ORE, 6, 4, 12)
                : roll < 88
                    ? new Resource(Material.COPPER_ORE, 6, 5, 11)
                    : new Resource(Material.GOLD_ORE, 3, 10, 8);
        };
    }

    private void placeVein(LimitedRegion region,
                           int x,
                           int y,
                           int z,
                           Material material,
                           int maxVein,
                           SplittableRandom random) {
        int blocks = 1 + random.nextInt(Math.max(1, maxVein));
        int px = x;
        int py = y;
        int pz = z;

        for (int i = 0; i < blocks; i++) {
            if (region.isInRegion(px, py, pz) && replaceable(region.getType(px, py, pz))) {
                region.setType(px, py, pz, material);
            }

            switch (random.nextInt(6)) {
                case 0 -> px++;
                case 1 -> px--;
                case 2 -> pz++;
                case 3 -> pz--;
                case 4 -> py++;
                default -> py--;
            }
        }
    }

    private boolean replaceable(Material material) {
        return material == Material.STONE
            || material == Material.CALCITE
            || material == Material.TUFF
            || material == Material.ANDESITE
            || material == Material.DIRT
            || material == Material.ROOTED_DIRT
            || material == Material.COARSE_DIRT;
    }

    private static long mixSeed(long seed, int x, int z) {
        long mixed = seed;
        mixed ^= (long) x * 341873128712L;
        mixed ^= (long) z * 132897987541L;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return mixed;
    }

    private record Resource(Material material, int maxVein, int minDepth, int depthRange) {
    }
}
