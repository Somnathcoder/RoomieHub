package com.roommate.management.util;

import com.roommate.management.dto.request.SplitItemRequest;
import com.roommate.management.exception.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes equal and custom expense splits.
 */
public final class SplitCalculator {

    private SplitCalculator() {}

    /**
     * Splits totalAmount equally across memberIds. Any leftover paise from
     * rounding (since amounts must sum exactly to totalAmount) is assigned to
     * the first member in the list, so SUM(shares) == totalAmount always.
     */
    public static Map<Long, BigDecimal> equalSplit(BigDecimal totalAmount, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new BadRequestException("At least one member must be selected for an equal split");
        }
        int n = memberIds.size();
        BigDecimal share = totalAmount.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        BigDecimal distributed = share.multiply(BigDecimal.valueOf(n));
        BigDecimal remainder = totalAmount.subtract(distributed);

        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            BigDecimal amount = share;
            if (i == 0) {
                amount = amount.add(remainder);
            }
            result.put(memberIds.get(i), amount);
        }
        return result;
    }

    /**
     * Validates that a custom split's individual shares sum exactly to the
     * expense total. Throws BadRequestException otherwise.
     */
    public static Map<Long, BigDecimal> customSplit(BigDecimal totalAmount, List<SplitItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Custom split requires at least one member share");
        }
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (SplitItemRequest item : items) {
            if (item.amount() == null || item.amount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Each split amount must be zero or greater");
            }
            if (result.containsKey(item.roomMemberId())) {
                throw new BadRequestException("Duplicate member in custom split");
            }
            result.put(item.roomMemberId(), item.amount());
            sum = sum.add(item.amount());
        }
        if (sum.setScale(2, RoundingMode.HALF_UP).compareTo(totalAmount.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new BadRequestException(
                    "Sum of custom split shares (" + sum + ") must equal the total expense amount (" + totalAmount + ")");
        }
        return result;
    }
}
