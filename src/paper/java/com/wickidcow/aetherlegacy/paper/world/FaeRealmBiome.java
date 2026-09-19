package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.Material;

/**
 * Lightweight server-side biome identities used by the Fae Realm generator.
 * These do not register client-side custom biomes; they control terrain palette,
 * vegetation and feature placement while remaining compatible with vanilla clients.
 */
public enum FaeRealmBiome {
    GOLDEN_MEADOWS(Material.GRASS_BLOCK, Material.DIRT, Material.STONE, Material.DANDELION),
    CRYSTAL_WOODS(Material.MOSS_BLOCK, Material.ROOTED_DIRT, Material.CALCITE, Material.AZALEA),
    MIST_GARDENS(Material.PALE_MOSS_BLOCK, Material.DIRT, Material.TUFF, Material.ALLIUM),
    ANCIENT_FAE_FOREST(Material.PODZOL, Material.DIRT, Material.STONE, Material.FERN),
    SKY_HIGHLANDS(Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.ANDESITE, Material.CORNFLOWER);

    private final Material surface;
    private final Material subsurface;
    private final Material core;
    private final Material accent;

    FaeRealmBiome(Material surface, Material subsurface, Material core, Material accent) {
        this.surface = surface;
        this.subsurface = subsurface;
        this.core = core;
        this.accent = accent;
    }

    public Material surface() {
        return surface;
    }

    public Material subsurface() {
        return subsurface;
    }

    public Material core() {
        return core;
    }

    public Material accent() {
        return accent;
    }
}
