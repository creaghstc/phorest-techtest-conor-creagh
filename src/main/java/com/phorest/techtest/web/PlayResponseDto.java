package com.phorest.techtest.web;

import com.phorest.techtest.enums.PrizeCategory;
import com.phorest.techtest.model.Colour;
import com.phorest.techtest.model.PlayOutcome;

import java.math.BigDecimal;
import java.util.List;

public record PlayResponseDto(
        List<Integer> slots,
        PrizeCategory prizeCategory,
        BigDecimal amountWon,
        int freePlaysAwarded,
        BigDecimal balance) {

    public static PlayResponseDto from(PlayOutcome outcome) {
        return new PlayResponseDto(
                outcome.slots().stream().map(Colour::id).toList(),
                outcome.prizeCategory(),
                outcome.amountWon(),
                outcome.freePlaysAwarded(),
                outcome.balance());
    }
}
