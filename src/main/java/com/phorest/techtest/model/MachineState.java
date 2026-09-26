package com.phorest.techtest.model;

import java.math.BigDecimal;

public record MachineState(
        BigDecimal balance,
        int freePlays,
        int slotCount,
        int colourCount,
        int smallPrizeRunLength,
        BigDecimal costPerPlay) {
}
