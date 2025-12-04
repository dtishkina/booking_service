package com.example.booking.dto.auth;

import java.util.List;

public record JwtResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String username,
        List<String> roles
) {
    public JwtResponse(String accessToken, Long userId, String username, List<String> roles) {
        this(accessToken, "Bearer", userId, username, roles);
    }
}
