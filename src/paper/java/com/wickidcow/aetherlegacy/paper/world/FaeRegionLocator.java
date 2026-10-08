package com.wickidcow.aetherlegacy.paper.world;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Pure deterministic locator for the Fae Realm region field.
 *
 * <p>This never loads or generates chunks. Returned coordinates identify a sample
 * point inside the requested region; the exact point may still be open sky because
 * terrain placement and region identity are intentionally independent.</p>
 */
public final class FaeRegionLocator {

    private static final int SAMPLE_STEP = 64;
    private static final List<String> REGION_NAMES = List.of(
        "golden_meadows",
        "crystal_woods",
        "mist_gardens",
        "ancient_fae_forest",
        "sky_highlands"
    );

    private FaeRegionLocator() {
    }

    public static @Nullable Result findNearest(long seed,
                                                int originX,
                                                int originZ,
                                                FaeRealmBiome target,
                                                int maxRadius) {
        Result origin = sample(seed, originX, originZ, originX, originZ, target);
        if (origin != null) {
            return origin;
        }

        int cappedRadius = Math.max(SAMPLE_STEP, maxRadius);
        for (int radius = SAMPLE_STEP; radius <= cappedRadius; radius += SAMPLE_STEP) {
            Result best = null;

            for (int offset = -radius; offset <= radius; offset += SAMPLE_STEP) {
                best = nearer(best, sample(seed,
                    originX + offset, originZ - radius, originX, originZ, target));
                best = nearer(best, sample(seed,
                    originX + offset, originZ + radius, originX, originZ, target));
            }

            for (int offset = -radius + SAMPLE_STEP;
                 offset <= radius - SAMPLE_STEP;
                 offset += SAMPLE_STEP) {
                best = nearer(best, sample(seed,
                    originX - radius, originZ + offset, originX, originZ, target));
                best = nearer(best, sample(seed,
                    originX + radius, originZ + offset, originX, originZ, target));
            }

            if (best != null) {
                return best;
            }
        }

        return null;
    }

    public static @Nullable FaeRealmBiome parseRegion(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String normalized = input.trim()
            .toLowerCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');

        return switch (normalized) {
            case "golden", "meadow", "meadows", "golden_meadow", "golden_meadows" ->
                FaeRealmBiome.GOLDEN_MEADOWS;
            case "crystal", "crystal_wood", "crystal_woods" ->
                FaeRealmBiome.CRYSTAL_WOODS;
            case "mist", "garden", "gardens", "mist_garden", "mist_gardens" ->
                FaeRealmBiome.MIST_GARDENS;
            case "ancient", "ancient_forest", "fae_forest", "ancient_fae_forest" ->
                FaeRealmBiome.ANCIENT_FAE_FOREST;
            case "sky", "highland", "highlands", "sky_highland", "sky_highlands" ->
                FaeRealmBiome.SKY_HIGHLANDS;
            default -> null;
        };
    }

    public static List<String> regionNames() {
        return REGION_NAMES;
    }

    private static @Nullable Result sample(long seed,
                                           int x,
                                           int z,
                                           int originX,
                                           int originZ,
                                           FaeRealmBiome target) {
        if (AetherChunkGenerator.biomeAt(seed, x, z) != target) {
            return null;
        }

        long dx = (long) x - originX;
        long dz = (long) z - originZ;
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        return new Result(target, x, z, distance);
    }

    private static @Nullable Result nearer(@Nullable Result current, @Nullable Result candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.distance() < current.distance()) {
            return candidate;
        }
        return current;
    }

    public record Result(FaeRealmBiome biome, int x, int z, int distance) {
    }
}
