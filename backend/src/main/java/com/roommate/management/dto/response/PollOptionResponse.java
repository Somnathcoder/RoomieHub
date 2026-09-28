package com.roommate.management.dto.response;

public record PollOptionResponse(
        Long id,
        String optionText,
        long voteCount
) {}
