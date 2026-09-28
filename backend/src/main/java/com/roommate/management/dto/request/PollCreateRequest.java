package com.roommate.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public record PollCreateRequest(
        @NotBlank String question,
        @NotEmpty List<String> options,
        LocalDateTime closesAt
) {}
