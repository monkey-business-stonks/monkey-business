package main.service;

import main.dto.*;
import main.entity.UserEntity;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new user
     */
    public UserResponse createUser(CreateUserRequest request) {
        // Validate request
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        // Check for duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create user entity
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity(
            userId,
            request.getUsername(),
            request.getEmail(),
            request.getPassword(),
            UserEntity.AccessLevel.User,
            true,
            LocalDateTime.now()
        );

        // Save to database
        user = userRepository.save(user);
        return toUserResponse(user);
    }

    /**
     * Get user by ID
     */
    public UserResponse getUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));
        return toUserResponse(user);
    }

    /**
     * Authenticate user
     */
    public AuthResponse authenticate(AuthenticateRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        // Validate password (currently plain text; TODO: use bcrypt in production)
        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is inactive");
        }

        return new AuthResponse(
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getUserAccessLevel().toString(),
            "mock-jwt-token-" + user.getUserId()  // Mock JWT token
        );
    }

    /**
     * Convert UserEntity to UserResponse
     */
    private UserResponse toUserResponse(UserEntity user) {
        return new UserResponse(
            user.getUserId(),
            user.getUsername(),
            user.getUsername(),  // Reusing for name (should be separate in User entity)
            user.getEmail(),
            null,
            null,
            user.getUserAccessLevel().toString(),
            user.getCreatedAt()
        );
    }

    /**
     * Get stored user entity (internal use)
     */
    public UserEntity getUserDirect(UUID userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
