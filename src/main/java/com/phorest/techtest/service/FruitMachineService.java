package com.phorest.techtest.service;

import com.phorest.techtest.domain.FruitMachine;
import com.phorest.techtest.model.MachineState;
import com.phorest.techtest.model.PlayOutcome;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;

@Service
public class FruitMachineService {

    private final RandomGenerator randomGenerator;
    private final AtomicReference<FruitMachine> fruitMachine;

    public FruitMachineService(RandomGenerator randomGenerator, FruitMachine fruitMachine) {
        this.randomGenerator = randomGenerator;
        this.fruitMachine = new AtomicReference<>(fruitMachine);
    }

    public PlayOutcome play() {
        return this.fruitMachine.get().spin();
    }

    public MachineState state() {
        return this.fruitMachine.get().state();
    }

    public MachineState configure(
            BigDecimal initialBalance,
            BigDecimal costPerPlay,
            int slotCount,
            int colourCount,
            int smallPrizeRunLength) {
        FruitMachine newMachine = new FruitMachine(
                this.randomGenerator, initialBalance, costPerPlay, slotCount, colourCount, smallPrizeRunLength);
        this.fruitMachine.set(newMachine);
        return newMachine.state();
    }
}
