package com.phorest.techtest.testUtils;

import com.phorest.techtest.model.Colour;

import java.util.random.RandomGenerator;

/**
 * Stub {@link RandomGenerator} for tests: returns the given colours' ids, in order, from
 * {@link #nextInt(int, int)} — the requested origin/bound are ignored.
 */
public class FixedSequenceRandomGenerator implements RandomGenerator {

    private final Colour[] colours;
    private int position = 0;

    public FixedSequenceRandomGenerator(Colour... colours) {
        this.colours = colours;
    }

    @Override
    public int nextInt(int origin, int bound) {
        return colours[position++].id();
    }

    @Override
    public long nextLong() {
        throw new UnsupportedOperationException("not used by FruitMachine");
    }
}
