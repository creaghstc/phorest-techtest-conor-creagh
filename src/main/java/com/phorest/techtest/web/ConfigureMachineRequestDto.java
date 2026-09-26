package com.phorest.techtest.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ConfigureMachineRequestDto(
        @NotNull @PositiveOrZero BigDecimal initialBalance,
        @NotNull @Positive BigDecimal costPerPlay,
        @Positive int slotCount,
        @Positive int colourCount,
        @Min(2) int smallPrizeRunLength) {
}
