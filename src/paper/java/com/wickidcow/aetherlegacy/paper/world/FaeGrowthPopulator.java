package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.SplittableRandom;

/**
 * Extra ecology pass inspired by data-driven generators: broad moisture,
 * fertility and magic fields are domain-warped into coherent growth zones.
 * No Iris classes or assets are used; the logic is native to The Fae Realm.
 */
public final class FaeGrowthPopulator {
    private static final long GROWTH_SALT = 0x243F6A8885A308D3L;

    public void populate(@NotNull WorldInfo info, int chunkX, int chunkZ,
                         @NotNull LimitedRegion region, @NotNull FaeGeneratorSettings settings) {
        double density = settings.growthDensity();
        if (density <= 0.0) return;

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        Ecology ecology = sample(info.getSeed(), baseX + 8, baseZ + 8);
        double localDensity = density * ecology.growthMultiplier();
        SplittableRandom random = new SplittableRandom(mixSeed(info.getSeed() ^ GROWTH_SALT, chunkX, chunkZ));

        int clusterAttempts = scaledAttempts(5 + random.nextInt(6), localDensity, random);
        for (int i = 0; i < clusterAttempts; i++) {
            int x = baseX + 2 + random.nextInt(12);
            int z = baseZ + 2 + random.nextInt(12);
            int surfaceY = region.getHighestBlockYAt(x, z);
            if (isFaeSurface(info, region, x, surfaceY, z)) {
                placeGrowthCluster(info, region, random, ecology, x, surfaceY + 1, z);
            }
        }

        int edgeAttempts = scaledAttempts(2 + random.nextInt(4), localDensity, random);
        for (int i = 0; i < edgeAttempts; i++) {
            int x = baseX + 2 + random.nextInt(12);
            int z = baseZ + 2 + random.nextInt(12);
            placeHangingGrowth(info, region, random, ecology, x, z, settings.cloudLevel() + 8);
        }
    }

    private Ecology sample(long seed, int x, int z) {
        double warpX = FaeNoise.fractal(seed ^ 0x13198A2E03707344L, x * 0.0016, z * 0.0016, 3, 2.0, 0.52) * 72.0;
        double warpZ = FaeNoise.fractal(seed ^ 0xA4093822299F31D0L, x * 0.0016, z * 0.0016, 3, 2.0, 0.52) * 72.0;
        double wx = x + warpX;
        double wz = z + warpZ;
        double fertility = unit(FaeNoise.fractal(seed ^ 0x082EFA98EC4E6C89L, wx * 0.0031, wz * 0.0031, 4, 2.0, 0.52));
        double moisture = unit(FaeNoise.fractal(seed ^ 0x452821E638D01377L, wx * 0.0026, wz * 0.0026, 3, 2.1, 0.50));
        double magic = unit(FaeNoise.fractal(seed ^ 0xBE5466CF34E90C6CL, wx * 0.0042, wz * 0.0042, 3, 2.0, 0.55));
        double combined = fertility * 0.48 + moisture * 0.30 + magic * 0.22;
        GrowthBand band = combined < 0.28 ? GrowthBand.SPARSE : combined < 0.48 ? GrowthBand.MEADOW
            : combined < 0.69 ? GrowthBand.LUSH : combined < 0.84 ? GrowthBand.ANCIENT : GrowthBand.ENCHANTED;
        return new Ecology(moisture, magic, clamp(0.55, 2.05, 0.50 + combined * 1.55), band);
    }

