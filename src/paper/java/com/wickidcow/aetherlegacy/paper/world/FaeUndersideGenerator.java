package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.SplittableRandom;

/**
 * Dense hanging ecology and formations beneath Fae islands.
 *
 * <p>The underside is part of the realm's silhouette, not empty filler. Long roots,
 * mossy tendrils, crystal teeth and hanging vegetation should make islands look like
 * pieces of an ancient living world torn into the sky.</p>
 */
public final class FaeUndersideGenerator {

    private static final long UNDERSIDE_SALT = 0x082EFA98EC4E6C89L;
    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };

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

        int baseAttempts = 9 + random.nextInt(7);
        int attempts = scaledAttempts(baseAttempts, settings.decorationDensity(), random);
        for (int attempt = 0; attempt < attempts; attempt++) {
            int worldX = baseX + 2 + random.nextInt(12);
            int worldZ = baseZ + 2 + random.nextInt(12);
            int bottomY = findLowestSolid(info, region, worldX, worldZ, searchFloor);
            if (bottomY < searchFloor || bottomY - info.getMinHeight() < 14) {
                continue;
            }

            FaeRegionProfile profile = AetherChunkGenerator.regionProfileAt(info.getSeed(), worldX, worldZ);
            double roll = random.nextDouble();
            if (roll < 0.45) {
                placeRootCurtain(info, region, worldX, bottomY, worldZ, profile, random);
            } else if (roll < 0.78) {
                placeHangingSpire(info, region, worldX, bottomY, worldZ, profile, random);
            } else {
                placeUndersideGarden(info, region, worldX, bottomY, worldZ, profile, random);
            }
        }
    }

    private void placeRootCurtain(WorldInfo info,
                                  LimitedRegion region,
                                  int x,
                                  int bottomY,
                                  int z,
                                  FaeRegionProfile profile,
                                  SplittableRandom random) {
        Material root = rootMaterial(profile);
        int strands = 2 + random.nextInt(4);
        int[] drift = DIRECTIONS[random.nextInt(DIRECTIONS.length)];

        for (int strand = 0; strand < strands; strand++) {
            int sx = x + random.nextInt(-2, 3);
            int sz = z + random.nextInt(-2, 3);
            int length = 8 + random.nextInt(19);
            int px = sx;
            int pz = sz;

            for (int drop = 1; drop <= length; drop++) {
                int y = bottomY - drop;
                if (y <= info.getMinHeight() + 2 || !isAir(region, px, y, pz)) {
                    break;
                }

                Material material = drop <= 3 ? profile.biome().core() : root;
                region.setType(px, y, pz, material);

                if (drop > 4 && drop % 5 == 0 && random.nextDouble() < 0.55) {
                    px += drift[0];
                    pz += drift[1];
                }

                if (drop > 3 && random.nextDouble() < 0.18) {
                    int[] side = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                    setIfAir(region, px + side[0], y, pz + side[1], Material.VINE);
                }
            }
        }

        if (profile.biome() == FaeRealmBiome.ANCIENT_FAE_FOREST
            || profile.anomaly() == FaeRegionProfile.Anomaly.WILDBLOOM) {
            for (int[] direction : DIRECTIONS) {
                if (random.nextDouble() < 0.62) {
                    setIfAir(region, x + direction[0], bottomY - 1, z + direction[1], Material.MOSS_BLOCK);
                    setIfAir(region, x + direction[0] * 2, bottomY - 2, z + direction[1] * 2,
                        Material.MANGROVE_ROOTS);
                }
            }
        }
    }

    private void placeHangingSpire(WorldInfo info,
                                   LimitedRegion region,
                                   int x,
                                   int bottomY,
                                   int z,
                                   FaeRegionProfile profile,
                                   SplittableRandom random) {
        int length = 7 + random.nextInt(18);
        Material shaft = undersideMaterial(profile.biome(), random);
        int radius = length >= 18 ? 2 : 1;

        for (int drop = 1; drop <= length; drop++) {
            int y = bottomY - drop;
            if (y <= info.getMinHeight() + 2) {
                break;
            }
            int taper = drop > length * 0.58 ? Math.max(0, radius - 1) : radius;
            placeDiscIfAir(region, x, y, z, taper, shaft);
        }

        int tipY = bottomY - length - 1;
        if (tipY > info.getMinHeight() + 1) {
            Material tip = switch (profile.biome()) {
                case CRYSTAL_WOODS -> Material.AMETHYST_CLUSTER;
                case MIST_GARDENS -> Material.SOUL_LANTERN;
                case GOLDEN_MEADOWS -> Material.GLOWSTONE;
                case ANCIENT_FAE_FOREST -> Material.MANGROVE_ROOTS;
                case SKY_HIGHLANDS -> Material.CALCITE;
            };
            setIfAir(region, x, tipY, z, tip);
        }

        if (profile.biome() == FaeRealmBiome.CRYSTAL_WOODS
            || profile.anomaly() == FaeRegionProfile.Anomaly.STARFALL) {
            int branches = 2 + random.nextInt(3);
            for (int i = 0; i < branches; i++) {
                int[] direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                int startY = bottomY - 3 - random.nextInt(Math.max(2, length / 2));
                int branchLength = 2 + random.nextInt(4);
                for (int step = 1; step <= branchLength; step++) {
                    int bx = x + direction[0] * step;
                    int bz = z + direction[1] * step;
                    int by = startY - step / 2;
                    setIfAir(region, bx, by, bz,
                        step == branchLength ? Material.AMETHYST_CLUSTER : Material.AMETHYST_BLOCK);
                }
            }
        }
    }

    private void placeUndersideGarden(WorldInfo info,
                                      LimitedRegion region,
                                      int x,
                                      int bottomY,
                                      int z,
                                      FaeRegionProfile profile,
                                      SplittableRandom random) {
        int radius = 2 + random.nextInt(3);
        Material mat = switch (profile.biome()) {
            case GOLDEN_MEADOWS -> Material.MOSS_BLOCK;
            case CRYSTAL_WOODS -> Material.CALCITE;
            case MIST_GARDENS -> Material.PALE_MOSS_BLOCK;
            case ANCIENT_FAE_FOREST -> Material.MOSS_BLOCK;
            case SKY_HIGHLANDS -> Material.ROOTED_DIRT;
        };

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 1) {
                    continue;
                }
                setIfAir(region, x + dx, bottomY - 1, z + dz, mat);
                if (random.nextDouble() < 0.38) {
                    int length = 4 + random.nextInt(13);
                    Material hanging = profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM
                        ? Material.VINE
                        : (random.nextBoolean() ? Material.VINE : Material.MANGROVE_ROOTS);
                    for (int d = 2; d < length + 2; d++) {
                        int y = bottomY - d;
                        if (y <= info.getMinHeight() + 1 || !isAir(region, x + dx, y, z + dz)) {
                            break;
                        }
                        region.setType(x + dx, y, z + dz, hanging);
                    }
                }
            }
        }

        if (profile.anomaly() == FaeRegionProfile.Anomaly.WILDBLOOM) {
            setIfAir(region, x, bottomY - 2, z, Material.SHROOMLIGHT);
        } else if (profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM) {
            setIfAir(region, x, bottomY - 2, z, Material.SOUL_LANTERN);
        } else if (profile.anomaly() == FaeRegionProfile.Anomaly.STARFALL) {
            setIfAir(region, x, bottomY - 2, z, Material.AMETHYST_CLUSTER);
        }
    }

    private int scaledAttempts(int baseAttempts, double density, SplittableRandom random) {
        double expected = Math.max(0.0, baseAttempts * density);
        int whole = (int) Math.floor(expected);
        return whole + (random.nextDouble() < expected - whole ? 1 : 0);
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

    private Material rootMaterial(FaeRegionProfile profile) {
        if (profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM) {
            return Material.MANGROVE_ROOTS;
        }
        return switch (profile.biome()) {
            case GOLDEN_MEADOWS -> Material.ROOTED_DIRT;
            case CRYSTAL_WOODS -> Material.MANGROVE_ROOTS;
            case MIST_GARDENS -> Material.HANGING_ROOTS;
            case ANCIENT_FAE_FOREST -> Material.MANGROVE_ROOTS;
            case SKY_HIGHLANDS -> Material.ROOTED_DIRT;
        };
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

    private void placeDiscIfAir(LimitedRegion region,
                                int x,
                                int y,
                                int z,
                                int radius,
                                Material material) {
        if (radius <= 0) {
            setIfAir(region, x, y, z, material);
            return;
        }
        double limit = radius * radius + 0.65;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= limit) {
                    setIfAir(region, x + dx, y, z + dz, material);
                }
            }
        }
    }

    private boolean isAir(LimitedRegion region, int x, int y, int z) {
        return region.isInRegion(x, y, z) && region.getType(x, y, z).isAir();
    }

    private void setIfAir(LimitedRegion region, int x, int y, int z, Material material) {
        if (isAir(region, x, y, z)) {
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
