package com.phorest.techtest.model;

import com.phorest.techtest.enums.PrizeCategory;

import java.math.BigDecimal;
import java.util.List;

public record PlayOutcome(
        List<Colour> slots,
        PrizeCategory prizeCategory,
        BigDecimal amountWon,
        int freePlaysAwarded,
        BigDecimal balance) {
}
