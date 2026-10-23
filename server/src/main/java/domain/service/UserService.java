package domain.service;

import domain.dto.*;
import domain.entities.User;
import domain.error.BusinessConflictException;
import domain.repository.UserRepository;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String authServiceUrl = "http://monkey_business_auth:3000/auth";

    public UserResponse createUser(CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        String hashedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());

        UUID userId = UUID.randomUUID();
        User user = new User(
            userId,
            request.getUsername(),
            request.getEmail(),
            hashedPassword,
            request.getName(),
            request.getPhone().orElse(null),
            request.getDob(),
            User.AccessLevel.USER,
            new HashSet<>(),
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        userRepository.save(user);

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        // Validate all supplied details before changing the user.
        validateUserUpdate(user, request);
        String passwordHash = request.getPassword() == null ? null
            : BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());

        updateUsername(user, request.getUsername());
        updateName(user, request.getName());
        updateEmail(user, request.getEmail());
        updateDob(user, request.getDob());
        updatePassword(user, passwordHash);
        user.setUpdatedAt(ZonedDateTime.now());

        saveUserUpdate(user);
        return toUserResponse(user);
    }

    private void validateUserUpdate(User user, UpdateUserRequest request) {
        validateNonBlank(request.getUsername(), "Username");
        validateNonBlank(request.getName(), "Name");
        validateNonBlank(request.getEmail(), "Email");
        if (request.getDob() != null && request.getDob().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth must not be in the future");
        }
        if (request.getPassword() != null && (request.getPassword().isBlank()
                || request.getPassword().length() < 8
                || request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72)) {
            throw new IllegalArgumentException("Password must be at least 8 characters and at most 72 UTF-8 bytes");
        }
        validatePasswordChange(user, request);

        if (request.getUsername() != null) {
            userRepository.findByUsername(request.getUsername())
                .filter(existing -> !user.getUserId().equals(existing.getUserId()))
                .ifPresent(existing -> { throw new BusinessConflictException("Username already exists"); });
        }
        if (request.getEmail() != null) {
            userRepository.findByEmail(request.getEmail())
                .filter(existing -> !user.getUserId().equals(existing.getUserId()))
                .ifPresent(existing -> { throw new BusinessConflictException("Email already exists"); });
        }
    }

    private void validatePasswordChange(User user, UpdateUserRequest request) {
        if (request.getPassword() == null) {
            if (request.getCurrentPassword() != null) {
                throw new IllegalArgumentException("A new password is required with the current password");
            }
            return;
        }
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("Current password is required to change the password");
        }
        if (!BCrypt.checkpw(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
    }

    private void updateUsername(User user, String username) {
        if (username != null) {
            user.setUsername(username);
        }
    }

    private void updateName(User user, String name) {
        if (name != null) {
            user.setName(name);
        }
    }

    private void updateEmail(User user, String email) {
        if (email != null) {
            user.setEmail(email);
        }
    }

    private void updateDob(User user, LocalDate dob) {
        if (dob != null) {
            user.setDob(dob);
        }
    }

    private void updatePassword(User user, String passwordHash) {
        if (passwordHash != null) {
            user.setPasswordHash(passwordHash);
        }
    }

    private void saveUserUpdate(User user) {
        try {
            // Flush inside the transaction so concurrent uniqueness conflicts become HTTP 409.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessConflictException("User details conflict with an existing user");
        }
    }

    private void validateNonBlank(String value, String field) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    public AuthResponse authenticate(AuthenticateRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Pass full user object to include userId and role
        Map<String, String> tokens = requestTokensFromAuthService(user);
        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");

        user.setRefreshToken(refreshToken);
        user.setUpdatedAt(ZonedDateTime.now());
        userRepository.save(user);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId(user.getUserId());
        authResponse.setIsAuthenticated(true);
        authResponse.setAccessLevel(domain.dto.AccessLevel.valueOf(user.getAccessLevel().toString()));
        authResponse.setAccessToken(JsonNullable.of(accessToken));
        authResponse.setRefreshToken(JsonNullable.of(refreshToken));

        return authResponse;
    }

    public Map<String, String> refreshToken(String refreshToken) {
        User user = userRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> authPayload = new HashMap<>();
        authPayload.put("refreshToken", refreshToken);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(authPayload, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(authServiceUrl + "/refresh", entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String newAccessToken = (String) response.getBody().get("accessToken");
                Map<String, String> result = new HashMap<>();
                result.put("accessToken", newAccessToken);
                return result;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or expired refresh token", e);
        }

        throw new IllegalArgumentException("Invalid or expired refresh token");
    }

    public void logout(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> revokePayload = new HashMap<>();
            revokePayload.put("username", user.getUsername());

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(revokePayload, headers);
            restTemplate.postForEntity(authServiceUrl + "/revoke-all", entity, Map.class);
        } catch (Exception e) {
            // TODO: log exception but continue with logout cleanup
        }
        user.setRefreshToken(null);
        user.setUpdatedAt(ZonedDateTime.now());
        userRepository.save(user);
    }

    private Map<String, String> requestTokensFromAuthService(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> authPayload = new HashMap<>();
        authPayload.put("userId", user.getUserId().toString());
        authPayload.put("username", user.getUsername());
        authPayload.put("role", user.getAccessLevel().name()); 

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(authPayload, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(authServiceUrl + "/login", entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, String> tokens = new HashMap<>();
                tokens.put("accessToken", (String) response.getBody().get("accessToken"));
                tokens.put("refreshToken", (String) response.getBody().get("refreshToken"));
                return tokens;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Auth Service error: " + e.getMessage(), e);
        }

        throw new IllegalStateException("Failed to retrieve tokens from Auth Service");
    }

    public UserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(JsonNullable.of(user.getPhone()));
        response.setDob(user.getDob());
        response.setAccessLevel(domain.dto.AccessLevel.valueOf(user.getAccessLevel().toString()));
        response.setCreatedOn(user.getCreatedAt().toOffsetDateTime());
        response.setLastLogin(JsonNullable.of(user.getCreatedAt().toOffsetDateTime()));
        return response;
    }

    public User getUserDirect(UUID userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}