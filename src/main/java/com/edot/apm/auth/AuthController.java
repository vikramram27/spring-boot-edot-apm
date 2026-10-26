package com.edot.apm.auth;

import com.edot.apm.auth.dto.AuthResponse;
import com.edot.apm.auth.dto.LoginRequest;
import com.edot.apm.auth.dto.RegisterRequest;
import com.edot.apm.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", "Token revoked"));
    }
}
