package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

/**
 * Deterministic weighted micro-landmark layer for ordinary Fae exploration.
 *
 * <p>Each subregion owns a small feature pool, while rare realm anomalies can inject
 * their own landmarks across normal region boundaries. This keeps ordinary chunks
 * varied without consuming the spacing budget reserved for major structures.</p>
 */
public final class FaeFeaturePopulator {

    private static final long FEATURE_SALT = 0xA4093822299F31D0L;

    public void populate(WorldInfo info,
                         Random chunkRandom,
                         int chunkX,
                         int chunkZ,
                         LimitedRegion region,
                         double density) {
        SplittableRandom random = new SplittableRandom(
            mixSeed(info.getSeed() ^ FEATURE_SALT, chunkX, chunkZ));

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        FaeRegionProfile chunkProfile = AetherChunkGenerator.regionProfileAt(
            info.getSeed(), baseX + 8, baseZ + 8);

        // At density 1.0, roughly one in six chunks gets a recognizable micro-feature.
        // Anomaly pockets are slightly more eventful so they read as special places.
        double anomalyMultiplier = chunkProfile.anomaly() == FaeRegionProfile.Anomaly.NONE ? 1.0 : 1.35;
        double featureChance = Math.min(1.0, Math.max(0.0, density * anomalyMultiplier / 6.0));
        if (random.nextDouble() >= featureChance) {
            return;
        }

        int x = baseX + 4 + random.nextInt(8);
        int z = baseZ + 4 + random.nextInt(8);
        int y = region.getHighestBlockYAt(x, z);
        if (y < info.getMinHeight() || y + 10 >= info.getMaxHeight()) {
            return;
        }

        FaeRegionProfile profile = AetherChunkGenerator.regionProfileAt(info.getSeed(), x, z);
        FaeRealmBiome biome = profile.biome();
        if (region.getType(x, y, z) != biome.surface() || !stable(region, x, y, z, 3)) {
            return;
        }

        FeatureKind feature = chooseFeature(profile, random);
        switch (feature) {
            case FLOWER_CIRCLE -> placeFlowerCircle(region, x, y + 1, z);
            case SUN_POOL -> placeSunPool(region, x, y, z);
            case FAIRY_RING -> placeFairyRing(region, x, y + 1, z);
            case CRYSTAL_SPIRE -> placeCrystalSpire(region, x, y + 1, z, random);
            case SHARD_GARDEN -> placeShardGarden(region, x, y + 1, z, random);
            case MIST_MUSHROOMS -> placeMistMushrooms(region, x, y + 1, z, random);
            case MOON_WELL -> placeMoonWell(region, x, y, z);
            case FALLEN_ANCIENT -> placeFallenAncient(region, x, y + 1, z, random);
            case ROOT_SHRINE -> placeRootShrine(region, x, y + 1, z);
            case STANDING_STONES -> placeStandingStones(region, x, y + 1, z, random);
            case WIND_ALTAR -> placeWindAltar(region, x, y + 1, z);
            case STARFALL_SCAR -> placeStarfallScar(region, x, y, z, random);
            case GLOAM_CAIRN -> placeGloamCairn(region, x, y + 1, z);
            case WILDBLOOM_CIRCLE -> placeWildbloomCircle(region, x, y + 1, z);
        }
    }

