package com.phorest.techtest.web;

import com.phorest.techtest.enums.PrizeCategory;
import com.phorest.techtest.model.Colour;
import com.phorest.techtest.model.PlayOutcome;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlayResponseDtoTest {

    @Test
    void fromFlattensColoursToTheirPlainIds() {
        // given
        PlayOutcome outcome = new PlayOutcome(
                List.of(new Colour(3), new Colour(1), new Colour(1), new Colour(0)),
                PrizeCategory.SMALL_PRIZE,
                new BigDecimal("10.00"),
                2,
                new BigDecimal("42.00"));

        // when
        PlayResponseDto dto = PlayResponseDto.from(outcome);

        // then
        assertThat(dto.slots()).containsExactly(3, 1, 1, 0);
        assertThat(dto.prizeCategory()).isEqualTo(PrizeCategory.SMALL_PRIZE);
        assertThat(dto.amountWon()).isEqualByComparingTo("10.00");
        assertThat(dto.freePlaysAwarded()).isEqualTo(2);
        assertThat(dto.balance()).isEqualByComparingTo("42.00");
    }
}
