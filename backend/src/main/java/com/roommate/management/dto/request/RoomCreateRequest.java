package com.roommate.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCreateRequest(
        @NotBlank @Size(max = 150) String roomName,
        @NotBlank @Size(max = 500) String address
) {}
