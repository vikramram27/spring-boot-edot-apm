package com.edot.apm.auth;

import com.edot.apm.auth.dto.AuthResponse;
import com.edot.apm.auth.dto.LoginRequest;
import com.edot.apm.auth.dto.RegisterRequest;
import com.edot.apm.common.exception.BadRequestException;
import com.edot.apm.pocket.PocketService;
import com.edot.apm.security.JwtTokenProvider;
import com.edot.apm.security.UserPrincipal;
import com.edot.apm.user.Role;
import com.edot.apm.user.User;
import com.edot.apm.user.UserRepository;
import com.edot.apm.user.dto.UserProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PocketService pocketService;
    private final StringRedisTemplate redisTemplate;
    private final long expirationMs;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       PocketService pocketService,
                       StringRedisTemplate redisTemplate,
                       @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.pocketService = pocketService;
        this.redisTemplate = redisTemplate;
        this.expirationMs = expirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        logger.info("Attempting registration for email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email address already registered: " + email);
        }

        User user = new User(
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_USER
        );

        User savedUser = userRepository.save(user);

        // Initialize the user's single pocket immediately
        pocketService.createPocketForUser(savedUser);

        UserPrincipal principal = UserPrincipal.create(savedUser);
        String token = tokenProvider.generateToken(principal);

        logger.info("User registered successfully with id: {}", savedUser.getId());

        return new AuthResponse(
                token,
                expirationMs / 1000,
                new UserProfileResponse(
                        savedUser.getId(),
                        savedUser.getName(),
                        savedUser.getEmail(),
                        savedUser.getRole(),
                        savedUser.getCreatedAt()
                )
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        logger.info("Authenticating user with email: {}", email);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.generateToken(principal);

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadRequestException("User profile not found"));

        logger.info("User authenticated successfully with id: {}", user.getId());

        return new AuthResponse(
                token,
                expirationMs / 1000,
                new UserProfileResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getCreatedAt()
                )
        );
    }

    public void logout(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            long remainingMs = tokenProvider.getRemainingExpirationMs(token);

            if (remainingMs > 0) {
                String blacklistKey = "jwt:blacklist:" + token;
                redisTemplate.opsForValue().set(blacklistKey, "revoked", Duration.ofMillis(remainingMs));
                logger.info("JWT token successfully blacklisted in Redis for {} ms", remainingMs);
            }
        }
    }
}
