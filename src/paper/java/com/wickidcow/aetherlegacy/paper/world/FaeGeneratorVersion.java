package com.wickidcow.aetherlegacy.paper.world;

/**
 * Code-owned generator schema revision.
 *
 * <p>This must be bumped whenever deterministic terrain/feature placement changes
 * in a way that can create seams between old and newly explored chunks. It is not
 * a user setting and intentionally does not come from config.yml.</p>
 */
public final class FaeGeneratorVersion {

    public static final int CURRENT = 8;

    private FaeGeneratorVersion() {
    }
}
