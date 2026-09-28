package com.roommate.management.dto.request;

import java.time.LocalDate;

public record SettlementPayRequest(
        LocalDate paymentDate
) {}
