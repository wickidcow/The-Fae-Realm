package com.wickidcow.aetherlegacy.paper.world;

/**
 * Deterministic local identity layered beneath the five public Fae regions.
 *
 * <p>The public {@link FaeRealmBiome} remains the compatibility-facing region used by
 * commands, resources and major structures. A subregion adds local terrain and ecology
 * character, while rare anomalies can cross region boundaries. This creates an
 * Iris-style region -> local identity -> decorator hierarchy without registering
 * client-side custom biomes or depending on an external generator.</p>
 */
public record FaeRegionProfile(
    FaeRealmBiome biome,
    Subregion subregion,
    Anomaly anomaly,
    double magic,
    double ruggedness
) {
    private static final long REGION_SALT = 0x510E527FADE682D1L;
    private static final long LOCAL_SALT = 0x9B05688C2B3E6C1FL;
    private static final long MAGIC_SALT = 0x1F83D9ABFB41BD6BL;
    private static final long RUGGED_SALT = 0x5BE0CD19137E2179L;
    private static final long ANOMALY_SALT = 0xA54FF53A5F1D36F1L;
    private static final long BLOOM_SALT = 0xC6EF372FE94F82BEL;

    public static FaeRegionProfile at(long seed, int worldX, int worldZ) {
        // Broad warped noise forms large coherent provinces instead of hard bands.
        double broad = FaeNoise.warpedFractal(
            seed ^ REGION_SALT,
            worldX * 0.00185,
            worldZ * 0.00185,
            3,
            2.0,
            0.52,
            0.85);
        double local = FaeNoise.warpedFractal(
            seed ^ LOCAL_SALT,
            worldX * 0.0048,
            worldZ * 0.0048,
            3,
            2.05,
            0.52,
            0.55);
        double magic = FaeNoise.fractal(
            seed ^ MAGIC_SALT,
            worldX * 0.0031,
            worldZ * 0.0031,
            3,
            2.0,
            0.52);
        double ruggedness = FaeNoise.ridged(
            seed ^ RUGGED_SALT,
            worldX * 0.0044,
            worldZ * 0.0044,
            3,
            2.0,
            0.50);

        FaeRealmBiome biome = chooseBiome(broad, local);
        Subregion subregion = chooseSubregion(biome, local, magic, ruggedness);

        double anomalyField = FaeNoise.warpedFractal(
            seed ^ ANOMALY_SALT,
            worldX * 0.00155,
            worldZ * 0.00155,
            3,
            2.0,
            0.54,
            1.10);
        double bloomField = FaeNoise.fractal(
            seed ^ BLOOM_SALT,
            worldX * 0.0036,
            worldZ * 0.0036,
            3,
            2.0,
            0.50);

        Anomaly anomaly = Anomaly.NONE;
        if (anomalyField > 0.60 && magic > 0.08) {
            anomaly = Anomaly.STARFALL;
        } else if (anomalyField < -0.60 && magic < 0.05) {
            anomaly = Anomaly.GLOAM;
        } else if (bloomField > 0.64 && magic > 0.16) {
            anomaly = Anomaly.WILDBLOOM;
        }

        return new FaeRegionProfile(biome, subregion, anomaly, magic, ruggedness);
    }

    private static FaeRealmBiome chooseBiome(double broad, double local) {
        if (broad < -0.43) {
            return FaeRealmBiome.MIST_GARDENS;
        }
        if (broad < -0.10) {
            return local > 0.16 ? FaeRealmBiome.CRYSTAL_WOODS : FaeRealmBiome.GOLDEN_MEADOWS;
        }
        if (broad < 0.34) {
            return local < -0.18 ? FaeRealmBiome.ANCIENT_FAE_FOREST : FaeRealmBiome.CRYSTAL_WOODS;
        }
        return local > 0.12 ? FaeRealmBiome.SKY_HIGHLANDS : FaeRealmBiome.ANCIENT_FAE_FOREST;
    }

    private static Subregion chooseSubregion(FaeRealmBiome biome,
                                             double local,
                                             double magic,
                                             double ruggedness) {
        return switch (biome) {
            case GOLDEN_MEADOWS -> local + (magic * 0.25) > -0.02
                ? Subregion.SUNLIT_GLADE
                : Subregion.AMBER_STEPPE;
            case CRYSTAL_WOODS -> magic + (ruggedness * 0.30) > 0.30
                ? Subregion.PRISMATIC_GROVE
                : Subregion.SHARDWOOD;
            case MIST_GARDENS -> magic > -0.02
                ? Subregion.MOON_MIST
                : Subregion.VEIL_MARSH;
            case ANCIENT_FAE_FOREST -> ruggedness > 0.48 || local > 0.12
                ? Subregion.ELDERWOOD
                : Subregion.MOSSBOUND_HOLLOWS;
            case SKY_HIGHLANDS -> ruggedness + (magic * 0.20) > 0.62
                ? Subregion.WINDCARVED_HEIGHTS
                : Subregion.SUNSPIRE;
        };
    }

    /** Multiplier applied to profile relief after the base island shape is calculated. */
    public double reliefMultiplier() {
        double region = switch (biome) {
            case GOLDEN_MEADOWS -> 0.78;
            case CRYSTAL_WOODS -> 1.06;
            case MIST_GARDENS -> 0.88;
            case ANCIENT_FAE_FOREST -> 1.02;
            case SKY_HIGHLANDS -> 1.28;
        };
        double local = switch (subregion) {
            case SUNLIT_GLADE -> 0.92;
            case AMBER_STEPPE -> 1.02;
            case PRISMATIC_GROVE -> 1.06;
            case SHARDWOOD -> 1.12;
            case MOON_MIST -> 0.92;
            case VEIL_MARSH -> 0.84;
            case ELDERWOOD -> 1.06;
            case MOSSBOUND_HOLLOWS -> 0.94;
            case SUNSPIRE -> 1.12;
            case WINDCARVED_HEIGHTS -> 1.22;
        };
        double anomalyScale = switch (anomaly) {
            case NONE -> 1.0;
            case STARFALL -> 1.18;
            case GLOAM -> 0.93;
            case WILDBLOOM -> 0.96;
        };
        return region * local * anomalyScale;
    }

    /** Positive values make caverns rarer; negative values make them more common. */
    public double caveThresholdOffset() {
        double offset = switch (biome) {
            case GOLDEN_MEADOWS -> 0.035;
            case CRYSTAL_WOODS -> -0.005;
            case MIST_GARDENS -> -0.040;
            case ANCIENT_FAE_FOREST -> -0.025;
            case SKY_HIGHLANDS -> 0.025;
        };
        if (subregion == Subregion.MOSSBOUND_HOLLOWS || subregion == Subregion.VEIL_MARSH) {
            offset -= 0.025;
        }
        if (anomaly == Anomaly.GLOAM) {
            offset -= 0.045;
        } else if (anomaly == Anomaly.STARFALL) {
            offset -= 0.010;
        }
        return offset;
    }

    public double vegetationMultiplier() {
        double multiplier = switch (subregion) {
            case SUNLIT_GLADE -> 0.85;
            case AMBER_STEPPE -> 0.55;
            case PRISMATIC_GROVE -> 1.05;
            case SHARDWOOD -> 0.92;
            case MOON_MIST -> 1.08;
            case VEIL_MARSH -> 0.88;
            case ELDERWOOD -> 1.22;
            case MOSSBOUND_HOLLOWS -> 1.08;
            case SUNSPIRE -> 0.72;
            case WINDCARVED_HEIGHTS -> 0.52;
        };
        if (anomaly == Anomaly.WILDBLOOM) {
            multiplier *= 1.35;
        } else if (anomaly == Anomaly.GLOAM) {
            multiplier *= 0.82;
        }
        return multiplier;
    }

    public double radiusMultiplier() {
        return switch (biome) {
            case GOLDEN_MEADOWS -> 1.10;
            case CRYSTAL_WOODS -> 1.00;
            case MIST_GARDENS -> 1.04;
            case ANCIENT_FAE_FOREST -> 1.08;
            case SKY_HIGHLANDS -> 0.92;
        };
    }

    public double thicknessMultiplier() {
        return switch (biome) {
            case GOLDEN_MEADOWS -> 0.92;
            case CRYSTAL_WOODS -> 1.02;
            case MIST_GARDENS -> 1.05;
            case ANCIENT_FAE_FOREST -> 1.12;
            case SKY_HIGHLANDS -> 1.08;
        };
    }

    public int heightOffset() {
        int offset = switch (biome) {
            case GOLDEN_MEADOWS -> -2;
            case CRYSTAL_WOODS -> 2;
            case MIST_GARDENS -> -6;
            case ANCIENT_FAE_FOREST -> 0;
            case SKY_HIGHLANDS -> 10;
        };
        if (anomaly == Anomaly.STARFALL) {
            offset += 8;
        } else if (anomaly == Anomaly.GLOAM) {
            offset -= 5;
        }
        return offset;
    }

    public double satelliteMultiplier() {
        double multiplier = switch (biome) {
            case GOLDEN_MEADOWS -> 0.86;
            case CRYSTAL_WOODS -> 1.08;
            case MIST_GARDENS -> 0.90;
            case ANCIENT_FAE_FOREST -> 1.00;
            case SKY_HIGHLANDS -> 1.24;
        };
        return anomaly == Anomaly.STARFALL ? multiplier * 1.25 : multiplier;
    }

    public double fractureStrength() {
        double strength = switch (subregion) {
            case PRISMATIC_GROVE -> 0.55;
            case SHARDWOOD -> 0.78;
            case WINDCARVED_HEIGHTS -> 0.70;
            case SUNSPIRE -> 0.45;
            default -> 0.18;
        };
        if (anomaly == Anomaly.STARFALL) {
            strength += 0.55;
        }
        return strength;
    }

    public enum Subregion {
        SUNLIT_GLADE,
        AMBER_STEPPE,
        PRISMATIC_GROVE,
        SHARDWOOD,
        MOON_MIST,
        VEIL_MARSH,
        ELDERWOOD,
        MOSSBOUND_HOLLOWS,
        SUNSPIRE,
        WINDCARVED_HEIGHTS
    }

    public enum Anomaly {
        NONE,
        STARFALL,
        GLOAM,
        WILDBLOOM
    }
}
