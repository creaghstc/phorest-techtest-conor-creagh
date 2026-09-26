package com.phorest.techtest.service;

import com.phorest.techtest.enums.Colour;
import com.phorest.techtest.enums.PrizeCategory;
import com.phorest.techtest.model.PlayOutcome;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.random.RandomGenerator;

public class FruitMachine {

    private static final int SLOT_COUNT = 4;
    private static final BigDecimal SMALL_PRIZE_MULTIPLIER = BigDecimal.valueOf(5);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final int MONEY_SCALE = 2;

    private final RandomGenerator randomGenerator;
    private final BigDecimal costPerPlay;

    private BigDecimal balance;
    private int freePlays;

    public FruitMachine(RandomGenerator randomGenerator, BigDecimal initialBalance, BigDecimal costPerPlay) {
        this.randomGenerator = randomGenerator;
        this.balance = initialBalance;
        this.costPerPlay = costPerPlay;
        this.freePlays = 0;
    }

    public PlayOutcome spin() {
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
        Colour[] colours = Colour.values();

        return this.randomGenerator.ints(SLOT_COUNT, 0, colours.length)
                .mapToObj(index -> colours[index])
                .toList();
    }

    private PrizeCategory determinePrizeCategory(List<Colour> slots) {
        long distinctColours = slots.stream().distinct().count();

        if (distinctColours == 1) {
            return PrizeCategory.JACKPOT;
        }
        if (distinctColours == slots.size()) {
            return PrizeCategory.FULL_HOUSE;
        }
        if (hasAdjacentMatch(slots)) {
            return PrizeCategory.SMALL_PRIZE;
        }
        return PrizeCategory.NONE;
    }

    private boolean hasAdjacentMatch(List<Colour> slots) {
        Colour previous = null;
        for (Colour colour : slots) {
            if (colour == previous) {
                return true;
            }
            previous = colour;
        }
        return false;
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
