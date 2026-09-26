package com.phorest.techtest.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigureMachineRequestDtoTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void aWellFormedRequestHasNoViolations() {
        // given
        ConfigureMachineRequestDto request = new ConfigureMachineRequestDto(
                new BigDecimal("100.00"), new BigDecimal("1.00"), 4, 4, 2);

        // when
        Set<ConstraintViolation<ConfigureMachineRequestDto>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsANegativeInitialBalance() {
        // given
        ConfigureMachineRequestDto request = new ConfigureMachineRequestDto(
                new BigDecimal("-0.01"), new BigDecimal("1.00"), 4, 4, 2);

        // when
        Set<ConstraintViolation<ConfigureMachineRequestDto>> violations = validator.validate(request);

        // then
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("initialBalance");
    }

    @Test
    void rejectsAZeroCostPerPlay() {
        // given
        ConfigureMachineRequestDto request = new ConfigureMachineRequestDto(
                new BigDecimal("100.00"), BigDecimal.ZERO, 4, 4, 2);

        // when
        Set<ConstraintViolation<ConfigureMachineRequestDto>> violations = validator.validate(request);

        // then
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("costPerPlay");
    }

    @Test
    void rejectsANonPositiveSlotCount() {
        // given
        ConfigureMachineRequestDto request = new ConfigureMachineRequestDto(
                new BigDecimal("100.00"), new BigDecimal("1.00"), 0, 4, 2);

        // when
        Set<ConstraintViolation<ConfigureMachineRequestDto>> violations = validator.validate(request);

        // then
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("slotCount");
    }

    @Test
    void rejectsANonPositiveColourCount() {
        // given
        ConfigureMachineRequestDto request = new ConfigureMachineRequestDto(
                new BigDecimal("100.00"), new BigDecimal("1.00"), 4, 0, 2);

        // when
        Set<ConstraintViolation<ConfigureMachineRequestDto>> violations = validator.validate(request);

        // then
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("colourCount");
    }

    @Test
    void rejectsASmallPrizeRunLengthBelowTwo() {
        // given
        ConfigureMachineRequestDto request = new ConfigureMachineRequestDto(
                new BigDecimal("100.00"), new BigDecimal("1.00"), 4, 4, 1);

        // when
        Set<ConstraintViolation<ConfigureMachineRequestDto>> violations = validator.validate(request);

        // then
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("smallPrizeRunLength");
    }
}