    private void placeGrowthCluster(WorldInfo info, LimitedRegion region, SplittableRandom random,
                                    Ecology ecology, int x, int y, int z) {
        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
        int radius = switch (ecology.band()) {
            case SPARSE -> 1;
            case MEADOW, LUSH -> 2;
            case ANCIENT, ENCHANTED -> 3;
        };
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 1 || random.nextDouble() < 0.28) continue;
                int px = x + dx;
                int pz = z + dz;
                int py = region.getHighestBlockYAt(px, pz) + 1;
                if (region.isInRegion(px, py, pz) && region.getType(px, py, pz).isAir()) {
                    region.setType(px, py, pz, growthMaterial(biome, ecology, random));
                }
            }
        }
        if ((ecology.band() == GrowthBand.ANCIENT || ecology.band() == GrowthBand.ENCHANTED) && random.nextDouble() < 0.32) {
            placeRootKnot(region, random, x, y, z, biome);
        }
    }

    private Material growthMaterial(FaeRealmBiome biome, Ecology ecology, SplittableRandom random) {
        if (ecology.magic() > 0.78 && random.nextDouble() < 0.24) {
            return switch (biome) {
                case CRYSTAL_WOODS -> Material.PINK_PETALS;
                case MIST_GARDENS -> Material.PALE_MOSS_CARPET;
                case ANCIENT_FAE_FOREST -> Material.FLOWERING_AZALEA;
                case SKY_HIGHLANDS -> Material.ALLIUM;
                case GOLDEN_MEADOWS -> Material.TORCHFLOWER;
            };
        }
        return switch (biome) {
            case GOLDEN_MEADOWS -> pick(random, Material.SHORT_GRASS, Material.DANDELION, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.ALLIUM, Material.PINK_PETALS);
            case CRYSTAL_WOODS -> pick(random, Material.PINK_PETALS, Material.AZALEA, Material.FLOWERING_AZALEA, Material.SHORT_GRASS, Material.MOSS_CARPET);
            case MIST_GARDENS -> pick(random, Material.PALE_MOSS_CARPET, Material.FERN, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.SHORT_GRASS);
            case ANCIENT_FAE_FOREST -> pick(random, Material.FERN, Material.LARGE_FERN, Material.MOSS_CARPET, Material.AZALEA, Material.FLOWERING_AZALEA, Material.BROWN_MUSHROOM);
            case SKY_HIGHLANDS -> pick(random, Material.SHORT_GRASS, Material.FERN, Material.AZURE_BLUET, Material.CORNFLOWER, Material.OXEYE_DAISY);
        };
    }

    private void placeRootKnot(LimitedRegion region, SplittableRandom random, int x, int y, int z, FaeRealmBiome biome) {
        Material root = switch (biome) {
            case GOLDEN_MEADOWS -> Material.OAK_WOOD;
            case CRYSTAL_WOODS -> Material.CHERRY_WOOD;
            case MIST_GARDENS -> Material.PALE_OAK_WOOD;
            case ANCIENT_FAE_FOREST -> Material.DARK_OAK_WOOD;
            case SKY_HIGHLANDS -> Material.BIRCH_WOOD;
        };
        int arms = 3 + random.nextInt(4);
        for (int arm = 0; arm < arms; arm++) {
            int dx = random.nextInt(-1, 2);
            int dz = dx == 0 ? (random.nextBoolean() ? 1 : -1) : random.nextInt(-1, 2);
            int length = 2 + random.nextInt(4);
            for (int step = 0; step < length; step++) {
                int px = x + dx * step;
                int pz = z + dz * step;
                int py = region.getHighestBlockYAt(px, pz) + 1;
                setIfAir(region, px, py, pz, root);
                if (random.nextDouble() < 0.35) setIfAir(region, px, py + 1, pz, Material.MOSS_CARPET);
            }
        }
    }

    private void placeHangingGrowth(WorldInfo info, LimitedRegion region, SplittableRandom random,
                                    Ecology ecology, int x, int z, int searchFloor) {
        int bottomY = findLowestSolid(info, region, x, z, Math.max(info.getMinHeight(), searchFloor));
        if (bottomY <= info.getMinHeight() + 8) return;
        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
        Material hanging = switch (biome) {
            case MIST_GARDENS -> Material.PALE_HANGING_MOSS;
            case ANCIENT_FAE_FOREST -> ecology.moisture() > 0.55 ? Material.CAVE_VINES : Material.HANGING_ROOTS;
            case CRYSTAL_WOODS -> ecology.magic() > 0.68 ? Material.CAVE_VINES : Material.HANGING_ROOTS;
            case GOLDEN_MEADOWS, SKY_HIGHLANDS -> Material.HANGING_ROOTS;
        };
        int strands = 1 + (ecology.band().ordinal() >= GrowthBand.LUSH.ordinal() ? random.nextInt(3) : 0);
        for (int strand = 0; strand < strands; strand++) {
            int px = x + random.nextInt(-1, 2);
            int pz = z + random.nextInt(-1, 2);
            int length = 2 + random.nextInt(3 + Math.max(1, ecology.band().ordinal()));
            for (int drop = 1; drop <= length; drop++) if (!setIfAir(region, px, bottomY - drop, pz, hanging)) break;
        }
    }

    private boolean isFaeSurface(WorldInfo info, LimitedRegion region, int x, int y, int z) {
        if (y < info.getMinHeight() || y + 1 >= info.getMaxHeight() || !region.isInRegion(x, y, z)) return false;
        return region.getType(x, y, z) == AetherChunkGenerator.biomeAt(info.getSeed(), x, z).surface();
    }

    private int findLowestSolid(WorldInfo info, LimitedRegion region, int x, int z, int startY) {
        for (int y = startY; y < info.getMaxHeight(); y++) {
            if (region.isInRegion(x, y, z)) {
                Material material = region.getType(x, y, z);
                if (material.isSolid() && material != Material.WHITE_WOOL && material != Material.SNOW_BLOCK) return y;
            }
        }
        return info.getMinHeight() - 1;
    }

    private int scaledAttempts(int baseAttempts, double density, SplittableRandom random) {
        double expected = Math.max(0.0, baseAttempts * density);
        int whole = (int) Math.floor(expected);
        return whole + (random.nextDouble() < expected - whole ? 1 : 0);
    }

    private boolean setIfAir(LimitedRegion region, int x, int y, int z, Material material) {
        if (!region.isInRegion(x, y, z) || !region.getType(x, y, z).isAir()) return false;
        region.setType(x, y, z, material);
        return true;
    }

    private Material pick(SplittableRandom random, Material... materials) { return materials[random.nextInt(materials.length)]; }
    private double unit(double value) { return clamp(0.0, 1.0, (value + 1.0) * 0.5); }
    private double clamp(double min, double max, double value) { return Math.max(min, Math.min(max, value)); }

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

    private enum GrowthBand { SPARSE, MEADOW, LUSH, ANCIENT, ENCHANTED }
    private record Ecology(double moisture, double magic, double growthMultiplier, GrowthBand band) {}
}
