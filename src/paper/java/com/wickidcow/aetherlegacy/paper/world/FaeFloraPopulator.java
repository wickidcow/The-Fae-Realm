package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.SplittableRandom;

/**
 * Dense deterministic ecology for every Fae plane.
 *
 * <p>The Fae Realm is intentionally vegetation-first: floating terrain is the
 * foundation, while oversized trees, giant fungi, roots, vines and layered ground
 * growth provide the dominant silhouette. Everything is built from vanilla-backed
 * blocks and LimitedRegion-safe primitives so no client mod is required.</p>
 */
public final class FaeFloraPopulator {

    private static final long FLORA_SALT = 0x243F6A8885A308D3L;
    private static final long TREE_SALT = 0x13198A2E03707344L;
    private static final long MEGA_TREE_SALT = 0xA4093822299F31D0L;
    private static final long MUSHROOM_SALT = 0x082EFA98EC4E6C89L;

    private static final int[][] CARDINALS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };
    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };

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
        int groundAttempts = scaledAttempts(58, density, floraRandom);
        for (int attempt = 0; attempt < groundAttempts; attempt++) {
            int x = baseX + floraRandom.nextInt(16);
            int z = baseZ + floraRandom.nextInt(16);
            FaeRegionProfile profile = AetherChunkGenerator.regionProfileAt(info.getSeed(), x, z);
            int surfaceY = findFaeSurface(info, region, x, z, profile.biome());
            if (surfaceY == Integer.MIN_VALUE) {
                continue;
            }

            double vegetation = Math.max(0.35, profile.vegetationMultiplier());
            if (floraRandom.nextDouble() <= Math.min(1.0, 0.92 * vegetation)) {
                placeGroundPatch(info, region, x, surfaceY, z, profile, floraRandom);
            }
            if (floraRandom.nextDouble() < Math.min(0.20, 0.055 * density * vegetation)) {
                placeEdgeCurtain(region, x, surfaceY, z, floraRandom);
            }
        }

        SplittableRandom treeRandom = new SplittableRandom(
            mixSeed(info.getSeed() ^ TREE_SALT, chunkX, chunkZ));
        int treeAttempts = scaledAttempts(3, density, treeRandom);
        for (int attempt = 0; attempt < treeAttempts; attempt++) {
            int x = baseX + 3 + treeRandom.nextInt(10);
            int z = baseZ + 3 + treeRandom.nextInt(10);
            FaeRegionProfile profile = AetherChunkGenerator.regionProfileAt(info.getSeed(), x, z);
            int surfaceY = findFaeSurface(info, region, x, z, profile.biome());
            if (surfaceY == Integer.MIN_VALUE || surfaceY + 30 >= info.getMaxHeight()) {
                continue;
            }

            double chance = treeChance(profile) * Math.min(1.45, Math.max(0.40, density));
            if (treeRandom.nextDouble() < Math.min(0.98, chance)) {
                placeFaeTree(region, x, surfaceY + 1, z, profile, treeRandom);
            }
        }

        SplittableRandom megaRandom = new SplittableRandom(
            mixSeed(info.getSeed() ^ MEGA_TREE_SALT, chunkX, chunkZ));
        int megaX = baseX + 5 + megaRandom.nextInt(6);
        int megaZ = baseZ + 5 + megaRandom.nextInt(6);
        FaeRegionProfile megaProfile = AetherChunkGenerator.regionProfileAt(info.getSeed(), megaX, megaZ);
        int megaSurface = findFaeSurface(info, region, megaX, megaZ, megaProfile.biome());
        if (megaSurface != Integer.MIN_VALUE
            && megaSurface + 72 < info.getMaxHeight()
            && megaRandom.nextDouble() < Math.min(0.58, megaTreeChance(megaProfile) * Math.max(0.45, density))) {
            placeMegaTree(region, megaX, megaSurface + 1, megaZ, megaProfile, megaRandom);
        }

        SplittableRandom mushroomRandom = new SplittableRandom(
            mixSeed(info.getSeed() ^ MUSHROOM_SALT, chunkX, chunkZ));
        int mushroomX = baseX + 4 + mushroomRandom.nextInt(8);
        int mushroomZ = baseZ + 4 + mushroomRandom.nextInt(8);
        FaeRegionProfile mushroomProfile = AetherChunkGenerator.regionProfileAt(info.getSeed(), mushroomX, mushroomZ);
        int mushroomSurface = findFaeSurface(info, region, mushroomX, mushroomZ, mushroomProfile.biome());
        if (mushroomSurface != Integer.MIN_VALUE
            && mushroomSurface + 34 < info.getMaxHeight()
            && mushroomRandom.nextDouble() < Math.min(0.62, giantMushroomChance(mushroomProfile) * Math.max(0.45, density))) {
            placeGiantMushroom(region, mushroomX, mushroomSurface + 1, mushroomZ, mushroomProfile, mushroomRandom);
        }
    }

    private int findFaeSurface(WorldInfo info,
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

    private void placeGroundPatch(WorldInfo info,
                                  LimitedRegion region,
                                  int x,
                                  int surfaceY,
                                  int z,
                                  FaeRegionProfile profile,
                                  SplittableRandom random) {
        placeGroundGrowth(region, x, surfaceY + 1, z, profile, random);

        int satellites = 2 + random.nextInt(5);
        for (int i = 0; i < satellites; i++) {
            int px = x + random.nextInt(-2, 3);
            int pz = z + random.nextInt(-2, 3);
            int py = findFaeSurface(info, region, px, pz, profile.biome());
            if (py != Integer.MIN_VALUE && random.nextDouble() < 0.76) {
                placeGroundGrowth(region, px, py + 1, pz, profile, random);
            }
        }
    }

    private void placeGroundGrowth(LimitedRegion region,
                                   int x,
                                   int y,
                                   int z,
                                   FaeRegionProfile profile,
                                   SplittableRandom random) {
        if (!isAir(region, x, y, z)) {
            return;
        }

        Material growth = switch (profile.biome()) {
            case GOLDEN_MEADOWS -> pick(random,
                Material.SHORT_GRASS, Material.SHORT_GRASS,
                Material.DANDELION, Material.POPPY, Material.CORNFLOWER,
                Material.OXEYE_DAISY, Material.AZURE_BLUET);
            case CRYSTAL_WOODS -> pick(random,
                Material.MOSS_CARPET, Material.MOSS_CARPET,
                Material.AZALEA, Material.FLOWERING_AZALEA,
                Material.PINK_PETALS, Material.ALLIUM);
            case MIST_GARDENS -> pick(random,
                Material.PALE_MOSS_CARPET, Material.PALE_MOSS_CARPET,
                Material.ALLIUM, Material.BLUE_ORCHID,
                Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.FERN);
            case ANCIENT_FAE_FOREST -> pick(random,
                Material.FERN, Material.FERN, Material.MOSS_CARPET,
                Material.BROWN_MUSHROOM, Material.RED_MUSHROOM,
                Material.AZALEA, Material.FLOWERING_AZALEA);
            case SKY_HIGHLANDS -> pick(random,
                Material.SHORT_GRASS, Material.FERN,
                Material.CORNFLOWER, Material.OXEYE_DAISY,
                Material.AZURE_BLUET, Material.MOSS_CARPET);
        };

        if (profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM && random.nextDouble() < 0.22) {
            growth = random.nextBoolean() ? Material.WITHER_ROSE : Material.RED_MUSHROOM;
        } else if (profile.anomaly() == FaeRegionProfile.Anomaly.WILDBLOOM && random.nextDouble() < 0.34) {
            growth = random.nextBoolean() ? Material.PINK_PETALS : Material.FLOWERING_AZALEA;
        } else if (profile.anomaly() == FaeRegionProfile.Anomaly.STARFALL && random.nextDouble() < 0.12) {
            growth = Material.AMETHYST_CLUSTER;
        }

        region.setType(x, y, z, growth);
    }

    private void placeEdgeCurtain(LimitedRegion region,
                                  int x,
                                  int surfaceY,
                                  int z,
                                  SplittableRandom random) {
        int[] direction = CARDINALS[random.nextInt(CARDINALS.length)];
        int vx = x + direction[0];
        int vz = z + direction[1];
        if (!isAir(region, vx, surfaceY, vz) || !isAir(region, vx, surfaceY + 1, vz)) {
            return;
        }

        int length = 4 + random.nextInt(10);
        for (int i = 0; i < length; i++) {
            int y = surfaceY - i;
            if (!isAir(region, vx, y, vz)) {
                break;
            }
            region.setType(vx, y, vz, Material.VINE);
        }
    }

    private void placeFaeTree(LimitedRegion region,
                              int baseX,
                              int baseY,
                              int baseZ,
                              FaeRegionProfile profile,
                              SplittableRandom random) {
        TreePalette palette = palette(profile);
        int height = switch (profile.biome()) {
            case ANCIENT_FAE_FOREST -> 18 + random.nextInt(9);
            case CRYSTAL_WOODS -> 16 + random.nextInt(9);
            case MIST_GARDENS -> 14 + random.nextInt(8);
            case SKY_HIGHLANDS -> 14 + random.nextInt(8);
            case GOLDEN_MEADOWS -> 15 + random.nextInt(8);
        };

        int x = baseX;
        int z = baseZ;
        int trunkRadius = height >= 22 ? 1 : 0;
        for (int dy = 0; dy < height; dy++) {
            int y = baseY + dy;
            placeTrunkDisc(region, x, y, z, trunkRadius, palette.log());
            if (dy > 3 && dy < height - 5 && dy % 5 == 0 && random.nextDouble() < 0.72) {
                int[] drift = CARDINALS[random.nextInt(CARDINALS.length)];
                x += drift[0];
                z += drift[1];
            }
        }

        int crownY = baseY + height - 2;
        placeLeafCloud(region, x, crownY, z, palette, 5, random);

        int branches = 5 + random.nextInt(3);
        for (int branch = 0; branch < branches; branch++) {
            int[] direction = DIRECTIONS[(branch + random.nextInt(DIRECTIONS.length)) % DIRECTIONS.length];
            int branchY = crownY - 4 - random.nextInt(7);
            int length = 4 + random.nextInt(5);
            int bx = x;
            int bz = z;
            int by = branchY;
            for (int step = 0; step < length; step++) {
                bx += direction[0];
                bz += direction[1];
                if ((step & 1) == 1) {
                    by++;
                }
                placeTrunkDisc(region, bx, by, bz, step < 2 ? 1 : 0, palette.log());
            }
            placeLeafCloud(region, bx, by + 1, bz, palette, 3 + random.nextInt(2), random);
            hangTendrils(region, bx, by, bz, palette, 2 + random.nextInt(3), 4, 10, random);
        }

        placeRoots(region, baseX, baseY, baseZ, palette, 4, 7, random);

        if (profile.biome() == FaeRealmBiome.CRYSTAL_WOODS
            || profile.anomaly() == FaeRegionProfile.Anomaly.STARFALL) {
            placeCrystalTips(region, x, crownY + 2, z, random);
        }
    }

    private void placeMegaTree(LimitedRegion region,
                               int baseX,
                               int baseY,
                               int baseZ,
                               FaeRegionProfile profile,
                               SplittableRandom random) {
        TreePalette palette = palette(profile);
        int height = switch (profile.biome()) {
            case ANCIENT_FAE_FOREST -> 46 + random.nextInt(17);
            case CRYSTAL_WOODS -> 40 + random.nextInt(15);
            case MIST_GARDENS -> 36 + random.nextInt(14);
            case SKY_HIGHLANDS -> 38 + random.nextInt(14);
            case GOLDEN_MEADOWS -> 38 + random.nextInt(15);
        };
        if (profile.anomaly() == FaeRegionProfile.Anomaly.WILDBLOOM) {
            height += 6 + random.nextInt(7);
        }

        int x = baseX;
        int z = baseZ;
        int trunkRadius = height >= 54 ? 3 : 2;
        for (int dy = 0; dy < height; dy++) {
            int y = baseY + dy;
            int taper = dy > height * 0.72 ? Math.max(1, trunkRadius - 1) : trunkRadius;
            placeTrunkDisc(region, x, y, z, taper, palette.log());
            if (dy > 8 && dy < height - 10 && dy % 9 == 0 && random.nextDouble() < 0.58) {
                int[] drift = CARDINALS[random.nextInt(CARDINALS.length)];
                x += drift[0];
                z += drift[1];
            }
        }

        int crownY = baseY + height - 3;
        placeLeafCloud(region, x, crownY, z, palette, 8, random);
        placeLeafCloud(region, x + 3, crownY - 2, z - 2, palette, 6, random);
        placeLeafCloud(region, x - 3, crownY - 1, z + 2, palette, 6, random);

        int branches = 8 + random.nextInt(3);
        for (int branch = 0; branch < branches; branch++) {
            int[] direction = DIRECTIONS[branch % DIRECTIONS.length];
            int by = crownY - 7 - random.nextInt(13);
            int bx = x;
            int bz = z;
            int length = 7 + random.nextInt(6);
            for (int step = 0; step < length; step++) {
                bx += direction[0];
                bz += direction[1];
                by += step % 3 == 2 ? 1 : 0;
                placeTrunkDisc(region, bx, by, bz, step < 4 ? 1 : 0, palette.log());
            }
            placeLeafCloud(region, bx, by + 1, bz, palette, 4 + random.nextInt(2), random);
            hangTendrils(region, bx, by, bz, palette, 3 + random.nextInt(4), 7, 16, random);
        }

        placeRoots(region, baseX, baseY, baseZ, palette, 8, 13, random);
        for (int i = 0; i < 8; i++) {
            int ox = baseX + random.nextInt(-5, 6);
            int oz = baseZ + random.nextInt(-5, 6);
            hangTendrils(region, ox, crownY - random.nextInt(8), oz, palette, 1, 8, 18, random);
        }

        set(region, x, crownY, z, palette.glow());
        if (profile.biome() == FaeRealmBiome.CRYSTAL_WOODS
            || profile.anomaly() == FaeRegionProfile.Anomaly.STARFALL) {
            placeCrystalTips(region, x, crownY + 4, z, random);
        }
    }

    private void placeGiantMushroom(LimitedRegion region,
                                    int x,
                                    int y,
                                    int z,
                                    FaeRegionProfile profile,
                                    SplittableRandom random) {
        int height = 12 + random.nextInt(13);
        int radius = 5 + random.nextInt(3);
        if (profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM) {
            height += 4 + random.nextInt(6);
            radius++;
        }

        int stemRadius = height >= 20 ? 1 : 0;
        for (int dy = 0; dy < height; dy++) {
            placeTrunkDisc(region, x, y + dy, z, stemRadius, Material.MUSHROOM_STEM);
        }

        Material cap = profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM
            ? Material.BROWN_MUSHROOM_BLOCK
            : (random.nextBoolean() ? Material.RED_MUSHROOM_BLOCK : Material.BROWN_MUSHROOM_BLOCK);
        int capY = y + height;
        for (int dy = -2; dy <= 2; dy++) {
            int layerRadius = radius - Math.max(0, dy + 1);
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    double distance = dx * dx + dz * dz;
                    double limit = layerRadius * layerRadius + 1.5;
                    boolean rimLayer = dy <= 0 && distance >= Math.max(0.0, limit - (radius * 2.2));
                    boolean domeLayer = dy > 0 && distance <= limit;
                    if ((rimLayer || domeLayer) && random.nextDouble() > 0.07) {
                        setIfAir(region, x + dx, capY + dy, z + dz, cap);
                    }
                }
            }
        }

        for (int[] direction : DIRECTIONS) {
            if (random.nextDouble() < 0.55) {
                int gx = x + direction[0] * Math.max(2, radius - 2);
                int gz = z + direction[1] * Math.max(2, radius - 2);
                setIfAir(region, gx, capY - 1, gz,
                    profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM ? Material.SOUL_LANTERN : Material.SHROOMLIGHT);
            }
        }

        int satellites = 1 + random.nextInt(3);
        for (int i = 0; i < satellites; i++) {
            int sx = x + random.nextInt(-5, 6);
            int sz = z + random.nextInt(-5, 6);
            setIfAir(region, sx, y, sz, random.nextBoolean() ? Material.RED_MUSHROOM : Material.BROWN_MUSHROOM);
        }
    }

    private void placeTrunkDisc(LimitedRegion region,
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
                                TreePalette palette,
                                int radius,
                                SplittableRandom random) {
        int vertical = Math.max(3, radius / 2 + 1);
        for (int dy = -vertical; dy <= vertical; dy++) {
            int layerRadius = Math.max(2, radius - Math.max(0, Math.abs(dy) - 1));
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    double distance = dx * dx + dz * dz + dy * dy * 1.7;
                    if (distance > layerRadius * layerRadius + 2.0) {
                        continue;
                    }
                    if (random.nextDouble() < 0.10 && Math.abs(dx) + Math.abs(dz) > layerRadius) {
                        continue;
                    }
                    setIfAir(region, x + dx, y + dy, z + dz, palette.leaves());
                }
            }
        }
        if (random.nextDouble() < 0.78) {
            set(region, x, y, z, palette.glow());
        }
    }

    private void hangTendrils(LimitedRegion region,
                              int x,
                              int y,
                              int z,
                              TreePalette palette,
                              int count,
                              int minLength,
                              int maxLength,
                              SplittableRandom random) {
        for (int n = 0; n < count; n++) {
            int tx = x + random.nextInt(-4, 5);
            int tz = z + random.nextInt(-4, 5);
            int length = minLength + random.nextInt(Math.max(1, maxLength - minLength + 1));
            for (int i = 0; i < length; i++) {
                int ty = y - i;
                if (!isAir(region, tx, ty, tz)) {
                    if (i == 0) {
                        continue;
                    }
                    break;
                }
                region.setType(tx, ty, tz, palette.tendril());
            }
        }
    }

    private void placeRoots(LimitedRegion region,
                            int x,
                            int y,
                            int z,
                            TreePalette palette,
                            int minLength,
                            int maxLength,
                            SplittableRandom random) {
        for (int[] direction : DIRECTIONS) {
            if (random.nextDouble() > 0.88) {
                continue;
            }
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
                set(region, rx, ry, rz, step <= 2 ? palette.log() : palette.root());
            }
        }
    }

    private void placeCrystalTips(LimitedRegion region,
                                  int x,
                                  int y,
                                  int z,
                                  SplittableRandom random) {
        int[][] offsets = {{0, 3}, {3, 0}, {0, -3}, {-3, 0}, {0, 0}};
        for (int[] offset : offsets) {
            if (random.nextDouble() < 0.60) {
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
            case GOLDEN_MEADOWS -> 0.56;
            case CRYSTAL_WOODS -> 0.82;
            case MIST_GARDENS -> 0.76;
            case ANCIENT_FAE_FOREST -> 0.94;
            case SKY_HIGHLANDS -> 0.58;
        };
        return Math.max(0.16, Math.min(0.98, base * profile.vegetationMultiplier()));
    }

    private double megaTreeChance(FaeRegionProfile profile) {
        double base = switch (profile.biome()) {
            case GOLDEN_MEADOWS -> 0.08;
            case CRYSTAL_WOODS -> 0.18;
            case MIST_GARDENS -> 0.15;
            case ANCIENT_FAE_FOREST -> 0.30;
            case SKY_HIGHLANDS -> 0.10;
        };
        if (profile.anomaly() == FaeRegionProfile.Anomaly.WILDBLOOM) {
            base += 0.16;
        }
        return Math.max(0.04, Math.min(0.52, base * profile.vegetationMultiplier()));
    }

    private double giantMushroomChance(FaeRegionProfile profile) {
        double base = switch (profile.biome()) {
            case GOLDEN_MEADOWS -> 0.05;
            case CRYSTAL_WOODS -> 0.10;
            case MIST_GARDENS -> 0.28;
            case ANCIENT_FAE_FOREST -> 0.20;
            case SKY_HIGHLANDS -> 0.06;
        };
        if (profile.anomaly() == FaeRegionProfile.Anomaly.GLOAM) {
            base += 0.28;
        } else if (profile.anomaly() == FaeRegionProfile.Anomaly.WILDBLOOM) {
            base += 0.08;
        }
        return Math.max(0.03, Math.min(0.56, base * profile.vegetationMultiplier()));
    }

    private int scaledAttempts(int baseAttempts, double density, SplittableRandom random) {
        double expected = Math.max(0.0, baseAttempts * density);
        int whole = (int) Math.floor(expected);
        return whole + (random.nextDouble() < expected - whole ? 1 : 0);
    }

    private Material pick(SplittableRandom random, Material... values) {
        return values[random.nextInt(values.length)];
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

    private record TreePalette(
        Material log,
        Material leaves,
        Material root,
        Material glow,
        Material tendril
    ) {
    }
}
