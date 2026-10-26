package com.edot.apm.user;

import com.edot.apm.common.dto.ApiResponse;
import com.edot.apm.security.UserPrincipal;
import com.edot.apm.user.dto.UpdateProfileRequest;
import com.edot.apm.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserProfileResponse profile = userService.getUserProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("User profile retrieved successfully", profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse profile = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("User profile updated successfully", profile));
    }
}
