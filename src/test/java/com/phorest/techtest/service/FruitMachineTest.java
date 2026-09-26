package com.phorest.techtest.service;

import com.phorest.techtest.enums.Colour;
import com.phorest.techtest.enums.PrizeCategory;
import com.phorest.techtest.model.PlayOutcome;
import com.phorest.techtest.testUtils.FixedSequenceRandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class FruitMachineTest {

    private static final BigDecimal COST_PER_PLAY = new BigDecimal("2.00");

    @Test
    void spinReturnsFourSlots() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                RandomGenerator.getDefault(), new BigDecimal("100.00"), COST_PER_PLAY);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.slots()).hasSize(4);
    }

    @Test
    void chargesTheCostOfPlayBeforeSpinning() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(Colour.BLACK, Colour.WHITE, Colour.BLACK, Colour.WHITE),
                new BigDecimal("50.00"),
                COST_PER_PLAY);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.balance()).isEqualByComparingTo("52.00");
    }

    @ParameterizedTest
    @CsvSource({"BLACK", "WHITE", "GREEN", "YELLOW"})
    void jackpotPaysOutTheEntireBalance(Colour colour) {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(colour, colour, colour, colour),
                new BigDecimal("50.00"),
                COST_PER_PLAY);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.JACKPOT);
        assertThat(outcome.amountWon()).isEqualByComparingTo("52.00");
        assertThat(outcome.freePlaysAwarded()).isZero();
        assertThat(outcome.balance()).isEqualByComparingTo("0.00");
    }

    @Test
    void fullHousePaysOutHalfTheBalance() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(Colour.BLACK, Colour.WHITE, Colour.GREEN, Colour.YELLOW),
                new BigDecimal("50.00"),
                COST_PER_PLAY);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.FULL_HOUSE);
        assertThat(outcome.amountWon()).isEqualByComparingTo("26.00");
        assertThat(outcome.freePlaysAwarded()).isZero();
        assertThat(outcome.balance()).isEqualByComparingTo("26.00");
    }

    @Test
    void fullHousePayoutIsRoundedToTheNearestCent() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(Colour.BLACK, Colour.WHITE, Colour.GREEN, Colour.YELLOW),
                new BigDecimal("51.01"),
                COST_PER_PLAY);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.FULL_HOUSE);
        assertThat(outcome.amountWon()).isEqualByComparingTo("26.51");
        assertThat(outcome.balance()).isEqualByComparingTo("26.50");
    }

    @Test
    void smallPrizePaysOutFiveTimesTheCostOfAPlayWhenTheBalanceCanAffordIt() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(Colour.BLACK, Colour.BLACK, Colour.WHITE, Colour.GREEN),
                new BigDecimal("100.00"),
                COST_PER_PLAY);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.SMALL_PRIZE);
        assertThat(outcome.amountWon()).isEqualByComparingTo("10.00");
        assertThat(outcome.freePlaysAwarded()).isZero();
        assertThat(outcome.balance()).isEqualByComparingTo("92.00");
    }

    @Test
    void noPrizeWhenSlotsDontMatchAnyRule() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(Colour.BLACK, Colour.WHITE, Colour.BLACK, Colour.WHITE),
                new BigDecimal("50.00"),
                COST_PER_PLAY);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.NONE);
        assertThat(outcome.amountWon()).isEqualByComparingTo("0.00");
        assertThat(outcome.freePlaysAwarded()).isZero();
        assertThat(outcome.balance()).isEqualByComparingTo("52.00");
    }

    @Test
    void shortfallOnAPrizeIsCreditedAsFreePlaysRoundedDown() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(Colour.BLACK, Colour.BLACK, Colour.WHITE, Colour.GREEN),
                new BigDecimal("0.00"),
                COST_PER_PLAY);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.SMALL_PRIZE);
        assertThat(outcome.amountWon()).isEqualByComparingTo("2.00");
        assertThat(outcome.freePlaysAwarded()).isEqualTo(4);
        assertThat(outcome.balance()).isEqualByComparingTo("0.00");
    }

    @Test
    void aBankedFreePlayIsConsumedInsteadOfChargingForTheNextPlay() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(
                        Colour.BLACK, Colour.BLACK, Colour.WHITE, Colour.GREEN,
                        Colour.BLACK, Colour.WHITE, Colour.BLACK, Colour.WHITE),
                new BigDecimal("0.00"),
                COST_PER_PLAY);

        // when
        PlayOutcome firstOutcome = fruitMachine.spin();

        // then
        assertThat(firstOutcome.freePlaysAwarded()).isEqualTo(4);
        assertThat(firstOutcome.balance()).isEqualByComparingTo("0.00");

        // when
        PlayOutcome secondOutcome = fruitMachine.spin();

        // then
        assertThat(secondOutcome.prizeCategory()).isEqualTo(PrizeCategory.NONE);
        assertThat(secondOutcome.balance()).isEqualByComparingTo("0.00");
    }
}
