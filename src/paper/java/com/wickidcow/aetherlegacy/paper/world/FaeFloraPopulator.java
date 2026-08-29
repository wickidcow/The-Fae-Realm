package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.SplittableRandom;

/**
 * Dense, deterministic ecology layer for Fae Realm islands.
 *
 * <p>This pass intentionally does not use Bukkit's vanilla tree generator. Fae trees
 * are assembled from simple block primitives so their silhouettes can bend, fork,
 * glow and change character by Fae region while remaining fully server-side and
 * safe inside Paper's {@link LimitedRegion} generation window.</p>
 */
public final class FaeFloraPopulator {

    private static final long FLORA_SALT = 0x243F6A8885A308D3L;
    private static final long TREE_SALT = 0x13198A2E03707344L;

    public void populate(WorldInfo info,
                         int chunkX,
                         int chunkZ,
                         LimitedRegion region,
                         double density) {
        if (density <= 0.0) {
            return;
        }

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        SplittableRandom floraRandom = new SplittableRandom(
            mixSeed(info.getSeed() ^ FLORA_SALT, chunkX, chunkZ));

        int groundAttempts = scaledAttempts(26, density, floraRandom);
        for (int attempt = 0; attempt < groundAttempts; attempt++) {
            int x = baseX + floraRandom.nextInt(16);
            int z = baseZ + floraRandom.nextInt(16);
            FaeRegionProfile profile = AetherChunkGenerator.regionProfileAt(info.getSeed(), x, z);
            int surfaceY = findFaeSurface(info, region, x, z, profile.biome());
            if (surfaceY == Integer.MIN_VALUE) {
                continue;
            }

            double vegetation = Math.max(0.25, profile.vegetationMultiplier());
            if (floraRandom.nextDouble() <= Math.min(1.0, 0.78 * vegetation)) {
                placeGroundGrowth(region, x, surfaceY + 1, z, profile, floraRandom);
            }
        }

        SplittableRandom treeRandom = new SplittableRandom(
            mixSeed(info.getSeed() ^ TREE_SALT, chunkX, chunkZ));
        int treeAttempts = scaledAttempts(3, density, treeRandom);
        for (int attempt = 0; attempt < treeAttempts; attempt++) {
            // Keep the trunk away from the central chunk edge. The LimitedRegion buffer
            // still allows crowns and roots to cross naturally into neighboring chunks.
            int x = baseX + 3 + treeRandom.nextInt(10);
            int z = baseZ + 3 + treeRandom.nextInt(10);
            FaeRegionProfile profile = AetherChunkGenerator.regionProfileAt(info.getSeed(), x, z);
            int surfaceY = findFaeSurface(info, region, x, z, profile.biome());
            if (surfaceY == Integer.MIN_VALUE || surfaceY + 18 >= info.getMaxHeight()) {
                continue;
            }

            double chance = treeChance(profile) * Math.min(1.35, Math.max(0.35, density));
            if (treeRandom.nextDouble() < Math.min(0.96, chance)) {
                placeFaeTree(region, x, surfaceY + 1, z, profile, treeRandom);
            }
        }
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
            if (region.getType(x, y, z) == biome.surface()
                && region.isInRegion(x, y + 1, z)
                && region.getType(x, y + 1, z).isAir()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private void placeGroundGrowth(LimitedRegion region,
                                   int x,
                                   int y,
                                   int z,
                                   FaeRegionProfile profile,
                                   SplittableRandom random) {
        if (!region.isInRegion(x, y, z) || !region.getType(x, y, z).isAir()) {
            return;
        }

        Material growth = switch (profile.biome()) {
            case GOLDEN_MEADOWS -> pick(random,
                Material.SHORT_GRASS,
                Material.DANDELION,
                Material.POPPY,
                Material.CORNFLOWER,
                Material.OXEYE_DAISY);
            case CRYSTAL_WOODS -> pick(random,
                Material.MOSS_CARPET,
                Material.AZALEA,
                Material.FLOWERING_AZALEA,
                Material.PINK_PETALS,
                Material.ALLIUM);
            case MIST_GARDENS -> pick(random,
                Material.PALE_MOSS_CARPET,
                Material.ALLIUM,
                Material.BLUE_ORCHID,
                Material.BROWN_MUSHROOM,
                Material.RED_MUSHROOM);
            case ANCIENT_FAE_FOREST -> pick(random,
                Material.FERN,
                Material.MOSS_CARPET,
                Material.BROWN_MUSHROOM,
                Material.RED_MUSHROOM,
                Material.AZALEA);
            case SKY_HIGHLANDS -> pick(random,
                Material.SHORT_GRASS,
                Material.CORNFLOWER,
                Material.OXEYE_DAISY,
                Material.AZURE_BLUET,
                Material.FERN);
        };

        if (profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM && random.nextDouble() < 0.14) {
            growth = random.nextBoolean() ? Material.WITHER_ROSE : Material.RED_MUSHROOM;
        } else if (profile.anomaly() == FaeRegionProfile.Anomaly.WILDBLOOM && random.nextDouble() < 0.24) {
            growth = random.nextBoolean() ? Material.PINK_PETALS : Material.FLOWERING_AZALEA;
        } else if (profile.anomaly() == FaeRegionProfile.Anomaly.STARFALL && random.nextDouble() < 0.10) {
            growth = Material.AMETHYST_CLUSTER;
        }

        region.setType(x, y, z, growth);
    }

    private void placeFaeTree(LimitedRegion region,
                              int baseX,
                              int baseY,
                              int baseZ,
                              FaeRegionProfile profile,
                              SplittableRandom random) {
        TreePalette palette = palette(profile);
        int height = switch (profile.biome()) {
            case ANCIENT_FAE_FOREST -> 10 + random.nextInt(6);
            case CRYSTAL_WOODS -> 8 + random.nextInt(6);
            case MIST_GARDENS -> 7 + random.nextInt(5);
            case SKY_HIGHLANDS -> 7 + random.nextInt(5);
            case GOLDEN_MEADOWS -> 8 + random.nextInt(5);
        };

        int x = baseX;
        int z = baseZ;
        for (int dy = 0; dy < height; dy++) {
            int y = baseY + dy;
            set(region, x, y, z, palette.log());

            // An occasional one-block drift gives the trunk an organic corkscrew
            // silhouette instead of a vanilla vertical column.
            if (dy > 1 && dy < height - 2 && dy % 3 == 0 && random.nextDouble() < 0.72) {
                int direction = random.nextInt(4);
                int nextX = x + switch (direction) {
                    case 0 -> 1;
                    case 1 -> -1;
                    default -> 0;
                };
                int nextZ = z + switch (direction) {
                    case 2 -> 1;
                    case 3 -> -1;
                    default -> 0;
                };
                if (region.isInRegion(nextX, y + 1, nextZ)) {
                    set(region, nextX, y, nextZ, palette.log());
                    x = nextX;
                    z = nextZ;
                }
            }
        }

        int crownY = baseY + height - 1;
        placeLeafCloud(region, x, crownY, z, palette, 3, random);

        int branches = 3 + random.nextInt(3);
        for (int branch = 0; branch < branches; branch++) {
            int direction = (branch + random.nextInt(4)) & 3;
            int dx = switch (direction) {
                case 0 -> 1;
                case 1 -> -1;
                default -> 0;
            };
            int dz = switch (direction) {
                case 2 -> 1;
                case 3 -> -1;
                default -> 0;
            };
            int branchY = crownY - 2 - random.nextInt(4);
            int length = 2 + random.nextInt(4);
            int bx = x;
            int bz = z;
            int by = branchY;
            for (int step = 0; step < length; step++) {
                bx += dx;
                bz += dz;
                if ((step & 1) == 1) {
                    by++;
                }
                set(region, bx, by, bz, palette.log());
            }
            placeLeafCloud(region, bx, by + 1, bz, palette, 2, random);
            if (random.nextDouble() < 0.48) {
                hangTendrils(region, bx, by, bz, palette, random);
            }
        }

        placeRoots(region, baseX, baseY, baseZ, palette, random);

        if (profile.biome() == FaeRealmBiome.CRYSTAL_WOODS
            || profile.anomaly() == FaeRegionProfile.Anomaly.STARFALL) {
            placeCrystalTips(region, x, crownY + 1, z, random);
        }
    }

    private void placeLeafCloud(LimitedRegion region,
                                int x,
                                int y,
                                int z,
                                TreePalette palette,
                                int radius,
                                SplittableRandom random) {
        for (int dy = -1; dy <= 2; dy++) {
            int layerRadius = dy == 2 ? Math.max(1, radius - 1) : radius;
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    double distance = (dx * dx) + (dz * dz) + (dy * dy * 1.4);
                    if (distance > layerRadius * layerRadius + 1.5) {
                        continue;
                    }
                    if (random.nextDouble() < 0.12 && Math.abs(dx) + Math.abs(dz) > 2) {
                        continue;
                    }
                    setIfAir(region, x + dx, y + dy, z + dz, palette.leaves());
                }
            }
        }

        if (random.nextDouble() < 0.70) {
            set(region, x, y, z, palette.glow());
        }
    }

