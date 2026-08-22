package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

/**
 * Thread-safe first-pass floating-island generator for the Paper port.
 *
 * <p>This intentionally uses only vanilla Paper blocks. It is a clean-room
 * foundation for the server-side port and does not package the original
 * Aether mod's all-rights-reserved assets.</p>
 */
public final class AetherChunkGenerator extends ChunkGenerator {

    private static final int CELL_SIZE = 96;
    private static final int MAX_RADIUS = 44;

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo,
                              @NotNull Random random,
                              int chunkX,
                              int chunkZ,
                              @NotNull ChunkData chunkData) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        List<Island> islands = collectNearbyIslands(worldInfo.getSeed(), minX, maxX, minZ, maxZ);
        for (Island island : islands) {
            carveIsland(chunkData, island, minX, minZ);
        }
    }

    private List<Island> collectNearbyIslands(long worldSeed, int minX, int maxX, int minZ, int maxZ) {
        List<Island> islands = new ArrayList<>();

        // Guaranteed starter island at world origin so first entry is always safe.
        if (intersects(minX, maxX, minZ, maxZ, 0, 0, 48)) {
            islands.add(new Island(0, 0, 128, 48, 18));
        }

        int minCellX = Math.floorDiv(minX - MAX_RADIUS, CELL_SIZE);
        int maxCellX = Math.floorDiv(maxX + MAX_RADIUS, CELL_SIZE);
        int minCellZ = Math.floorDiv(minZ - MAX_RADIUS, CELL_SIZE);
        int maxCellZ = Math.floorDiv(maxZ + MAX_RADIUS, CELL_SIZE);

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                // Keep the origin dominated by the guaranteed starter island.
                if (Math.abs(cellX) <= 1 && Math.abs(cellZ) <= 1) {
                    continue;
                }

                SplittableRandom cellRandom = new SplittableRandom(mixSeed(worldSeed, cellX, cellZ));
                if (cellRandom.nextDouble() > 0.78) {
                    continue;
                }

                int centerX = cellX * CELL_SIZE + cellRandom.nextInt(18, CELL_SIZE - 18);
                int centerZ = cellZ * CELL_SIZE + cellRandom.nextInt(18, CELL_SIZE - 18);
                int centerY = cellRandom.nextInt(96, 166);
                int radius = cellRandom.nextInt(22, MAX_RADIUS + 1);
                int thickness = cellRandom.nextInt(10, 22);

                if (intersects(minX, maxX, minZ, maxZ, centerX, centerZ, radius)) {
                    islands.add(new Island(centerX, centerZ, centerY, radius, thickness));
                }
            }
        }

        return islands;
    }

    private void carveIsland(ChunkData chunkData, Island island, int chunkMinX, int chunkMinZ) {
        int minHeight = chunkData.getMinHeight();
        int maxHeight = chunkData.getMaxHeight() - 1;

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkMinX + localX;
            double dx = worldX - island.x();

            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkMinZ + localZ;
                double dz = worldZ - island.z();
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (distance > island.radius()) {
                    continue;
                }

                double edge = 1.0 - (distance / island.radius());
                double ripple = Math.sin(worldX * 0.105) * 1.8 + Math.cos(worldZ * 0.095) * 1.8;
                int topY = island.y() + (int) Math.round(edge * 5.0 + ripple);
                int depth = Math.max(3, (int) Math.round(island.thickness() * (0.18 + edge * edge * 0.82)));
                int bottomY = topY - depth;

                topY = Math.min(topY, maxHeight);
                bottomY = Math.max(bottomY, minHeight);

                for (int y = bottomY; y <= topY; y++) {
                    Material material;
                    if (y == topY) {
                        material = Material.GRASS_BLOCK;
                    } else if (y >= topY - 3) {
                        material = Material.DIRT;
                    } else if (y <= bottomY + 1 && edge < 0.45) {
                        material = Material.CALCITE;
                    } else {
                        material = Material.STONE;
                    }
                    chunkData.setBlock(localX, y, localZ, material);
                }
            }
        }
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return true;
    }

    private static boolean intersects(int minX, int maxX, int minZ, int maxZ,
                                      int centerX, int centerZ, int radius) {
        return centerX + radius >= minX
            && centerX - radius <= maxX
            && centerZ + radius >= minZ
            && centerZ - radius <= maxZ;
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

    private record Island(int x, int z, int y, int radius, int thickness) {
    }
}
