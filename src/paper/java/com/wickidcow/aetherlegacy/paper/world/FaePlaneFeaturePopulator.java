package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.SplittableRandom;

/**
 * Signature ecology and landmark pass for the linked Fae planes.
 * The ordinary Fae region/subregion decorators still run first; this layer gives
 * Wildbloom, Gloam and Starfall an unmistakable identity of their own.
 */
public final class FaePlaneFeaturePopulator {

    private static final long PLANE_FEATURE_SALT = 0x510E527FADE682D1L;

    public void populate(WorldInfo info,
                         int chunkX,
                         int chunkZ,
                         LimitedRegion region,
                         double density) {
        FaePlane plane = FaePlane.fromWorldName(info.getName());
        if (plane == FaePlane.REALM || density <= 0.0) {
            return;
        }

        SplittableRandom random = new SplittableRandom(
            mixSeed(info.getSeed() ^ PLANE_FEATURE_SALT ^ (plane.ordinal() * 0x9E3779B9L), chunkX, chunkZ));

        double chance = Math.min(0.72, 0.24 * Math.max(0.5, density));
        if (random.nextDouble() > chance) {
            return;
        }

        int x = (chunkX << 4) + 4 + random.nextInt(8);
        int z = (chunkZ << 4) + 4 + random.nextInt(8);
        FaeRegionProfile base = AetherChunkGenerator.regionProfileAt(info.getSeed(), x, z);
        FaeRegionProfile profile = plane.apply(base);
        int surfaceY = findSurface(info, region, x, z, profile.biome());
        if (surfaceY == Integer.MIN_VALUE || surfaceY + 22 >= info.getMaxHeight()) {
            return;
        }

        switch (plane) {
            case REALM -> {
            }
            case WILDBLOOM -> placeWildbloomFeature(region, x, surfaceY + 1, z, random);
            case GLOAM -> placeGloamFeature(region, x, surfaceY + 1, z, random);
            case STARFALL -> placeStarfallFeature(region, x, surfaceY + 1, z, random);
        }
    }

    private void placeWildbloomFeature(LimitedRegion region,
                                       int x,
                                       int y,
                                       int z,
                                       SplittableRandom random) {
        int choice = random.nextInt(3);
        if (choice == 0) {
            placeBloomTree(region, x, y, z, random);
        } else if (choice == 1) {
            placeLivingArch(region, x, y, z, random);
        } else {
            placeGiantFlower(region, x, y, z, random);
        }
    }

