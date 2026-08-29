package com.wickidcow.aetherlegacy.paper.world;

import java.util.Locale;

public enum FaePlane {
    REALM("realm", "Fae Realm", ""),
    WILDBLOOM("wildbloom", "Wildbloom", "_wildbloom"),
    GLOAM("gloam", "Gloam", "_gloam"),
    STARFALL("starfall", "Starfall", "_starfall");

    private final String id;
    private final String displayName;
    private final String worldSuffix;

    FaePlane(String id, String displayName, String worldSuffix) {
        this.id = id;
        this.displayName = displayName;
        this.worldSuffix = worldSuffix;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String defaultWorldName(String rootWorldName) {
        return rootWorldName + worldSuffix;
    }

    public FaeRegionProfile apply(FaeRegionProfile base) {
        return switch (this) {
            case REALM -> base;
            case WILDBLOOM -> new FaeRegionProfile(
                base.biome(), base.subregion(), FaeRegionProfile.Anomaly.WILDBLOOM,
                Math.max(base.magic(), 0.42), base.ruggedness() * 0.84);
            case GLOAM -> new FaeRegionProfile(
                base.biome(), base.subregion(), FaeRegionProfile.Anomaly.GLOAM,
                Math.min(base.magic(), -0.08), Math.max(base.ruggedness(), 0.58));
            case STARFALL -> new FaeRegionProfile(
                base.biome(), base.subregion(), FaeRegionProfile.Anomaly.STARFALL,
                Math.max(base.magic(), 0.34), Math.max(base.ruggedness(), 0.82));
        };
    }

    public static FaePlane parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("fae") || normalized.equals("main") || normalized.equals("fae_realm")) {
            return REALM;
        }
        for (FaePlane plane : values()) {
            if (plane.id.equals(normalized) || plane.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return plane;
            }
        }
        return null;
    }

    public static FaePlane fromWorldName(String worldName) {
        if (worldName == null) {
            return REALM;
        }
        String normalized = worldName.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(WILDBLOOM.worldSuffix)) {
            return WILDBLOOM;
        }
        if (normalized.endsWith(GLOAM.worldSuffix)) {
            return GLOAM;
        }
        if (normalized.endsWith(STARFALL.worldSuffix)) {
            return STARFALL;
        }
        return REALM;
    }
}
