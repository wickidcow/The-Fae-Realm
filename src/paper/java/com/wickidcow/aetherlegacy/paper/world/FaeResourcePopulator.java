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

        int attempts = scaledAttempts(11 + random.nextInt(7), density, random);
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = baseX + 2 + random.nextInt(12);
            int z = baseZ + 2 + random.nextInt(12);
            int surfaceY = FaeSurfaceLocator.find(info, region, x, z);
            if (surfaceY == Integer.MIN_VALUE || surfaceY <= info.getMinHeight() + 6) {
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
            if (density >= 1.5 && random.nextDouble() < Math.min(0.72, 0.22 * density)) {
                placeVein(region,
                    x + random.nextInt(-2, 3),
                    y + random.nextInt(-2, 3),
                    z + random.nextInt(-2, 3),
                    resource.material(),
                    Math.max(2, resource.maxVein() - 1),
                    random);
            }
        }

        int bloomAttempts = scaledAttempts(2 + random.nextInt(3), Math.max(0.0, density - 0.45), random);
        for (int attempt = 0; attempt < bloomAttempts; attempt++) {
            int x = baseX + 2 + random.nextInt(12);
            int z = baseZ + 2 + random.nextInt(12);
            int surfaceY = FaeSurfaceLocator.find(info, region, x, z);
            if (surfaceY == Integer.MIN_VALUE || surfaceY + 4 >= info.getMaxHeight()) {
                continue;
            }
            placeResourceBloom(info, region, random, x, surfaceY + 1, z);
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
            case GOLDEN_MEADOWS -> roll < 46
                ? new Resource(Material.GOLD_ORE, 7, 3, 9)
                : roll < 80
                    ? new Resource(Material.COPPER_ORE, 9, 4, 11)
                    : new Resource(Material.EMERALD_ORE, 4, 7, 9);
            case CRYSTAL_WOODS -> roll < 44
                ? new Resource(Material.AMETHYST_BLOCK, 8, 3, 9)
                : roll < 78
                    ? new Resource(Material.LAPIS_ORE, 7, 5, 10)
                    : new Resource(Material.DIAMOND_ORE, 4, 8, 8);
            case MIST_GARDENS -> roll < 44
                ? new Resource(Material.LAPIS_ORE, 8, 4, 11)
                : roll < 80
                    ? new Resource(Material.IRON_ORE, 8, 4, 10)
                    : new Resource(Material.GLOWSTONE, 5, 6, 8);
            case ANCIENT_FAE_FOREST -> roll < 44
                ? new Resource(Material.EMERALD_ORE, 5, 4, 9)
                : roll < 78
                    ? new Resource(Material.COAL_ORE, 9, 3, 10)
                    : new Resource(Material.DIAMOND_ORE, 4, 9, 7);
            case SKY_HIGHLANDS -> roll < 44
                ? new Resource(Material.IRON_ORE, 9, 3, 11)
                : roll < 80
                    ? new Resource(Material.COPPER_ORE, 9, 4, 10)
                    : new Resource(Material.GOLD_ORE, 5, 7, 8);
        };
    }

    private void placeResourceBloom(WorldInfo info,
                                    LimitedRegion region,
                                    SplittableRandom random,
                                    int x,
                                    int y,
                                    int z) {
        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
        Material base;
        Material accent;
        Material tip;
        switch (biome) {
            case GOLDEN_MEADOWS -> {
                base = Material.RAW_GOLD_BLOCK;
                accent = Material.GOLD_ORE;
                tip = Material.GLOWSTONE;
            }
            case CRYSTAL_WOODS -> {
                base = Material.AMETHYST_BLOCK;
                accent = Material.BUDDING_AMETHYST;
                tip = Material.AMETHYST_CLUSTER;
            }
            case MIST_GARDENS -> {
                base = Material.LAPIS_BLOCK;
                accent = Material.LAPIS_ORE;
                tip = Material.SEA_LANTERN;
            }
            case ANCIENT_FAE_FOREST -> {
                base = Material.MOSSY_COBBLESTONE;
                accent = Material.EMERALD_ORE;
                tip = Material.GLOW_BERRIES;
            }
            case SKY_HIGHLANDS -> {
                base = Material.RAW_IRON_BLOCK;
                accent = Material.IRON_ORE;
                tip = Material.GLOWSTONE;
            }
            default -> throw new IllegalStateException("Unexpected biome: " + biome);
        }

        setIfAir(region, x, y, z, base);
        if (random.nextDouble() < 0.78) setIfAir(region, x + 1, y, z, accent);
        if (random.nextDouble() < 0.78) setIfAir(region, x - 1, y, z, accent);
        if (random.nextDouble() < 0.58) setIfAir(region, x, y, z + 1, base);
        if (random.nextDouble() < 0.58) setIfAir(region, x, y, z - 1, base);
        setIfAir(region, x, y + 1, z, tip);
    }

    private void placeVein(LimitedRegion region,
                           int x,
                           int y,
                           int z,
                           Material material,
                           int maxVein,
                           SplittableRandom random) {
        int blocks = 2 + random.nextInt(Math.max(1, maxVein));
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

    private boolean setIfAir(LimitedRegion region, int x, int y, int z, Material material) {
        if (!region.isInRegion(x, y, z) || !region.getType(x, y, z).isAir()) {
            return false;
        }
        region.setType(x, y, z, material);
        return true;
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
