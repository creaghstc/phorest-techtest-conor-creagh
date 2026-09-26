package com.phorest.techtest.service;

import com.phorest.techtest.enums.Colour;
import com.phorest.techtest.model.PlayOutcome;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.random.RandomGenerator;

@Service
public class FruitMachine {

    private static final int SLOT_COUNT = 4;

    private final RandomGenerator randomGenerator;

    public FruitMachine(RandomGenerator randomGenerator) {
        this.randomGenerator = randomGenerator;
    }

    public PlayOutcome spin() {
        Colour[] colours = Colour.values();

        List<Colour> slots = randomGenerator.ints(SLOT_COUNT, 0, colours.length)
                .mapToObj(index -> colours[index])
                .toList();

        boolean jackpot = slots.stream().distinct().count() == 1;

        return new PlayOutcome(slots, jackpot);
    }
}