    private void hangTendrils(LimitedRegion region,
                              int x,
                              int y,
                              int z,
                              TreePalette palette,
                              SplittableRandom random) {
        int[][] offsets = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        int[] offset = offsets[random.nextInt(offsets.length)];
        int tx = x + offset[0];
        int tz = z + offset[1];
        int length = 1 + random.nextInt(4);
        for (int i = 0; i < length; i++) {
            int ty = y - i;
            if (!region.isInRegion(tx, ty, tz) || !region.getType(tx, ty, tz).isAir()) {
                break;
            }
            region.setType(tx, ty, tz, palette.tendril());
        }
    }

    private void placeRoots(LimitedRegion region,
                            int x,
                            int y,
                            int z,
                            TreePalette palette,
                            SplittableRandom random) {
        int[][] roots = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] root : roots) {
            if (random.nextDouble() < 0.78) {
                set(region, x + root[0], y, z + root[1], palette.log());
                if (random.nextBoolean()) {
                    set(region, x + root[0] * 2, y - 1, z + root[1] * 2, palette.root());
                }
            }
        }
    }

    private void placeCrystalTips(LimitedRegion region,
                                  int x,
                                  int y,
                                  int z,
                                  SplittableRandom random) {
        int[][] offsets = {{0, 2}, {2, 0}, {0, -2}, {-2, 0}, {0, 0}};
        for (int[] offset : offsets) {
            if (random.nextDouble() < 0.52) {
                int px = x + offset[0];
                int pz = z + offset[1];
                setIfAir(region, px, y, pz, Material.AMETHYST_BLOCK);
                setIfAir(region, px, y + 1, pz, Material.AMETHYST_CLUSTER);
            }
        }
    }

    private TreePalette palette(FaeRegionProfile profile) {
        TreePalette base = switch (profile.biome()) {
            case GOLDEN_MEADOWS -> new TreePalette(
                Material.OAK_LOG, Material.FLOWERING_AZALEA_LEAVES,
                Material.MOSS_BLOCK, Material.SHROOMLIGHT, Material.VINE);
            case CRYSTAL_WOODS -> new TreePalette(
                Material.CHERRY_LOG, Material.CHERRY_LEAVES,
                Material.ROOTED_DIRT, Material.SEA_LANTERN, Material.VINE);
            case MIST_GARDENS -> new TreePalette(
                Material.PALE_OAK_LOG, Material.PALE_OAK_LEAVES,
                Material.PALE_MOSS_BLOCK, Material.SHROOMLIGHT, Material.VINE);
            case ANCIENT_FAE_FOREST -> new TreePalette(
                Material.DARK_OAK_LOG, Material.DARK_OAK_LEAVES,
                Material.MOSS_BLOCK, Material.SHROOMLIGHT, Material.VINE);
            case SKY_HIGHLANDS -> new TreePalette(
                Material.BIRCH_LOG, Material.AZALEA_LEAVES,
                Material.COARSE_DIRT, Material.GLOWSTONE, Material.VINE);
        };

        if (profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM) {
            return new TreePalette(
                base.log(), Material.DARK_OAK_LEAVES,
                Material.SCULK, Material.SOUL_LANTERN, Material.VINE);
        }
        if (profile.anomaly() == FaeRegionProfile.Anomaly.WILDBLOOM) {
            return new TreePalette(
                base.log(), Material.FLOWERING_AZALEA_LEAVES,
                Material.MOSS_BLOCK, Material.SHROOMLIGHT, Material.VINE);
        }
        return base;
    }

    private double treeChance(FaeRegionProfile profile) {
        double base = switch (profile.biome()) {
            case GOLDEN_MEADOWS -> 0.42;
            case CRYSTAL_WOODS -> 0.74;
            case MIST_GARDENS -> 0.62;
            case ANCIENT_FAE_FOREST -> 0.86;
            case SKY_HIGHLANDS -> 0.46;
        };
        return Math.max(0.12, Math.min(0.95, base * profile.vegetationMultiplier()));
    }

    private int scaledAttempts(int baseAttempts, double density, SplittableRandom random) {
        double expected = Math.max(0.0, baseAttempts * density);
        int whole = (int) Math.floor(expected);
        return whole + (random.nextDouble() < expected - whole ? 1 : 0);
    }

    private Material pick(SplittableRandom random, Material... values) {
        return values[random.nextInt(values.length)];
    }

    private void set(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) {
            region.setType(x, y, z, material);
        }
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

    private record TreePalette(
        Material log,
        Material leaves,
        Material root,
        Material glow,
        Material tendril
    ) {
    }
}
