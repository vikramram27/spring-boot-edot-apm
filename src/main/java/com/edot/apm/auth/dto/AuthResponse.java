package com.edot.apm.auth.dto;

import com.edot.apm.user.dto.UserProfileResponse;

public class AuthResponse {

    private final String accessToken;
    private final String tokenType = "Bearer";
    private final long expiresIn;
    private final UserProfileResponse user;

    public AuthResponse(String accessToken, long expiresIn, UserProfileResponse user) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public UserProfileResponse getUser() {
        return user;
    }
}
