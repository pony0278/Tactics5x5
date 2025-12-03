package com.tactics.engine.util;

import java.util.Random;

/**
 * Provides deterministic randomness for game features.
 * Uses a seeded Random for reproducible results in replays.
 */
public class RngProvider {

    private final Random random;

    /**
     * Create with default seed (for testing/development).
     */
    public RngProvider() {
        this(System.currentTimeMillis());
    }

    /**
     * Create with specific seed for deterministic replays.
     */
    public RngProvider(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Get next random int in range [0, bound).
     */
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    /**
     * Get next random boolean.
     */
    public boolean nextBoolean() {
        return random.nextBoolean();
    }
}
