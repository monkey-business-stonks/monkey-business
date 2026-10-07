package domain.service;

import domain.dto.*;
import domain.entities.Account;
import domain.entities.User;
import domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
            request.getUsername(), // name - use username as placeholder
            null, // phone
            LocalDateTime.now().minusYears(25).toLocalDate(), // dob - default to 25 years ago
            User.AccessLevel.USER,
            new HashSet<>(),
            ZonedDateTime.now(),
            ZonedDateTime.now()
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

        // TODO: Implement password verification using BCrypt
        // For now, just validate that password is provided
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return new AuthResponse()
            .userId(user.userId())
            .isAuthenticated(true)
            .accessLevel(domain.dto.AccessLevel.valueOf(user.getAccessLevel().toString()));
    }

    /**
     * Convert User to UserResponse
     */
    private UserResponse toUserResponse(User user) {
        return new UserResponse()
            .userId(user.userId())
            .username(user.username())
            .name(user.name())
            .email(user.email())
            .phone(user.phone())
            .dob(user.dob())
            .accessLevel(domain.dto.AccessLevel.valueOf(user.getAccessLevel().toString()))
            .lastLogin(user.getCreatedAt().toOffsetDateTime());
    }

    /**
     * Get stored user (internal use)
     */
    public User getUserDirect(UUID userId) {
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * Get user by username (for auth context)
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}
