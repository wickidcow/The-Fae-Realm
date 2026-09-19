package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Maps Fae Realm generation regions to vanilla client-visible biome data. */
public final class FaeBiomeProvider extends BiomeProvider {

    private static final List<Biome> BIOMES = List.of(
        Biome.SUNFLOWER_PLAINS,
        Biome.CHERRY_GROVE,
        Biome.PALE_GARDEN,
        Biome.OLD_GROWTH_SPRUCE_TAIGA,
        Biome.WINDSWEPT_FOREST
    );

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        return switch (AetherChunkGenerator.biomeAt(worldInfo.getSeed(), x, z)) {
            case GOLDEN_MEADOWS -> Biome.SUNFLOWER_PLAINS;
            case CRYSTAL_WOODS -> Biome.CHERRY_GROVE;
            case MIST_GARDENS -> Biome.PALE_GARDEN;
            case ANCIENT_FAE_FOREST -> Biome.OLD_GROWTH_SPRUCE_TAIGA;
            case SKY_HIGHLANDS -> Biome.WINDSWEPT_FOREST;
        };
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return BIOMES;
    }
}
