package com.wickidcow.aetherlegacy.paper.world;

import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

/** Finds the generated Fae terrain surface beneath plants, trees and feature blocks. */
final class FaeSurfaceLocator {

    private FaeSurfaceLocator() {
    }

    static int find(WorldInfo info, LimitedRegion region, int x, int z) {
        FaeRealmBiome biome = AetherChunkGenerator.biomeAt(info.getSeed(), x, z);
        int highest = Math.min(info.getMaxHeight() - 2, region.getHighestBlockYAt(x, z));

        for (int y = highest; y >= info.getMinHeight(); y--) {
            if (region.isInRegion(x, y, z) && region.getType(x, y, z) == biome.surface()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