    private FeatureKind chooseFeature(FaeRegionProfile profile, SplittableRandom random) {
        if (profile.anomaly() != FaeRegionProfile.Anomaly.NONE && random.nextDouble() < 0.46) {
            return switch (profile.anomaly()) {
                case STARFALL -> FeatureKind.STARFALL_SCAR;
                case GLOAM -> FeatureKind.GLOAM_CAIRN;
                case WILDBLOOM -> FeatureKind.WILDBLOOM_CIRCLE;
                case NONE -> throw new IllegalStateException("NONE anomaly was handled before feature selection");
            };
        }

        List<WeightedFeature> pool = switch (profile.subregion()) {
            case SUNLIT_GLADE -> List.of(
                weighted(FeatureKind.FLOWER_CIRCLE, 5),
                weighted(FeatureKind.SUN_POOL, 3),
                weighted(FeatureKind.FAIRY_RING, 4));
            case AMBER_STEPPE -> List.of(
                weighted(FeatureKind.SUN_POOL, 6),
                weighted(FeatureKind.FLOWER_CIRCLE, 3),
                weighted(FeatureKind.WIND_ALTAR, 2));
            case PRISMATIC_GROVE -> List.of(
                weighted(FeatureKind.CRYSTAL_SPIRE, 6),
                weighted(FeatureKind.SHARD_GARDEN, 6),
                weighted(FeatureKind.FAIRY_RING, 2));
            case SHARDWOOD -> List.of(
                weighted(FeatureKind.SHARD_GARDEN, 7),
                weighted(FeatureKind.CRYSTAL_SPIRE, 4),
                weighted(FeatureKind.ROOT_SHRINE, 2));
            case MOON_MIST -> List.of(
                weighted(FeatureKind.MOON_WELL, 6),
                weighted(FeatureKind.MIST_MUSHROOMS, 5),
                weighted(FeatureKind.FAIRY_RING, 2));
            case VEIL_MARSH -> List.of(
                weighted(FeatureKind.MIST_MUSHROOMS, 7),
                weighted(FeatureKind.MOON_WELL, 5),
                weighted(FeatureKind.GLOAM_CAIRN, 1));
            case ELDERWOOD -> List.of(
                weighted(FeatureKind.FALLEN_ANCIENT, 6),
                weighted(FeatureKind.ROOT_SHRINE, 6),
                weighted(FeatureKind.FAIRY_RING, 1));
            case MOSSBOUND_HOLLOWS -> List.of(
                weighted(FeatureKind.ROOT_SHRINE, 7),
                weighted(FeatureKind.FALLEN_ANCIENT, 5),
                weighted(FeatureKind.MIST_MUSHROOMS, 2));
            case SUNSPIRE -> List.of(
                weighted(FeatureKind.WIND_ALTAR, 7),
                weighted(FeatureKind.STANDING_STONES, 5),
                weighted(FeatureKind.CRYSTAL_SPIRE, 1));
            case WINDCARVED_HEIGHTS -> List.of(
                weighted(FeatureKind.STANDING_STONES, 7),
                weighted(FeatureKind.WIND_ALTAR, 4),
                weighted(FeatureKind.CRYSTAL_SPIRE, 2));
        };

        int totalWeight = pool.stream().mapToInt(WeightedFeature::weight).sum();
        int roll = random.nextInt(totalWeight);
        for (WeightedFeature entry : pool) {
            if (roll < entry.weight()) {
                return entry.feature();
            }
            roll -= entry.weight();
        }
        return pool.getFirst().feature();
    }

    private WeightedFeature weighted(FeatureKind feature, int weight) {
        return new WeightedFeature(feature, weight);
    }

    private void placeFlowerCircle(LimitedRegion region, int x, int y, int z) {
        int[][] ring = {
            {-3, 0}, {-2, -2}, {0, -3}, {2, -2}, {3, 0},
            {2, 2}, {0, 3}, {-2, 2}
        };
        Material[] flowers = {
            Material.DANDELION,
            Material.CORNFLOWER,
            Material.OXEYE_DAISY,
            Material.ALLIUM
        };
        for (int i = 0; i < ring.length; i++) {
            int px = x + ring[i][0];
            int pz = z + ring[i][1];
            if (region.isInRegion(px, y, pz) && region.getType(px, y, pz).isAir()) {
                region.setType(px, y, pz, flowers[i % flowers.length]);
            }
        }
        set(region, x, y - 1, z, Material.GLOWSTONE);
        set(region, x, y, z, Material.LIGHT);
    }

