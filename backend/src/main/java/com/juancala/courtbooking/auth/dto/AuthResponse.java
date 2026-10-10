package com.juancala.courtbooking.auth.dto;

import com.juancala.courtbooking.user.UserResponse;

public record AuthResponse(
        String token,
        long expiresIn,
        UserResponse user
) {
}
