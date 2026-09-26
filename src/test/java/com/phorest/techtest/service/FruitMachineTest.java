package com.phorest.techtest.service;

import com.phorest.techtest.enums.PrizeCategory;
import com.phorest.techtest.model.Colour;
import com.phorest.techtest.model.PlayOutcome;
import com.phorest.techtest.testUtils.FixedSequenceRandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class FruitMachineTest {

    private static final BigDecimal COST_PER_PLAY = new BigDecimal("2.00");

    private static final int DEFAULT_SLOT_COUNT = 4;
    private static final int DEFAULT_COLOUR_COUNT = 4;
    private static final int DEFAULT_SMALL_PRIZE_RUN_LENGTH = 2;

    private static final Colour BLACK = new Colour(0);
    private static final Colour WHITE = new Colour(1);
    private static final Colour GREEN = new Colour(2);
    private static final Colour YELLOW = new Colour(3);

    @Test
    void spinReturnsAColourPerSlot() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("100.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.slots()).hasSize(DEFAULT_SLOT_COUNT);
    }

    @Test
    void spinReturnsASlotPerConfiguredSlotCountEvenWhenNotFour() {
        // given
        int slotCount = 7;
        FruitMachine fruitMachine = new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("100.00"),
                COST_PER_PLAY,
                slotCount,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.slots()).hasSize(slotCount);
    }

    @Test
    void chargesTheCostOfPlayBeforeSpinning() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(BLACK, WHITE, BLACK, WHITE),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.balance()).isEqualByComparingTo("52.00");
    }

    @ParameterizedTest
    @CsvSource({"0", "1", "2", "3"})
    void jackpotPaysOutTheEntireBalance(int colourId) {
        // given
        Colour colour = new Colour(colourId);
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(colour, colour, colour, colour),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

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
                new FixedSequenceRandomGenerator(BLACK, WHITE, GREEN, YELLOW),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

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
                new FixedSequenceRandomGenerator(BLACK, WHITE, GREEN, YELLOW),
                new BigDecimal("51.01"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.FULL_HOUSE);
        assertThat(outcome.amountWon()).isEqualByComparingTo("26.51");
        assertThat(outcome.balance()).isEqualByComparingTo("26.50");
    }

    @Test
    void fullHouseGeneralisesToAllSlotsShowingDifferentColoursEvenWithFarMoreColoursThanSlots() {
        // given: 3 slots, but the machine has 100 possible colours - none of which repeat here
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(new Colour(5), new Colour(42), new Colour(99)),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                3,
                100,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.FULL_HOUSE);
    }

    @Test
    void smallPrizePaysOutFiveTimesTheCostOfAPlayWhenTheBalanceCanAffordIt() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(BLACK, BLACK, WHITE, GREEN),
                new BigDecimal("100.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.SMALL_PRIZE);
        assertThat(outcome.amountWon()).isEqualByComparingTo("10.00");
        assertThat(outcome.freePlaysAwarded()).isZero();
        assertThat(outcome.balance()).isEqualByComparingTo("92.00");
    }

    @Test
    void smallPrizeStaysAFlatPrizeWhenThereAreTwoSeparateQualifyingRuns() {
        // given: BLACK,BLACK and WHITE,WHITE are two separate runs of 2, both meeting k = 2
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(BLACK, BLACK, WHITE, WHITE, GREEN),
                new BigDecimal("100.00"),
                COST_PER_PLAY,
                5,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then: still only the single flat small prize, not doubled for the second run
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.SMALL_PRIZE);
        assertThat(outcome.amountWon()).isEqualByComparingTo("10.00");
    }

    @Test
    void smallPrizeStaysAFlatPrizeWhenTheRunIsLongerThanK() {
        // given: a run of 3 comfortably exceeds k = 2, but the prize doesn't scale with run length
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(BLACK, BLACK, BLACK, WHITE),
                new BigDecimal("100.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then: same flat amount as a run of exactly 2 (see smallPrizePaysOutFiveTimesTheCostOfAPlayWhenTheBalanceCanAffordIt)
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.SMALL_PRIZE);
        assertThat(outcome.amountWon()).isEqualByComparingTo("10.00");
    }

    @Test
    void smallPrizeRunLengthOfThreeIsNotSatisfiedByARunOfTwo() {
        // given: k = 3, but the longest run in these slots is only 2
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(BLACK, BLACK, WHITE, GREEN, YELLOW),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                5,
                DEFAULT_COLOUR_COUNT,
                3);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.NONE);
    }

    @Test
    void smallPrizeRunLengthOfThreeIsSatisfiedByARunOfThreeOrMore() {
        // given: k = 3, and the slots contain a run of exactly 3
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(BLACK, BLACK, BLACK, WHITE, GREEN),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                5,
                DEFAULT_COLOUR_COUNT,
                3);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.SMALL_PRIZE);
    }

    @Test
    void noPrizeWhenSlotsDontMatchAnyRule() {
        // given
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(BLACK, WHITE, BLACK, WHITE),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

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
                new FixedSequenceRandomGenerator(BLACK, BLACK, WHITE, GREEN),
                new BigDecimal("0.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

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
                        BLACK, BLACK, WHITE, GREEN,
                        BLACK, WHITE, BLACK, WHITE),
                new BigDecimal("0.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

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

    @Test
    void rejectsASmallPrizeRunLengthBelowTwo() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("100.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                1));
    }

    @Test
    void rejectsASmallPrizeRunLengthLongerThanTheSlotCount() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("100.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SLOT_COUNT + 1));
    }

    @Test
    void rejectsASlotCountBelowOne() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("100.00"),
                COST_PER_PLAY,
                0,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH));
    }

    @Test
    void rejectsAColourCountBelowOne() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("100.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                0,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH));
    }

    @Test
    void rejectsANegativeInitialBalance() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("-0.01"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH));
    }

    @Test
    void rejectsAZeroCostPerPlay() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH));
    }

    @Test
    void rejectsANegativeCostPerPlay() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("100.00"),
                new BigDecimal("-1.00"),
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH));
    }

    @Test
    void smallPrizeIsUnreachableWhenRunLengthEqualsSlotCount() {
        // given: k == slotCount, so the only run long enough to qualify is "all slots the same" -
        // which is already claimed by jackpot
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(BLACK, BLACK, BLACK, BLACK),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SLOT_COUNT);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then: jackpot takes priority, small prize can never actually be won with this configuration
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.JACKPOT);
    }

    @Test
    void shortfallSmallerThanOnePlaysCostRoundsDownToZeroFreePlays() {
        // given: prize owed is 10.00, only 9.00 is available, leaving a 1.00 shortfall - less than
        // one play's 2.00 cost
        FruitMachine fruitMachine = new FruitMachine(
                new FixedSequenceRandomGenerator(BLACK, BLACK, WHITE, GREEN),
                new BigDecimal("7.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.SMALL_PRIZE);
        assertThat(outcome.amountWon()).isEqualByComparingTo("9.00");
        assertThat(outcome.freePlaysAwarded()).isZero();
        assertThat(outcome.balance()).isEqualByComparingTo("0.00");
    }

    @Test
    void aSingleColourMachineAlwaysPaysOutTheJackpot() {
        // given: only one possible colour, so every slot is guaranteed to match
        FruitMachine fruitMachine = new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("50.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                1,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

        // when
        PlayOutcome outcome = fruitMachine.spin();

        // then
        assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.JACKPOT);
    }

    @Test
    void aSingleSlotMachineCanNeverBeConfiguredValidly() {
        // given/when/then: the minimum small prize run length (2) can never fit within 1 slot
        assertThatIllegalArgumentException().isThrownBy(() -> new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("100.00"),
                COST_PER_PLAY,
                1,
                DEFAULT_COLOUR_COUNT,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH));
    }

    @Test
    void concurrentSpinsDoNotCorruptTheSharedBalance() throws InterruptedException {
        // given: colourCount = 1 guarantees every spin is a jackpot, which charges the cost then
        // immediately pays out the entire balance - so every single spin, regardless of how many
        // others run concurrently, should independently end with amountWon == costPerPlay and
        // balance == 0.00. A lost update on the shared balance would show up as a spin reporting
        // some other amount instead.
        int threadCount = 20;
        int spinsPerThread = 50;
        FruitMachine fruitMachine = new FruitMachine(
                RandomGenerator.getDefault(),
                new BigDecimal("0.00"),
                COST_PER_PLAY,
                DEFAULT_SLOT_COUNT,
                1,
                DEFAULT_SMALL_PRIZE_RUN_LENGTH);

        Queue<PlayOutcome> outcomes = new ConcurrentLinkedQueue<>();
        List<Callable<Void>> tasks = IntStream.range(0, threadCount)
                .<Callable<Void>>mapToObj(threadIndex -> () -> {
                    for (int i = 0; i < spinsPerThread; i++) {
                        outcomes.add(fruitMachine.spin());
                    }
                    return null;
                })
                .toList();

        // when
        List<Future<Void>> futures;
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            futures = executor.invokeAll(tasks);
        }

        // then
        for (Future<Void> future : futures) {
            assertThatCode(future::get).doesNotThrowAnyException();
        }
        assertThat(outcomes).hasSize(threadCount * spinsPerThread);
        assertThat(outcomes).allSatisfy(outcome -> {
            assertThat(outcome.prizeCategory()).isEqualTo(PrizeCategory.JACKPOT);
            assertThat(outcome.amountWon()).isEqualByComparingTo(COST_PER_PLAY);
            assertThat(outcome.balance()).isEqualByComparingTo("0.00");
        });
    }
}
