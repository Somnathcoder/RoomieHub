package com.roommate.management.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PublicMessageCreateRequest(
        @NotBlank String content
) {}
