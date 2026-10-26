package com.edot.apm.user;

import com.edot.apm.common.exception.ResourceNotFoundException;
import com.edot.apm.user.dto.UpdateProfileRequest;
import com.edot.apm.user.dto.UserProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        logger.info("Fetching profile for user id: {}", userId);
        User user = findUserById(userId);
        return mapToProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        logger.info("Updating profile for user id: {}", userId);
        User user = findUserById(userId);
        user.setName(request.getName().trim());
        User updated = userRepository.save(user);
        logger.info("Profile successfully updated for user id: {}", userId);
        return mapToProfileResponse(updated);
    }

    @Transactional(readOnly = true)
    public User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
