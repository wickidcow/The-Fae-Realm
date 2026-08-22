package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.SplittableRandom;

/**
 * Adds sparse hanging formations beneath floating islands so the realm remains
 * visually interesting from below without force-loading neighboring chunks.
 */
public final class FaeUndersideGenerator {

    private static final long UNDERSIDE_SALT = 0x082EFA98EC4E6C89L;

    public void populate(WorldInfo info,
                         int chunkX,
                         int chunkZ,
                         LimitedRegion region,
                         FaeGeneratorSettings settings) {
        SplittableRandom random = new SplittableRandom(
            mixSeed(info.getSeed() ^ UNDERSIDE_SALT, chunkX, chunkZ));
        int searchFloor = Math.max(info.getMinHeight(), settings.cloudLevel() + 12);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        int attempts = 5 + random.nextInt(4);
        for (int attempt = 0; attempt < attempts; attempt++) {
            int worldX = baseX + 2 + random.nextInt(12);
            int worldZ = baseZ + 2 + random.nextInt(12);
            int bottomY = findLowestSolid(info, region, worldX, worldZ, searchFloor);
            if (bottomY < searchFloor || bottomY - info.getMinHeight() < 10) {
                continue;
            }

            FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), worldX, worldZ);
            int length = 2 + random.nextInt(7);
            Material shaft = undersideMaterial(biome, random);
            int placed = 0;

            for (int drop = 1; drop <= length; drop++) {
                int y = bottomY - drop;
                if (y <= info.getMinHeight()
                    || !region.isInRegion(worldX, y, worldZ)
                    || !region.getType(worldX, y, worldZ).isAir()) {
                    break;
                }
                region.setType(worldX, y, worldZ, shaft);
                placed++;

                if (drop <= 2 && length >= 6) {
                    setIfAir(region, worldX + 1, y, worldZ, shaft);
                    setIfAir(region, worldX, y, worldZ + 1, shaft);
                }
            }

            int tipY = bottomY - placed - 1;
            if (placed > 0 && tipY > info.getMinHeight()
                && region.isInRegion(worldX, tipY, worldZ)
                && region.getType(worldX, tipY, worldZ).isAir()) {
                Material tip = switch (biome) {
                    case CRYSTAL_WOODS -> Material.AMETHYST_BLOCK;
                    case MIST_GARDENS -> Material.SOUL_LANTERN;
                    case GOLDEN_MEADOWS -> Material.GLOWSTONE;
                    case ANCIENT_FAE_FOREST -> Material.MOSSY_COBBLESTONE;
                    case SKY_HIGHLANDS -> Material.CALCITE;
                };
                region.setType(worldX, tipY, worldZ, tip);
            }
        }
    }

    private int findLowestSolid(WorldInfo info,
                                LimitedRegion region,
                                int x,
                                int z,
                                int startY) {
        for (int y = startY; y < info.getMaxHeight(); y++) {
            if (!region.isInRegion(x, y, z)) {
                continue;
            }
            Material type = region.getType(x, y, z);
            if (type.isSolid() && type != Material.WHITE_WOOL && type != Material.SNOW_BLOCK) {
                return y;
            }
        }
        return info.getMinHeight() - 1;
    }

    private Material undersideMaterial(FaeRealmBiome biome, SplittableRandom random) {
        return switch (biome) {
            case GOLDEN_MEADOWS -> random.nextBoolean() ? Material.STONE : Material.CALCITE;
            case CRYSTAL_WOODS -> random.nextInt(3) == 0 ? Material.AMETHYST_BLOCK : Material.CALCITE;
            case MIST_GARDENS -> random.nextBoolean() ? Material.TUFF : Material.POLISHED_TUFF;
            case ANCIENT_FAE_FOREST -> random.nextBoolean() ? Material.STONE : Material.MOSSY_COBBLESTONE;
            case SKY_HIGHLANDS -> random.nextBoolean() ? Material.ANDESITE : Material.CALCITE;
        };
    }

    private void setIfAir(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z) && region.getType(x, y, z).isAir()) {
            region.setType(x, y, z, material);
        }
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
}
