package com.phorest.techtest.web;

import com.phorest.techtest.model.MachineState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MachineStateResponseDtoTest {

    @Test
    void fromCopiesEveryFieldFromTheDomainState() {
        // given
        MachineState state = new MachineState(
                new BigDecimal("500.00"), 3, 6, 10, 3, new BigDecimal("5.00"));

        // when
        MachineStateResponseDto dto = MachineStateResponseDto.from(state);

        // then
        assertThat(dto.balance()).isEqualByComparingTo("500.00");
        assertThat(dto.freePlays()).isEqualTo(3);
        assertThat(dto.slotCount()).isEqualTo(6);
        assertThat(dto.colourCount()).isEqualTo(10);
        assertThat(dto.smallPrizeRunLength()).isEqualTo(3);
        assertThat(dto.costPerPlay()).isEqualByComparingTo("5.00");
    }
}
