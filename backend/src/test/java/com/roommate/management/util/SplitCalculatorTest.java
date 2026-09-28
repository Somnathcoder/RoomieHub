package com.roommate.management.util;

import com.roommate.management.dto.request.SplitItemRequest;
import com.roommate.management.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SplitCalculatorTest {

    @Test
    void equalSplit_distributesEvenlyAndSumsToTotal() {
        Map<Long, BigDecimal> result = SplitCalculator.equalSplit(new BigDecimal("2000"), List.of(1L, 2L, 3L, 4L));

        assertEquals(4, result.size());
        BigDecimal sum = result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(new BigDecimal("2000")));
        // 2000 / 4 = exactly 500 each
        result.values().forEach(v -> assertEquals(0, v.compareTo(new BigDecimal("500.00"))));
    }

    @Test
    void equalSplit_withRemainder_assignsLeftoverToFirstMember() {
        // 100 / 3 = 33.33 each with 0.01 leftover
        Map<Long, BigDecimal> result = SplitCalculator.equalSplit(new BigDecimal("100"), List.of(10L, 20L, 30L));

        BigDecimal sum = result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(new BigDecimal("100.00")));
        assertTrue(result.get(10L).compareTo(result.get(20L)) >= 0);
    }

    @Test
    void equalSplit_emptyMemberList_throws() {
        assertThrows(BadRequestException.class, () -> SplitCalculator.equalSplit(BigDecimal.TEN, List.of()));
    }

    @Test
    void customSplit_matchingSum_succeeds() {
        List<SplitItemRequest> items = List.of(
                new SplitItemRequest(1L, new BigDecimal("200")),
                new SplitItemRequest(2L, new BigDecimal("600")),
                new SplitItemRequest(3L, new BigDecimal("600")),
                new SplitItemRequest(4L, new BigDecimal("600"))
        );
        Map<Long, BigDecimal> result = SplitCalculator.customSplit(new BigDecimal("2000"), items);
        assertEquals(4, result.size());
        assertEquals(0, result.get(1L).compareTo(new BigDecimal("200")));
    }

    @Test
    void customSplit_sumNotMatchingTotal_throwsBadRequest() {
        List<SplitItemRequest> items = List.of(
                new SplitItemRequest(1L, new BigDecimal("200")),
                new SplitItemRequest(2L, new BigDecimal("600"))
        );
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> SplitCalculator.customSplit(new BigDecimal("2000"), items));
        assertTrue(ex.getMessage().contains("must equal"));
    }

    @Test
    void customSplit_duplicateMember_throws() {
        List<SplitItemRequest> items = List.of(
                new SplitItemRequest(1L, new BigDecimal("100")),
                new SplitItemRequest(1L, new BigDecimal("100"))
        );
        assertThrows(BadRequestException.class, () -> SplitCalculator.customSplit(new BigDecimal("200"), items));
    }
}
