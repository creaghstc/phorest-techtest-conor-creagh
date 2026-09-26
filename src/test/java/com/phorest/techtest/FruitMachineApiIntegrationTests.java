package com.phorest.techtest;

import com.phorest.techtest.web.ConfigureMachineRequestDto;
import com.phorest.techtest.web.MachineStateResponseDto;
import com.phorest.techtest.web.PlayResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack tests that boot the real application (real {@code FruitMachineService}, real
 * {@code FruitMachine}, real embedded server) and drive it over genuine HTTP, rather than mocking
 * the service the way {@code FruitMachineControllerTest} does. These exist to catch wiring
 * problems (property binding, serialization, exception-handling configuration) that layer-isolated
 * tests can't see, not to re-verify game-rule details already covered at the unit level.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class FruitMachineApiIntegrationTests {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void configuringThenPlayingThenReadingStateRoundTripsCorrectly() {
        // given: reconfigure the real machine through the real HTTP API
        ConfigureMachineRequestDto configureRequest = new ConfigureMachineRequestDto(
                new BigDecimal("500.00"), new BigDecimal("5.00"), 4, 4, 2);

        MachineStateResponseDto configuredState = this.restTestClient.put()
                .uri("/fruit-machine")
                .contentType(MediaType.APPLICATION_JSON)
                .body(configureRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MachineStateResponseDto.class)
                .returnResult()
                .getResponseBody();

        assert configuredState != null;
        assertThat(configuredState.balance()).isEqualByComparingTo("500.00");
        assertThat(configuredState.slotCount()).isEqualTo(4);
        assertThat(configuredState.colourCount()).isEqualTo(4);

        // when: play for real, through the real domain logic (an unpredictable, real random spin)
        PlayResponseDto playOutcome = this.restTestClient.post()
                .uri("/fruit-machine/play")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PlayResponseDto.class)
                .returnResult()
                .getResponseBody();

        assert playOutcome != null;
        assertThat(playOutcome.slots()).hasSize(4);

        // then: reading state afterwards reflects exactly what the play response said
        this.restTestClient.get()
                .uri("/fruit-machine")
                .exchange()
                .expectStatus().isOk()
                .expectBody(MachineStateResponseDto.class)
                .value(state -> {
                    assert state != null;
                    assertThat(state.balance()).isEqualByComparingTo(playOutcome.balance());
                });
    }

    @Test
    void configuringWithATrulyInvalidCrossFieldCombinationReturnsARealProblemDetail() {
        // given: k (5) cannot exceed slotCount (4) - a domain rule enforced by FruitMachine's
        // constructor, not a Bean Validation annotation on the request DTO
        ConfigureMachineRequestDto invalidRequest = new ConfigureMachineRequestDto(
                new BigDecimal("100.00"), new BigDecimal("1.00"), 4, 4, 5);

        // when/then: the real FruitMachine constructor throws, and GlobalExceptionHandler really
        // translates it end-to-end (unlike FruitMachineControllerTest, where this is stubbed)
        this.restTestClient.put()
                .uri("/fruit-machine")
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("smallPrizeRunLength cannot exceed slotCount");
    }

    @Test
    void configuringWithAnInvalidRequestBodyReturnsA400() {
        // given: a Bean Validation failure (negative initialBalance) rather than a domain one
        ConfigureMachineRequestDto invalidRequest = new ConfigureMachineRequestDto(
                new BigDecimal("-1.00"), new BigDecimal("1.00"), 4, 4, 2);

        // when/then
        this.restTestClient.put()
                .uri("/fruit-machine")
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
