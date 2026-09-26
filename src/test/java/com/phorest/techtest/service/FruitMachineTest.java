package com.phorest.techtest.service;

import com.phorest.techtest.enums.Colour;
import com.phorest.techtest.model.PlayOutcome;
import com.phorest.techtest.testUtils.FixedSequenceRandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class FruitMachineTest {

    @Test
    void spinReturnsFourSlots() {
        FruitMachine fruitMachine = new FruitMachine(RandomGenerator.getDefault());

        PlayOutcome outcome = fruitMachine.spin();

        assertThat(outcome.slots()).hasSize(4);
    }

    @ParameterizedTest
    @CsvSource({"BLACK", "WHITE", "GREEN", "YELLOW"})
    void spinIsAJackpotWhenAllSlotsMatch(Colour colour) {
        FruitMachine fruitMachine = new FruitMachine(new FixedSequenceRandomGenerator(colour, colour, colour, colour));

        PlayOutcome outcome = fruitMachine.spin();

        assertThat(outcome.jackpot()).isTrue();
        assertThat(outcome.slots()).containsExactly(colour, colour, colour, colour);
    }

    @Test
    void spinIsNotAJackpotWhenSlotsDiffer() {
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(Colour.BLACK, Colour.WHITE, Colour.BLACK, Colour.BLACK));

        PlayOutcome outcome = fruitMachine.spin();

        assertThat(outcome.jackpot()).isFalse();
        assertThat(outcome.slots()).containsExactly(Colour.BLACK, Colour.WHITE, Colour.BLACK, Colour.BLACK);
    }

    @Test
    void spinIsNotAJackpotWhenOnlyLastSlotDiffers() {
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(Colour.GREEN, Colour.GREEN, Colour.GREEN, Colour.WHITE));

        PlayOutcome outcome = fruitMachine.spin();

        assertThat(outcome.jackpot()).isFalse();
        assertThat(outcome.slots()).containsExactly(Colour.GREEN, Colour.GREEN, Colour.GREEN, Colour.WHITE);
    }
}
