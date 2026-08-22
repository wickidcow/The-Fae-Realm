package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;

import java.util.SplittableRandom;

/**
 * Adds sparse hanging formations beneath floating islands so the realm remains
 * visually interesting from below without adding a second terrain pass.
 */
public final class FaeUndersideGenerator {

    private static final long UNDERSIDE_SALT = 0x082EFA98EC4E6C89L;

    public void generate(ChunkGenerator.ChunkData data,
                         long seed,
                         int chunkX,
                         int chunkZ,
                         FaeGeneratorSettings settings) {
        SplittableRandom random = new SplittableRandom(
            mixSeed(seed ^ UNDERSIDE_SALT, chunkX, chunkZ));
        int searchFloor = Math.max(data.getMinHeight(), settings.cloudLevel() + 12);

        int attempts = 5 + random.nextInt(4);
        for (int attempt = 0; attempt < attempts; attempt++) {
            int localX = 2 + random.nextInt(12);
            int localZ = 2 + random.nextInt(12);
            int bottomY = findLowestSolid(data, localX, localZ, searchFloor);
            if (bottomY < searchFloor || bottomY - data.getMinHeight() < 10) {
                continue;
            }

            int worldX = (chunkX << 4) + localX;
            int worldZ = (chunkZ << 4) + localZ;
            FaeRealmBiome biome = AetherChunkGenerator.biomeAt(seed, worldX, worldZ);

            int length = 2 + random.nextInt(7);
            Material shaft = undersideMaterial(biome, random);
            for (int drop = 1; drop <= length; drop++) {
                int y = bottomY - drop;
                if (y <= data.getMinHeight() || !data.getType(localX, y, localZ).isAir()) {
                    break;
                }
                data.setBlock(localX, y, localZ, shaft);

                // Longer formations start wider then taper into a single hanging point.
                if (drop <= 2 && length >= 6) {
                    setIfAir(data, localX + 1, y, localZ, shaft);
                    setIfAir(data, localX, y, localZ + 1, shaft);
                }
            }

            int tipY = bottomY - length - 1;
            if (tipY > data.getMinHeight() && data.getType(localX, tipY, localZ).isAir()) {
                Material tip = switch (biome) {
                    case CRYSTAL_WOODS -> Material.AMETHYST_BLOCK;
                    case MIST_GARDENS -> Material.SOUL_LANTERN;
                    case GOLDEN_MEADOWS -> Material.GLOWSTONE;
                    case ANCIENT_FAE_FOREST -> Material.MOSSY_COBBLESTONE;
                    case SKY_HIGHLANDS -> Material.CALCITE;
                };
                data.setBlock(localX, tipY, localZ, tip);
            }
        }
    }

    private int findLowestSolid(ChunkGenerator.ChunkData data, int x, int z, int startY) {
        for (int y = startY; y < data.getMaxHeight(); y++) {
            Material type = data.getType(x, y, z);
            if (type.isSolid() && type != Material.WHITE_WOOL && type != Material.SNOW_BLOCK) {
                return y;
            }
        }
        return data.getMinHeight() - 1;
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

    private void setIfAir(ChunkGenerator.ChunkData data, int x, int y, int z, Material material) {
        if (x < 0 || x >= 16 || z < 0 || z >= 16
            || y < data.getMinHeight() || y >= data.getMaxHeight()) {
            return;
        }
        if (data.getType(x, y, z).isAir()) {
            data.setBlock(x, y, z, material);
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
