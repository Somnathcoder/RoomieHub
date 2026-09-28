package com.roommate.management.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PollResponse(
        Long id,
        String question,
        String createdByName,
        String status,
        LocalDateTime closesAt,
        List<PollOptionResponse> options,
        long totalVotes,
        Long myVoteOptionId
) {}