    private void placeSunPool(LimitedRegion region, int x, int surfaceY, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 2.25) {
                    continue;
                }
                set(region, x + dx, surfaceY, z + dz,
                    Math.abs(dx) <= 1 && Math.abs(dz) <= 1 ? Material.WATER : Material.SMOOTH_SANDSTONE);
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    set(region, x + dx, surfaceY - 1, z + dz, Material.GLOWSTONE);
                }
            }
        }
    }

    private void placeFairyRing(LimitedRegion region, int x, int y, int z) {
        int[][] ring = {
            {-3, 0}, {-2, -2}, {0, -3}, {2, -2}, {3, 0},
            {2, 2}, {0, 3}, {-2, 2}
        };
        for (int i = 0; i < ring.length; i++) {
            Material material = (i & 1) == 0 ? Material.RED_MUSHROOM : Material.BROWN_MUSHROOM;
            int px = x + ring[i][0];
            int pz = z + ring[i][1];
            if (region.isInRegion(px, y, pz) && region.getType(px, y, pz).isAir()) {
                region.setType(px, y, pz, material);
            }
        }
        set(region, x, y - 1, z, Material.MOSS_BLOCK);
        set(region, x, y, z, Material.LIGHT);
    }

    private void placeCrystalSpire(LimitedRegion region,
                                   int x,
                                   int y,
                                   int z,
                                   SplittableRandom random) {
        int height = 4 + random.nextInt(5);
        for (int dy = 0; dy < height; dy++) {
            Material material = dy < 2
                ? Material.CALCITE
                : (dy == height - 1 ? Material.AMETHYST_CLUSTER : Material.AMETHYST_BLOCK);
            set(region, x, y + dy, z, material);
        }
        set(region, x - 1, y, z, Material.BUDDING_AMETHYST);
        set(region, x + 1, y, z, Material.AMETHYST_BLOCK);
        set(region, x, y, z - 1, Material.CALCITE);
        set(region, x, y, z + 1, Material.CALCITE);
    }

    private void placeShardGarden(LimitedRegion region,
                                  int x,
                                  int y,
                                  int z,
                                  SplittableRandom random) {
        int[][] shards = {{0, 0}, {-2, 1}, {2, -1}, {-1, -2}, {2, 2}};
        for (int i = 0; i < shards.length; i++) {
            int height = 2 + random.nextInt(i == 0 ? 5 : 3);
            int px = x + shards[i][0];
            int pz = z + shards[i][1];
            for (int dy = 0; dy < height; dy++) {
                set(region, px, y + dy, pz, dy == 0 ? Material.CALCITE : Material.AMETHYST_BLOCK);
            }
            set(region, px, y + height, pz, Material.AMETHYST_CLUSTER);
        }
    }

    private void placeMistMushrooms(LimitedRegion region,
                                    int x,
                                    int y,
                                    int z,
                                    SplittableRandom random) {
        placeSmallMushroom(region, x, y, z, Material.RED_MUSHROOM_BLOCK, 3 + random.nextInt(2));
        placeSmallMushroom(region, x - 3, y, z + 2, Material.BROWN_MUSHROOM_BLOCK, 2 + random.nextInt(2));
        placeSmallMushroom(region, x + 3, y, z - 2, Material.RED_MUSHROOM_BLOCK, 2 + random.nextInt(2));
        set(region, x - 1, y, z + 3, Material.PALE_MOSS_CARPET);
        set(region, x + 2, y, z + 2, Material.PALE_MOSS_CARPET);
    }

    private void placeSmallMushroom(LimitedRegion region,
                                    int x,
                                    int y,
                                    int z,
                                    Material cap,
                                    int height) {
        for (int dy = 0; dy < height; dy++) {
            set(region, x, y + dy, z, Material.MUSHROOM_STEM);
        }
        int capY = y + height;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(region, x + dx, capY, z + dz, cap);
            }
        }
        set(region, x, capY + 1, z, cap);
    }

    private void placeMoonWell(LimitedRegion region, int x, int surfaceY, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 2.35) {
                    continue;
                }
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    set(region, x + dx, surfaceY - 1, z + dz, Material.SEA_LANTERN);
                    set(region, x + dx, surfaceY, z + dz, Material.WATER);
                } else {
                    set(region, x + dx, surfaceY, z + dz, Material.POLISHED_TUFF);
                }
            }
        }
        set(region, x - 2, surfaceY + 1, z, Material.SOUL_LANTERN);
        set(region, x + 2, surfaceY + 1, z, Material.SOUL_LANTERN);
    }

    private void placeFallenAncient(LimitedRegion region,
                                    int x,
                                    int y,
                                    int z,
                                    SplittableRandom random) {
        boolean alongX = random.nextBoolean();
        for (int offset = -4; offset <= 4; offset++) {
            int px = alongX ? x + offset : x;
            int pz = alongX ? z : z + offset;
            set(region, px, y, pz, Material.DARK_OAK_LOG);
            if ((offset & 1) == 0) {
                set(region, px, y + 1, pz, Material.MOSS_CARPET);
            }
        }
        set(region, x - 2, y, z - 2, Material.FERN);
        set(region, x + 2, y, z + 2, Material.LARGE_FERN);
    }

    private void placeRootShrine(LimitedRegion region, int x, int y, int z) {
        for (int dy = 0; dy <= 4; dy++) {
            set(region, x - 2, y + dy, z, Material.DARK_OAK_LOG);
            set(region, x + 2, y + dy, z, Material.DARK_OAK_LOG);
        }
        for (int dx = -2; dx <= 2; dx++) {
            set(region, x + dx, y + 4, z, Material.DARK_OAK_LOG);
        }
        set(region, x, y, z, Material.MOSS_BLOCK);
        set(region, x, y + 1, z, Material.SOUL_LANTERN);
        set(region, x - 2, y + 5, z, Material.AZALEA_LEAVES);
        set(region, x + 2, y + 5, z, Material.FLOWERING_AZALEA_LEAVES);
    }

    private void placeStandingStones(LimitedRegion region,
                                     int x,
                                     int y,
                                     int z,
                                     SplittableRandom random) {
        int[][] stones = {{-3, 0}, {3, 0}, {0, -3}, {0, 3}};
        for (int i = 0; i < stones.length; i++) {
            int height = 3 + random.nextInt(4);
            Material material = (i & 1) == 0 ? Material.POLISHED_ANDESITE : Material.CALCITE;
            for (int dy = 0; dy < height; dy++) {
                set(region, x + stones[i][0], y + dy, z + stones[i][1], material);
            }
            set(region, x + stones[i][0], y + height, z + stones[i][1], Material.SEA_LANTERN);
        }
        set(region, x, y, z, Material.LODESTONE);
    }

    private void placeWindAltar(LimitedRegion region, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 3) {
                    set(region, x + dx, y - 1, z + dz, Material.POLISHED_ANDESITE);
                }
            }
        }
        for (int dy = 0; dy < 3; dy++) {
            set(region, x, y + dy, z, Material.QUARTZ_PILLAR);
        }
        set(region, x, y + 3, z, Material.LIGHTNING_ROD);
        set(region, x - 2, y, z, Material.SEA_LANTERN);
        set(region, x + 2, y, z, Material.SEA_LANTERN);
        set(region, x, y, z - 2, Material.SEA_LANTERN);
        set(region, x, y, z + 2, Material.SEA_LANTERN);
    }

    private void placeStarfallScar(LimitedRegion region,
                                   int x,
                                   int surfaceY,
                                   int z,
                                   SplittableRandom random) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 3.15) {
                    continue;
                }
                Material material;
                if (distance < 1.35) {
                    material = random.nextBoolean() ? Material.CRYING_OBSIDIAN : Material.AMETHYST_BLOCK;
                } else if (distance < 2.35) {
                    material = Material.CALCITE;
                } else {
                    material = Material.END_STONE;
                }
                set(region, x + dx, surfaceY, z + dz, material);
            }
        }
        set(region, x, surfaceY + 1, z, Material.AMETHYST_CLUSTER);
        set(region, x - 1, surfaceY + 1, z + 1, Material.END_ROD);
        set(region, x + 1, surfaceY + 1, z - 1, Material.END_ROD);
    }

    private void placeGloamCairn(LimitedRegion region, int x, int y, int z) {
        set(region, x, y, z, Material.POLISHED_DEEPSLATE);
        set(region, x, y + 1, z, Material.DEEPSLATE_BRICKS);
        set(region, x, y + 2, z, Material.CRYING_OBSIDIAN);
        set(region, x, y + 3, z, Material.SOUL_LANTERN);
        set(region, x - 1, y, z, Material.POLISHED_BASALT);
        set(region, x + 1, y, z, Material.POLISHED_BASALT);
        set(region, x, y, z - 1, Material.POLISHED_BASALT);
        set(region, x, y, z + 1, Material.POLISHED_BASALT);
        set(region, x - 2, y, z + 1, Material.WITHER_ROSE);
        set(region, x + 2, y, z - 1, Material.WITHER_ROSE);
    }

    private void placeWildbloomCircle(LimitedRegion region, int x, int y, int z) {
        Material[] flowers = {
            Material.PINK_TULIP,
            Material.ALLIUM,
            Material.CORNFLOWER,
            Material.OXEYE_DAISY,
            Material.TORCHFLOWER,
            Material.ORANGE_TULIP
        };
        int[][] offsets = {
            {-3, 0}, {-2, -2}, {0, -3}, {2, -2}, {3, 0}, {2, 2}, {0, 3}, {-2, 2},
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
        };
        for (int i = 0; i < offsets.length; i++) {
            int px = x + offsets[i][0];
            int pz = z + offsets[i][1];
            if (region.isInRegion(px, y, pz) && region.getType(px, y, pz).isAir()) {
                region.setType(px, y, pz, flowers[i % flowers.length]);
            }
        }
        set(region, x, y - 1, z, Material.FLOWERING_AZALEA_LEAVES);
        set(region, x, y, z, Material.LIGHT);
    }

    private boolean stable(LimitedRegion region, int x, int y, int z, int radius) {
        int[][] samples = {
            {0, 0}, {-radius, 0}, {radius, 0}, {0, -radius}, {0, radius},
            {-radius, -radius}, {-radius, radius}, {radius, -radius}, {radius, radius}
        };
        for (int[] sample : samples) {
            int sx = x + sample[0];
            int sz = z + sample[1];
            if (!region.isInRegion(sx, y, sz)) {
                return false;
            }
            int sy = region.getHighestBlockYAt(sx, sz);
            if (Math.abs(sy - y) > 2) {
                return false;
            }
        }
        return true;
    }

    private void set(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) {
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

    private enum FeatureKind {
        FLOWER_CIRCLE,
        SUN_POOL,
        FAIRY_RING,
        CRYSTAL_SPIRE,
        SHARD_GARDEN,
        MIST_MUSHROOMS,
        MOON_WELL,
        FALLEN_ANCIENT,
        ROOT_SHRINE,
        STANDING_STONES,
        WIND_ALTAR,
        STARFALL_SCAR,
        GLOAM_CAIRN,
        WILDBLOOM_CIRCLE
    }

    private record WeightedFeature(FeatureKind feature, int weight) {
    }
}
