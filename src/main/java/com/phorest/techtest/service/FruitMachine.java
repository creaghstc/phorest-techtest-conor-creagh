package com.phorest.techtest.service;

import com.phorest.techtest.enums.PrizeCategory;
import com.phorest.techtest.model.Colour;
import com.phorest.techtest.model.PlayOutcome;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

public class FruitMachine {

    private static final BigDecimal SMALL_PRIZE_MULTIPLIER = BigDecimal.valueOf(5);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final int MONEY_SCALE = 2;
    private static final int MINIMUM_SMALL_PRIZE_RUN_LENGTH = 2;

    private final RandomGenerator randomGenerator;
    private final BigDecimal costPerPlay;
    private final int slotCount;
    private final int colourCount;
    private final int smallPrizeRunLength;

    private BigDecimal balance;
    private int freePlays;

    public FruitMachine(
            RandomGenerator randomGenerator,
            BigDecimal initialBalance,
            BigDecimal costPerPlay,
            int slotCount,
            int colourCount,
            int smallPrizeRunLength) {
        validateConfiguration(initialBalance, costPerPlay, slotCount, colourCount, smallPrizeRunLength);

        this.randomGenerator = randomGenerator;
        this.balance = initialBalance;
        this.costPerPlay = costPerPlay;
        this.slotCount = slotCount;
        this.colourCount = colourCount;
        this.smallPrizeRunLength = smallPrizeRunLength;
        this.freePlays = 0;
    }

    private static void validateConfiguration(
            BigDecimal initialBalance,
            BigDecimal costPerPlay,
            int slotCount,
            int colourCount,
            int smallPrizeRunLength) {
        if (initialBalance.signum() < 0) {
            throw new IllegalArgumentException("initialBalance cannot be negative");
        }
        if (costPerPlay.signum() <= 0) {
            throw new IllegalArgumentException("costPerPlay must be positive");
        }
        if (slotCount < 1) {
            throw new IllegalArgumentException("slotCount must be at least 1");
        }
        if (colourCount < 1) {
            throw new IllegalArgumentException("colourCount must be at least 1");
        }
        if (smallPrizeRunLength < MINIMUM_SMALL_PRIZE_RUN_LENGTH) {
            throw new IllegalArgumentException(
                    "smallPrizeRunLength must be at least " + MINIMUM_SMALL_PRIZE_RUN_LENGTH);
        }
        if (smallPrizeRunLength > slotCount) {
            throw new IllegalArgumentException("smallPrizeRunLength cannot exceed slotCount");
        }
    }

    /**
     * FruitMachine is a shared, mutable singleton (balance/freePlays) - synchronized prevents
     * concurrent requests from interleaving reads and writes of that state and losing updates.
     **/
    public synchronized PlayOutcome spin() {
        chargeForPlay();

        List<Colour> slots = spinSlots();
        PrizeCategory prizeCategory = determinePrizeCategory(slots);

        BigDecimal prizeOwed = prizeOwed(prizeCategory);
        BigDecimal payout = prizeOwed.min(this.balance);
        BigDecimal shortfall = prizeOwed.subtract(payout);

        int freePlaysAwarded = 0;
        // Jackpot does not qualify for free plays
        if (prizeCategory != PrizeCategory.JACKPOT && shortfall.signum() > 0) {
            freePlaysAwarded = shortfall.divideToIntegralValue(this.costPerPlay).intValue();
            this.freePlays += freePlaysAwarded;
        }

        this.balance = this.balance.subtract(payout);

        return new PlayOutcome(slots, prizeCategory, payout, freePlaysAwarded, this.balance);
    }

    private void chargeForPlay() {
        if (this.freePlays > 0) {
            this.freePlays--;
        } else {
            this.balance = this.balance.add(this.costPerPlay);
        }
    }

    private List<Colour> spinSlots() {
        return this.randomGenerator.ints(this.slotCount, 0, this.colourCount)
                .mapToObj(Colour::new)
                .toList();
    }

    private PrizeCategory determinePrizeCategory(List<Colour> slots) {
        Colour first = slots.getFirst();
        Colour previous = null;
        boolean allSame = true;
        int currentRunLength = 0;
        int longestRunLength = 0;
        Set<Colour> distinctColours = new HashSet<>();

        for (Colour colour : slots) {
            if (!colour.equals(first)) {
                allSame = false;
            }

            currentRunLength = colour.equals(previous) ? currentRunLength + 1 : 1;
            longestRunLength = Math.max(longestRunLength, currentRunLength);
            previous = colour;

            distinctColours.add(colour);
        }

        if (allSame) {
            return PrizeCategory.JACKPOT;
        }
        if (distinctColours.size() == slots.size()) {
            return PrizeCategory.FULL_HOUSE;
        }
        if (longestRunLength >= this.smallPrizeRunLength) {
            return PrizeCategory.SMALL_PRIZE;
        }
        return PrizeCategory.NONE;
    }

    private BigDecimal prizeOwed(PrizeCategory prizeCategory) {
        return switch (prizeCategory) {
            case JACKPOT -> this.balance;
            case FULL_HOUSE -> this.balance.divide(TWO, MONEY_SCALE, RoundingMode.HALF_UP);
            case SMALL_PRIZE -> this.costPerPlay.multiply(SMALL_PRIZE_MULTIPLIER);
            case NONE -> BigDecimal.ZERO;
        };
    }
}
