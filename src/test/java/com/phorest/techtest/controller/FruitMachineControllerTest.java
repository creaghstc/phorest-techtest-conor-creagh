package com.phorest.techtest.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.phorest.techtest.enums.PrizeCategory;
import com.phorest.techtest.exception.GlobalExceptionHandler;
import com.phorest.techtest.model.Colour;
import com.phorest.techtest.model.MachineState;
import com.phorest.techtest.model.PlayOutcome;
import com.phorest.techtest.service.FruitMachineService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FruitMachineController.class)
class FruitMachineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FruitMachineService fruitMachineService;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogAppender() {
        this.logAppender = new ListAppender<>();
        this.logAppender.start();
        ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).addAppender(this.logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).detachAppender(this.logAppender);
    }

    @Test
    void playReturnsTheOutcomeFromTheService() throws Exception {
        // given
        PlayOutcome outcome = new PlayOutcome(
                List.of(new Colour(0), new Colour(0), new Colour(0), new Colour(0)),
                PrizeCategory.JACKPOT,
                new BigDecimal("52.00"),
                0,
                new BigDecimal("0.00"));
        given(fruitMachineService.play()).willReturn(outcome);

        // when/then: the domain Colour[id] wrapper is flattened to a plain int in the response
        mockMvc.perform(post("/fruit-machine/play"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(4))
                .andExpect(jsonPath("$.slots[0]").value(0))
                .andExpect(jsonPath("$.prizeCategory").value("JACKPOT"))
                .andExpect(jsonPath("$.amountWon").value(52.00))
                .andExpect(jsonPath("$.freePlaysAwarded").value(0))
                .andExpect(jsonPath("$.balance").value(0.00));
    }

    @Test
    void stateReturnsTheCurrentMachineState() throws Exception {
        // given
        MachineState state = new MachineState(
                new BigDecimal("100.00"), 2, 4, 4, 2, new BigDecimal("1.00"));
        given(fruitMachineService.state()).willReturn(state);

        // when/then
        mockMvc.perform(get("/fruit-machine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00))
                .andExpect(jsonPath("$.freePlays").value(2))
                .andExpect(jsonPath("$.slotCount").value(4))
                .andExpect(jsonPath("$.colourCount").value(4))
                .andExpect(jsonPath("$.smallPrizeRunLength").value(2))
                .andExpect(jsonPath("$.costPerPlay").value(1.00));
    }

    @Test
    void configureDelegatesToTheServiceAndReturnsTheNewState() throws Exception {
        // given
        MachineState newState = new MachineState(
                new BigDecimal("500.00"), 0, 6, 10, 3, new BigDecimal("5.00"));
        given(fruitMachineService.configure(new BigDecimal("500.00"), new BigDecimal("5.00"), 6, 10, 3))
                .willReturn(newState);

        // when/then
        mockMvc.perform(put("/fruit-machine")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "initialBalance": 500.00,
                                  "costPerPlay": 5.00,
                                  "slotCount": 6,
                                  "colourCount": 10,
                                  "smallPrizeRunLength": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00))
                .andExpect(jsonPath("$.slotCount").value(6));
    }

    @Test
    void configureRejectsAnInvalidRequestBodyBeforeReachingTheService() throws Exception {
        // when/then: negative initialBalance fails @Valid before the service is ever called,
        // and the field-level violation message is surfaced rather than a generic detail
        mockMvc.perform(put("/fruit-machine")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "initialBalance": -1.00,
                                  "costPerPlay": 5.00,
                                  "slotCount": 6,
                                  "colourCount": 10,
                                  "smallPrizeRunLength": 3
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("initialBalance: must be greater than or equal to 0"));

        verifyNoInteractions(fruitMachineService);
    }

    @Test
    void configureTranslatesDomainValidationFailuresToBadRequest() throws Exception {
        // given: well-formed request, but the service rejects the cross-field combination
        given(fruitMachineService.configure(any(), any(), anyInt(), anyInt(), anyInt()))
                .willThrow(new IllegalArgumentException("smallPrizeRunLength cannot exceed slotCount"));

        // when/then
        mockMvc.perform(put("/fruit-machine")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "initialBalance": 100.00,
                                  "costPerPlay": 1.00,
                                  "slotCount": 4,
                                  "colourCount": 4,
                                  "smallPrizeRunLength": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("smallPrizeRunLength cannot exceed slotCount"));
    }

    @Test
    void unexpectedExceptionsAreTranslatedToAGenericInternalServerError() throws Exception {
        // given: an exception with no dedicated handler, whose message must not leak to the client
        given(fruitMachineService.play()).willThrow(new RuntimeException("db connection string: secret"));

        // when/then
        mockMvc.perform(post("/fruit-machine/play"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));

        // and: the real exception is still logged server-side, for debugging
        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("db connection string: secret");
                });
    }
}
