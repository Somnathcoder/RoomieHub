package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record MemberCreateRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        String password,
        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be exactly 10 digits")
        String mobileNumber,
        String roomNumber,
        String bedNumber,
        @NotNull RoleType role,
        LocalDate joiningDate
) {}