    private void placeBloomTree(LimitedRegion region,
                                int x,
                                int y,
                                int z,
                                SplittableRandom random) {
        int height = 11 + random.nextInt(7);
        int driftX = random.nextBoolean() ? 1 : -1;
        int driftZ = random.nextBoolean() ? 1 : -1;
        int tx = x;
        int tz = z;

        for (int dy = 0; dy < height; dy++) {
            set(region, tx, y + dy, tz, Material.CHERRY_LOG);
            if (dy > 2 && dy % 4 == 0) {
                if (random.nextBoolean()) {
                    tx += driftX;
                } else {
                    tz += driftZ;
                }
            }
        }

        int crownY = y + height - 2;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    double distance = dx * dx + dz * dz + dy * dy * 2.0;
                    if (distance <= 18.5 && random.nextDouble() > 0.10) {
                        setIfAir(region, tx + dx, crownY + dy, tz + dz, Material.FLOWERING_AZALEA_LEAVES);
                    }
                }
            }
        }

        set(region, tx, crownY, tz, Material.SHROOMLIGHT);
        placeBranch(region, tx, crownY - 2, tz, 1, 0, 4, Material.CHERRY_LOG);
        placeBranch(region, tx, crownY - 3, tz, -1, 0, 4, Material.CHERRY_LOG);
        placeBranch(region, tx, crownY - 2, tz, 0, 1, 4, Material.CHERRY_LOG);
        placeBranch(region, tx, crownY - 4, tz, 0, -1, 4, Material.CHERRY_LOG);

        for (int i = 0; i < 8; i++) {
            int vx = tx + random.nextInt(-4, 5);
            int vz = tz + random.nextInt(-4, 5);
            int startY = crownY - random.nextInt(3);
            int length = 2 + random.nextInt(5);
            for (int d = 0; d < length; d++) {
                int vy = startY - d;
                if (!isAir(region, vx, vy, vz)) {
                    break;
                }
                set(region, vx, vy, vz, Material.VINE);
            }
        }

        for (int[] offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            set(region, x + offset[0], y, z + offset[1], Material.MOSS_BLOCK);
            setIfAir(region, x + offset[0] * 2, y, z + offset[1] * 2, Material.FLOWERING_AZALEA);
        }
    }

    private void placeLivingArch(LimitedRegion region,
                                 int x,
                                 int y,
                                 int z,
                                 SplittableRandom random) {
        boolean alongX = random.nextBoolean();
        for (int side : new int[]{-1, 1}) {
            int bx = x + (alongX ? side * 3 : 0);
            int bz = z + (alongX ? 0 : side * 3);
            for (int dy = 0; dy <= 6; dy++) {
                set(region, bx, y + dy, bz, Material.DARK_OAK_LOG);
            }
        }

        for (int step = -3; step <= 3; step++) {
            int ax = x + (alongX ? step : 0);
            int az = z + (alongX ? 0 : step);
            int archY = y + 6 + (3 - Math.abs(step)) / 2;
            set(region, ax, archY, az, Material.DARK_OAK_LOG);
            setIfAir(region, ax, archY + 1, az, Material.FLOWERING_AZALEA_LEAVES);
            if ((step & 1) == 0) {
                setIfAir(region, ax, archY - 1, az, Material.GLOW_LICHEN);
            }
        }

        set(region, x, y, z, Material.MOSS_BLOCK);
        setIfAir(region, x, y + 1, z, Material.SHROOMLIGHT);
    }

    private void placeGiantFlower(LimitedRegion region,
                                  int x,
                                  int y,
                                  int z,
                                  SplittableRandom random) {
        int height = 6 + random.nextInt(5);
        for (int dy = 0; dy < height; dy++) {
            set(region, x, y + dy, z, Material.BAMBOO_BLOCK);
        }
        int flowerY = y + height;
        set(region, x, flowerY, z, Material.SHROOMLIGHT);
        int[][] petals = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        for (int[] petal : petals) {
            setIfAir(region, x + petal[0], flowerY, z + petal[1], Material.PINK_WOOL);
        }
        setIfAir(region, x + 1, flowerY + 1, z, Material.MAGENTA_WOOL);
        setIfAir(region, x - 1, flowerY + 1, z, Material.MAGENTA_WOOL);
        setIfAir(region, x, flowerY + 1, z + 1, Material.MAGENTA_WOOL);
        setIfAir(region, x, flowerY + 1, z - 1, Material.MAGENTA_WOOL);
    }

    private void placeGloamFeature(LimitedRegion region,
                                   int x,
                                   int y,
                                   int z,
                                   SplittableRandom random) {
        int choice = random.nextInt(3);
        if (choice == 0) {
            placeGloamMushroom(region, x, y, z, random);
        } else if (choice == 1) {
            placeDeadSpiral(region, x, y, z, random);
        } else {
            placeGloamPool(region, x, y - 1, z, random);
        }
    }

    private void placeGloamMushroom(LimitedRegion region,
                                    int x,
                                    int y,
                                    int z,
                                    SplittableRandom random) {
        int height = 7 + random.nextInt(6);
        for (int dy = 0; dy < height; dy++) {
            set(region, x, y + dy, z, Material.MUSHROOM_STEM);
        }
        int capY = y + height;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz <= 10) {
                    Material cap = ((dx + dz) & 2) == 0 ? Material.RED_MUSHROOM_BLOCK : Material.BROWN_MUSHROOM_BLOCK;
                    setIfAir(region, x + dx, capY, z + dz, cap);
                }
            }
        }
        set(region, x, capY - 1, z, Material.SOUL_LANTERN);
        set(region, x + 1, y, z, Material.SCULK);
        set(region, x - 1, y, z, Material.SCULK_VEIN);
    }

    private void placeDeadSpiral(LimitedRegion region,
                                 int x,
                                 int y,
                                 int z,
                                 SplittableRandom random) {
        int height = 9 + random.nextInt(6);
        int px = x;
        int pz = z;
        for (int dy = 0; dy < height; dy++) {
            set(region, px, y + dy, pz, Material.DARK_OAK_LOG);
            if (dy > 1 && dy % 2 == 0) {
                int direction = (dy / 2) & 3;
                px += direction == 0 ? 1 : direction == 2 ? -1 : 0;
                pz += direction == 1 ? 1 : direction == 3 ? -1 : 0;
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 3 && random.nextDouble() < 0.62) {
                    setIfAir(region, px + dx, y + height - 1, pz + dz, Material.DARK_OAK_LEAVES);
                }
            }
        }
        setIfAir(region, px, y + height, pz, Material.SOUL_LANTERN);
    }

    private void placeGloamPool(LimitedRegion region,
                                int x,
                                int surfaceY,
                                int z,
                                SplittableRandom random) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 5) {
                    continue;
                }
                set(region, x + dx, surfaceY, z + dz, Material.SCULK);
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    set(region, x + dx, surfaceY + 1, z + dz, Material.WATER);
                } else if (random.nextDouble() < 0.28) {
                    setIfAir(region, x + dx, surfaceY + 1, z + dz, Material.SOUL_LANTERN);
                }
            }
        }
    }

    private void placeStarfallFeature(LimitedRegion region,
                                      int x,
                                      int y,
                                      int z,
                                      SplittableRandom random) {
        if (random.nextBoolean()) {
            placeCrystalNeedles(region, x, y, z, random);
        } else {
            placeImpactScar(region, x, y - 1, z, random);
        }
    }

    private void placeCrystalNeedles(LimitedRegion region,
                                     int x,
                                     int y,
                                     int z,
                                     SplittableRandom random) {
        int[][] offsets = {{0, 0}, {2, 1}, {-2, -1}, {1, -2}, {-1, 2}, {3, -2}};
        for (int i = 0; i < offsets.length; i++) {
            int height = (i == 0 ? 8 : 3) + random.nextInt(i == 0 ? 8 : 5);
            int px = x + offsets[i][0];
            int pz = z + offsets[i][1];
            for (int dy = 0; dy < height; dy++) {
                Material material = dy == 0 ? Material.CALCITE : Material.AMETHYST_BLOCK;
                set(region, px, y + dy, pz, material);
            }
            setIfAir(region, px, y + height, pz, Material.AMETHYST_CLUSTER);
        }
    }

    private void placeImpactScar(LimitedRegion region,
                                 int x,
                                 int surfaceY,
                                 int z,
                                 SplittableRandom random) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 3.3) {
                    continue;
                }
                Material material;
                if (distance < 1.5) {
                    material = random.nextBoolean() ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                } else if (distance < 2.5) {
                    material = Material.CALCITE;
                } else {
                    material = Material.TUFF;
                }
                set(region, x + dx, surfaceY, z + dz, material);
                if (distance < 2.0 && random.nextDouble() < 0.40) {
                    setIfAir(region, x + dx, surfaceY + 1, z + dz, Material.AMETHYST_CLUSTER);
                }
            }
        }
        set(region, x, surfaceY + 1, z, Material.SEA_LANTERN);
    }

    private void placeBranch(LimitedRegion region,
                             int x,
                             int y,
                             int z,
                             int dx,
                             int dz,
                             int length,
                             Material material) {
        for (int step = 1; step <= length; step++) {
            int py = y + step / 3;
            set(region, x + dx * step, py, z + dz * step, material);
        }
    }

    private int findSurface(WorldInfo info,
                            LimitedRegion region,
                            int x,
                            int z,
                            FaeRealmBiome biome) {
        for (int y = info.getMaxHeight() - 2; y >= info.getMinHeight(); y--) {
            if (region.isInRegion(x, y, z) && region.getType(x, y, z) == biome.surface()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private boolean isAir(LimitedRegion region, int x, int y, int z) {
        return region.isInRegion(x, y, z) && region.getType(x, y, z).isAir();
    }

    private void set(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) {
            region.setType(x, y, z, material);
        }
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
