package com.library.lms.dto.response;

import java.util.Set;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long userId,
        String username,
        Set<String> authorities
) {
    public static AuthResponse of(String accessToken, String refreshToken, Long userId,
                                   String username, Set<String> authorities) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", userId, username, authorities);
    }
}
