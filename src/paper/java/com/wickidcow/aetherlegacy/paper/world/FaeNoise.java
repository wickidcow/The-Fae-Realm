package com.wickidcow.aetherlegacy.paper.world;

/**
 * Small deterministic value-noise helper. It intentionally avoids external
 * libraries so the generator remains lightweight and reproducible from only
 * the world seed and coordinates.
 */
final class FaeNoise {

    private FaeNoise() {
    }

    static double fractal(long seed, double x, double z, int octaves, double lacunarity, double persistence) {
        double amplitude = 1.0;
        double frequency = 1.0;
        double total = 0.0;
        double normalization = 0.0;

        for (int i = 0; i < octaves; i++) {
            total += value(seed + (i * 0x9E3779B97F4A7C15L), x * frequency, z * frequency) * amplitude;
            normalization += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }

        return normalization == 0.0 ? 0.0 : total / normalization;
    }

    static double value(long seed, double x, double z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double fx = smooth(x - x0);
        double fz = smooth(z - z0);

        double a = lattice(seed, x0, z0);
        double b = lattice(seed, x1, z0);
        double c = lattice(seed, x0, z1);
        double d = lattice(seed, x1, z1);

        return lerp(lerp(a, b, fx), lerp(c, d, fx), fz);
    }

    private static double lattice(long seed, int x, int z) {
        long mixed = seed;
        mixed ^= (long) x * 0x632BE59BD9B4E019L;
        mixed ^= (long) z * 0x9E3779B185EBCA87L;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return ((mixed >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    private static int fastFloor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
