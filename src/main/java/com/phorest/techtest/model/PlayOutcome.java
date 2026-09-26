package com.phorest.techtest.model;

import com.phorest.techtest.enums.Colour;

import java.util.List;

public record PlayOutcome(List<Colour> slots, boolean jackpot) {
}
