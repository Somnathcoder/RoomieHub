package com.roommate.management.dto.request;

import jakarta.validation.constraints.NotNull;

public record PollVoteRequest(
        @NotNull Long optionId
) {}
