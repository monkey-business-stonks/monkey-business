package main.service;

import main.dto.*;
import main.Account;
import main.User;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create user
        UUID userId = UUID.randomUUID();
        User user = new User(
            userId,
            request.getUsername(),
            request.getEmail(),
            request.getPassword(),
            User.AccessLevel.User,
            true,
            new HashSet<>(),
            LocalDateTime.now()
        );

        // Store user in repository
        userRepository.save(user);

        return toUserResponse(user);
    }

    /**
     * Get user by ID
     */
    public UserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));
        return toUserResponse(user);
    }

    /**
     * Authenticate user
     */
    public AuthResponse authenticate(AuthenticateRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!user.login(request.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return new AuthResponse(
            user.userId(),
            true,
            user.userAccessLevel().toString(),
            "mock-jwt-token-" + user.userId()  // Mock JWT token
        );
    }

    /**
     * Convert User to UserResponse
     */
    private UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.userId(),
            user.username(),
            user.username(),  // Reusing for name (should be separate in User record)
            user.email(),
            null,
            null,
            user.userAccessLevel().toString(),
            user.createdAt()
        );
    }

    /**
     * Get stored user (internal use)
     */
    public User getUserDirect(UUID userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
