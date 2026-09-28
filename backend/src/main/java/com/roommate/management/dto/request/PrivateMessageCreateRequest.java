package com.roommate.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PrivateMessageCreateRequest(
        @NotNull Long receiverUserId,
        @NotBlank String content
) {}
