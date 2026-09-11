package com.library.lms.dto.response;

import com.library.lms.entity.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        boolean enabled,
        Set<String> roles,
        String profileImageUrl,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.isEnabled(),
                user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()),
                user.getProfileImageUrl(),
                user.getCreatedAt()
        );
    }
}
