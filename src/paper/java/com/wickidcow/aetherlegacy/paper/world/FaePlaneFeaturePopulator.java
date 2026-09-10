package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.SplittableRandom;

/**
 * Large signature ecology and landmark pass for the linked Fae planes.
 *
 * <p>These features intentionally sit above the ordinary regional decorators. They
 * should be readable from several chunks away and make Wildbloom, Gloam and
 * Starfall feel like authored fantasy environments rather than palette swaps.</p>
 */
public final class FaePlaneFeaturePopulator {

    private static final long PLANE_FEATURE_SALT = 0x510E527FADE682D1L;
    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };

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

        double chance = Math.min(0.86, 0.34 * Math.max(0.5, density));
        if (random.nextDouble() > chance) {
            return;
        }

        int x = (chunkX << 4) + 5 + random.nextInt(6);
        int z = (chunkZ << 4) + 5 + random.nextInt(6);
        FaeRegionProfile base = AetherChunkGenerator.regionProfileAt(info.getSeed(), x, z);
        FaeRegionProfile profile = plane.apply(base);
        int surfaceY = findSurface(info, region, x, z, profile.biome());
        if (surfaceY == Integer.MIN_VALUE || surfaceY + 72 >= info.getMaxHeight()) {
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
        int choice = random.nextInt(4);
        if (choice <= 1) {
            placeBloomTree(region, x, y, z, random);
        } else if (choice == 2) {
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
        int height = 34 + random.nextInt(17);
        int tx = x;
        int tz = z;

        for (int dy = 0; dy < height; dy++) {
            int radius = dy < height * 0.72 ? 2 : 1;
            placeDisc(region, tx, y + dy, tz, radius, Material.CHERRY_LOG);
            if (dy > 8 && dy < height - 10 && dy % 9 == 0 && random.nextDouble() < 0.62) {
                int[] drift = DIRECTIONS[random.nextInt(4)];
                tx += drift[0];
                tz += drift[1];
            }
        }

        int crownY = y + height - 4;
        placeLeafCloud(region, tx, crownY, tz, 8, Material.FLOWERING_AZALEA_LEAVES, random);
        placeLeafCloud(region, tx + 3, crownY - 2, tz - 2, 6, Material.CHERRY_LEAVES, random);
        placeLeafCloud(region, tx - 3, crownY - 1, tz + 2, 6, Material.FLOWERING_AZALEA_LEAVES, random);
        set(region, tx, crownY, tz, Material.SHROOMLIGHT);

        for (int branch = 0; branch < DIRECTIONS.length; branch++) {
            int[] direction = DIRECTIONS[branch];
            int branchY = crownY - 7 - random.nextInt(10);
            int length = 7 + random.nextInt(6);
            int bx = tx;
            int bz = tz;
            int by = branchY;
            for (int step = 0; step < length; step++) {
                bx += direction[0];
                bz += direction[1];
                if (step % 3 == 2) {
                    by++;
                }
                placeDisc(region, bx, by, bz, step < 4 ? 1 : 0, Material.CHERRY_LOG);
            }
            placeLeafCloud(region, bx, by + 1, bz, 4 + random.nextInt(2),
                Material.FLOWERING_AZALEA_LEAVES, random);
            hangVines(region, bx, by, bz, 3 + random.nextInt(4), 7, 16, random);
        }

        placeRadialRoots(region, x, y, z, Material.CHERRY_LOG, Material.MOSS_BLOCK, 8, 13, random);
        for (int i = 0; i < 16; i++) {
            int px = x + random.nextInt(-7, 8);
            int pz = z + random.nextInt(-7, 8);
            if (isAir(region, px, y, pz)) {
                set(region, px, y, pz,
                    random.nextBoolean() ? Material.FLOWERING_AZALEA : Material.PINK_PETALS);
            }
        }
    }

    private void placeLivingArch(LimitedRegion region,
                                 int x,
                                 int y,
                                 int z,
                                 SplittableRandom random) {
        boolean alongX = random.nextBoolean();
        int halfWidth = 5 + random.nextInt(3);
        int height = 11 + random.nextInt(5);

        for (int side : new int[]{-1, 1}) {
            int bx = x + (alongX ? side * halfWidth : 0);
            int bz = z + (alongX ? 0 : side * halfWidth);
            for (int dy = 0; dy <= height; dy++) {
                int lean = Math.max(0, dy - height / 2) / 4;
                int px = bx + (alongX ? -side * lean : 0);
                int pz = bz + (alongX ? 0 : -side * lean);
                placeDisc(region, px, y + dy, pz, dy < 4 ? 1 : 0, Material.DARK_OAK_LOG);
            }
        }

        for (int step = -halfWidth; step <= halfWidth; step++) {
            int ax = x + (alongX ? step : 0);
            int az = z + (alongX ? 0 : step);
            double normalized = Math.abs(step) / (double) halfWidth;
            int archY = y + height + (int) Math.round((1.0 - normalized * normalized) * 4.0);
            placeDisc(region, ax, archY, az, 1, Material.DARK_OAK_LOG);
            placeLeafCloud(region, ax, archY + 1, az, 2, Material.FLOWERING_AZALEA_LEAVES, random);
            if ((step & 1) == 0) {
                hangVines(region, ax, archY - 1, az, 1, 4, 10, random);
            }
        }

        placeRadialRoots(region, x - (alongX ? halfWidth : 0), y, z - (alongX ? 0 : halfWidth),
            Material.DARK_OAK_LOG, Material.MOSS_BLOCK, 4, 7, random);
        placeRadialRoots(region, x + (alongX ? halfWidth : 0), y, z + (alongX ? 0 : halfWidth),
            Material.DARK_OAK_LOG, Material.MOSS_BLOCK, 4, 7, random);
    }

    private void placeGiantFlower(LimitedRegion region,
                                  int x,
                                  int y,
                                  int z,
                                  SplittableRandom random) {
        int height = 12 + random.nextInt(7);
        for (int dy = 0; dy < height; dy++) {
            placeDisc(region, x, y + dy, z, dy < 4 ? 1 : 0, Material.BAMBOO_BLOCK);
        }

        int flowerY = y + height;
        set(region, x, flowerY, z, Material.SHROOMLIGHT);
        for (int[] direction : DIRECTIONS) {
            int reach = 3 + random.nextInt(3);
            for (int step = 1; step <= reach; step++) {
                int px = x + direction[0] * step;
                int pz = z + direction[1] * step;
                int py = flowerY + (step <= 2 ? 1 : 0);
                Material petal = step == reach ? Material.MAGENTA_WOOL : Material.PINK_WOOL;
                placeDisc(region, px, py, pz, step <= 2 ? 1 : 0, petal);
            }
        }
        placeLeafCloud(region, x, flowerY - 2, z, 3, Material.FLOWERING_AZALEA_LEAVES, random);
    }

    private void placeGloamFeature(LimitedRegion region,
                                   int x,
                                   int y,
                                   int z,
                                   SplittableRandom random) {
        int choice = random.nextInt(5);
        if (choice <= 2) {
            placeGloamMushroom(region, x, y, z, random);
        } else if (choice == 3) {
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
        int height = 20 + random.nextInt(13);
        int radius = 7 + random.nextInt(3);
        int stemRadius = height >= 27 ? 2 : 1;

        for (int dy = 0; dy < height; dy++) {
            int taper = dy > height * 0.78 ? Math.max(1, stemRadius - 1) : stemRadius;
            placeDisc(region, x, y + dy, z, taper, Material.MUSHROOM_STEM);
        }

        int capY = y + height;
        Material mainCap = random.nextBoolean() ? Material.RED_MUSHROOM_BLOCK : Material.BROWN_MUSHROOM_BLOCK;
        for (int dy = -3; dy <= 3; dy++) {
            int layerRadius = radius - Math.max(0, dy);
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    double distance = dx * dx + dz * dz;
                    double limit = layerRadius * layerRadius + 2.0;
                    boolean lowerRim = dy <= 0 && distance >= Math.max(0.0, limit - radius * 2.4);
                    boolean upperDome = dy > 0 && distance <= limit;
                    if ((lowerRim || upperDome) && random.nextDouble() > 0.05) {
                        Material cap = random.nextDouble() < 0.10
                            ? (mainCap == Material.RED_MUSHROOM_BLOCK
                                ? Material.BROWN_MUSHROOM_BLOCK : Material.RED_MUSHROOM_BLOCK)
                            : mainCap;
                        setIfAir(region, x + dx, capY + dy, z + dz, cap);
                    }
                }
            }
        }

        for (int i = 0; i < 14; i++) {
            double angle = (Math.PI * 2.0 * i) / 14.0;
            int gx = x + (int) Math.round(Math.cos(angle) * (radius - 2));
            int gz = z + (int) Math.round(Math.sin(angle) * (radius - 2));
            if ((i & 1) == 0) {
                setIfAir(region, gx, capY - 1, gz, Material.SHROOMLIGHT);
            } else if (random.nextBoolean()) {
                setIfAir(region, gx, capY - 1, gz, Material.SOUL_LANTERN);
            }
        }

        for (int i = 0; i < 12; i++) {
            int sx = x + random.nextInt(-8, 9);
            int sz = z + random.nextInt(-8, 9);
            if (isAir(region, sx, y, sz)) {
                set(region, sx, y, sz,
                    random.nextBoolean() ? Material.RED_MUSHROOM : Material.BROWN_MUSHROOM);
            }
        }
    }

    private void placeDeadSpiral(LimitedRegion region,
                                 int x,
                                 int y,
                                 int z,
                                 SplittableRandom random) {
        int height = 22 + random.nextInt(13);
        int px = x;
        int pz = z;
        for (int dy = 0; dy < height; dy++) {
            placeDisc(region, px, y + dy, pz, dy < height / 2 ? 1 : 0, Material.DARK_OAK_LOG);
            if (dy > 4 && dy % 4 == 0) {
                int direction = (dy / 4) & 3;
                px += direction == 0 ? 1 : direction == 2 ? -1 : 0;
                pz += direction == 1 ? 1 : direction == 3 ? -1 : 0;
            }
            if (dy > 8 && dy % 6 == 0) {
                int[] branch = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                placeBranch(region, px, y + dy, pz, branch[0], branch[1], 5 + random.nextInt(5), Material.DARK_OAK_LOG);
            }
        }

        placeLeafCloud(region, px, y + height - 2, pz, 5, Material.DARK_OAK_LEAVES, random);
        setIfAir(region, px, y + height + 2, pz, Material.SOUL_LANTERN);
        placeRadialRoots(region, x, y, z, Material.DARK_OAK_LOG, Material.SCULK, 6, 10, random);
    }

    private void placeGloamPool(LimitedRegion region,
                                int x,
                                int surfaceY,
                                int z,
                                SplittableRandom random) {
        int radius = 4 + random.nextInt(3);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius + 0.3) {
                    continue;
                }
                set(region, x + dx, surfaceY, z + dz,
                    distance > radius - 1.2 ? Material.SCULK : Material.DEEPSLATE);
                if (distance < radius - 1.6) {
                    set(region, x + dx, surfaceY + 1, z + dz, Material.WATER);
                } else if (random.nextDouble() < 0.24) {
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
        if (random.nextDouble() < 0.62) {
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
        int[][] offsets = {
            {0, 0}, {3, 1}, {-3, -1}, {2, -4}, {-2, 4}, {5, -3}, {-5, 3}, {4, 4}
        };
        for (int i = 0; i < offsets.length; i++) {
            int height = (i == 0 ? 17 : 7) + random.nextInt(i == 0 ? 14 : 8);
            int radius = i == 0 ? 2 : 1;
            int px = x + offsets[i][0];
            int pz = z + offsets[i][1];
            for (int dy = 0; dy < height; dy++) {
                int taper = dy > height * 0.72 ? Math.max(0, radius - 1) : radius;
                placeDisc(region, px, y + dy, pz, taper,
                    dy < 2 ? Material.CALCITE : Material.AMETHYST_BLOCK);
            }
            setIfAir(region, px, y + height, pz, Material.AMETHYST_CLUSTER);
        }
    }

    private void placeImpactScar(LimitedRegion region,
                                 int x,
                                 int surfaceY,
                                 int z,
                                 SplittableRandom random) {
        int radius = 5 + random.nextInt(3);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius + 0.3) {
                    continue;
                }
                Material material;
                if (distance < radius * 0.35) {
                    material = random.nextBoolean() ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                } else if (distance < radius * 0.70) {
                    material = Material.CALCITE;
                } else {
                    material = Material.TUFF;
                }
                set(region, x + dx, surfaceY, z + dz, material);
                if (distance < radius * 0.60 && random.nextDouble() < 0.42) {
                    setIfAir(region, x + dx, surfaceY + 1, z + dz, Material.AMETHYST_CLUSTER);
                }
            }
        }
        placeCrystalNeedles(region, x, surfaceY + 1, z, random);
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
            placeDisc(region, x + dx * step, py, z + dz * step, step < 3 ? 1 : 0, material);
        }
    }

    private void placeRadialRoots(LimitedRegion region,
                                  int x,
                                  int y,
                                  int z,
                                  Material trunk,
                                  Material root,
                                  int minLength,
                                  int maxLength,
                                  SplittableRandom random) {
        for (int[] direction : DIRECTIONS) {
            int length = minLength + random.nextInt(Math.max(1, maxLength - minLength + 1));
            int rx = x;
            int rz = z;
            int ry = y;
            for (int step = 1; step <= length; step++) {
                rx += direction[0];
                rz += direction[1];
                if ((step & 1) == 0) {
                    ry--;
                }
                set(region, rx, ry, rz, step <= 2 ? trunk : root);
            }
        }
    }

    private void hangVines(LimitedRegion region,
                           int x,
                           int y,
                           int z,
                           int count,
                           int minLength,
                           int maxLength,
                           SplittableRandom random) {
        for (int i = 0; i < count; i++) {
            int vx = x + random.nextInt(-4, 5);
            int vz = z + random.nextInt(-4, 5);
            int length = minLength + random.nextInt(Math.max(1, maxLength - minLength + 1));
            for (int d = 0; d < length; d++) {
                int vy = y - d;
                if (!isAir(region, vx, vy, vz)) {
                    if (d == 0) {
                        continue;
                    }
                    break;
                }
                set(region, vx, vy, vz, Material.VINE);
            }
        }
    }

    private void placeDisc(LimitedRegion region,
                           int x,
                           int y,
                           int z,
                           int radius,
                           Material material) {
        if (radius <= 0) {
            set(region, x, y, z, material);
            return;
        }
        double limit = radius * radius + 0.65;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= limit) {
                    set(region, x + dx, y, z + dz, material);
                }
            }
        }
    }

    private void placeLeafCloud(LimitedRegion region,
                                int x,
                                int y,
                                int z,
                                int radius,
                                Material leaves,
                                SplittableRandom random) {
        int vertical = Math.max(2, radius / 2 + 1);
        for (int dy = -vertical; dy <= vertical; dy++) {
            int layerRadius = Math.max(2, radius - Math.max(0, Math.abs(dy) - 1));
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    double distance = dx * dx + dz * dz + dy * dy * 1.7;
                    if (distance <= layerRadius * layerRadius + 2.0
                        && random.nextDouble() > 0.08) {
                        setIfAir(region, x + dx, y + dy, z + dz, leaves);
                    }
                }
            }
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
