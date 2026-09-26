package com.phorest.techtest.web;

import com.phorest.techtest.model.MachineState;

import java.math.BigDecimal;

public record MachineStateResponseDto(
        BigDecimal balance,
        int freePlays,
        int slotCount,
        int colourCount,
        int smallPrizeRunLength,
        BigDecimal costPerPlay) {

    public static MachineStateResponseDto from(MachineState state) {
        return new MachineStateResponseDto(
                state.balance(),
                state.freePlays(),
                state.slotCount(),
                state.colourCount(),
                state.smallPrizeRunLength(),
                state.costPerPlay());
    }
}
