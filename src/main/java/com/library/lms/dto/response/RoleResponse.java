package com.library.lms.dto.response;

import com.library.lms.entity.Role;

import java.util.Set;
import java.util.stream.Collectors;

public record RoleResponse(
        Long id,
        String name,
        String description,
        Set<String> permissions
) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream().map(p -> p.getName()).collect(Collectors.toSet())
        );
    }
}
