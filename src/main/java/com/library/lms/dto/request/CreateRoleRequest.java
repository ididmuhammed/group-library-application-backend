package com.library.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record CreateRoleRequest(
        @NotBlank(message = "Role name is required") String name,
        String description,
        @NotEmpty(message = "At least one permission must be provided") Set<String> permissionNames
) {}
