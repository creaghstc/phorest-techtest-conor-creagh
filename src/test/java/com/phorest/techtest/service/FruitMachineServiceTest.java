package com.phorest.techtest.service;

import com.phorest.techtest.domain.FruitMachine;
import com.phorest.techtest.enums.PrizeCategory;
import com.phorest.techtest.model.Colour;
import com.phorest.techtest.model.MachineState;
import com.phorest.techtest.model.PlayOutcome;
import com.phorest.techtest.testUtils.FixedSequenceRandomGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class FruitMachineServiceTest {

    private static final BigDecimal COST_PER_PLAY = new BigDecimal("2.00");

    private static final Colour BLACK = new Colour(0);
    private static final Colour WHITE = new Colour(1);

    private static FruitMachineService newService(
            RandomGenerator randomGenerator,
            BigDecimal initialBalance,
            BigDecimal costPerPlay,
            int slotCount,
            int colourCount,
            int smallPrizeRunLength) {
        FruitMachine fruitMachine = new FruitMachine(
                randomGenerator, initialBalance, costPerPlay, slotCount, colourCount, smallPrizeRunLength);
        return new FruitMachineService(randomGenerator, fruitMachine);
    }

    @Test
    void playDelegatesToTheUnderlyingMachine() {
        // given
        FruitMachineService service = newService(
                new FixedSequenceRandomGenerator(BLACK, BLACK, WHITE, WHITE),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                4,
                4,
                2);

        // when
        PlayOutcome outcome = service.play();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.SMALL_PRIZE);
        assertThat(outcome.balance()).isEqualByComparingTo("42.00");
    }

    @Test
    void stateReflectsTheCurrentConfigurationAndBalance() {
        // given
        FruitMachineService service = newService(
                RandomGenerator.getDefault(),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                4,
                4,
                2);

        // when
        MachineState state = service.state();

        // then
        assertThat(state.balance()).isEqualByComparingTo("50.00");
        assertThat(state.freePlays()).isZero();
        assertThat(state.slotCount()).isEqualTo(4);
        assertThat(state.colourCount()).isEqualTo(4);
        assertThat(state.smallPrizeRunLength()).isEqualTo(2);
        assertThat(state.costPerPlay()).isEqualByComparingTo(COST_PER_PLAY);
    }

    @Test
    void configureReplacesTheMachineAndResetsState() {
        // given
        FruitMachineService service = newService(
                RandomGenerator.getDefault(),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                4,
                4,
                2);
        service.play(); // move state away from the initial configuration

        // when
        MachineState state = service.configure(new BigDecimal("500.00"), new BigDecimal("5.00"), 6, 10, 3);

        // then
        assertThat(state.balance()).isEqualByComparingTo("500.00");
        assertThat(state.freePlays()).isZero();
        assertThat(state.slotCount()).isEqualTo(6);
        assertThat(state.colourCount()).isEqualTo(10);
        assertThat(state.smallPrizeRunLength()).isEqualTo(3);
        assertThat(state.costPerPlay()).isEqualByComparingTo("5.00");
        assertThat(service.state()).isEqualTo(state);
    }

    @Test
    void aFailedConfigureLeavesThePreviousMachineInPlace() {
        // given
        FruitMachineService service = newService(
                RandomGenerator.getDefault(),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                4,
                4,
                2);

        // when
        assertThatIllegalArgumentException().isThrownBy(() ->
                service.configure(new BigDecimal("50.00"), COST_PER_PLAY, 4, 4, 99));

        // then: the previous, still-valid machine remains in place
        MachineState state = service.state();
        assertThat(state.slotCount()).isEqualTo(4);
        assertThat(state.balance()).isEqualByComparingTo("50.00");
    }
}
