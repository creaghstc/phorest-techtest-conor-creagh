package com.phorest.techtest.config;

import com.phorest.techtest.service.FruitMachine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.random.RandomGenerator;

@Configuration
public class FruitMachineConfig {

    @Bean
    public RandomGenerator randomGenerator() {
        return RandomGenerator.getDefault();
    }

    @Bean
    public FruitMachine fruitMachine(
            RandomGenerator randomGenerator,
            @Value("${fruitMachine.initialBalance}") BigDecimal initialBalance,
            @Value("${fruitMachine.costPerPlay}") BigDecimal costPerPlay,
            @Value("${fruitMachine.slotCount}") int slotCount,
            @Value("${fruitMachine.colourCount}") int colourCount,
            @Value("${fruitMachine.smallPrizeRunLength}") int smallPrizeRunLength) {
        return new FruitMachine(
                randomGenerator, initialBalance, costPerPlay, slotCount, colourCount, smallPrizeRunLength);
    }
}
