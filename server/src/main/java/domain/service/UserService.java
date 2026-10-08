package domain.service;

import domain.dto.*;
import domain.entities.User;
import domain.repository.UserRepository;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String authServiceUrl = "http://monkey_business_auth:3000/auth";

    /**
     * Create a new user (Registration) - hashes password with BCrypt
     */
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

    /**
     * Authenticate user (Login) - verifies BCrypt hash, calls Auth Service, stores refresh token
     */
    public AuthResponse authenticate(AuthenticateRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        Map<String, String> tokens = requestTokensFromAuthService(user.getUsername());
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

    /**
     * Refresh Access Token via NestJS Auth Service
     */
    public Map<String, String> refreshToken(String refreshToken) {
        User user = userRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> authPayload = new HashMap<>();
        authPayload.put("refreshToken", refreshToken);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(authPayload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(authServiceUrl + "/refresh", entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String newAccessToken = (String) response.getBody().get("accessToken");
            Map<String, String> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            return result;
        }

        throw new IllegalArgumentException("Invalid or expired refresh token");
    }

    /**
     * Logout - removes stored refresh token
     */
    public void logout(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        user.setRefreshToken(null);
        user.setUpdatedAt(ZonedDateTime.now());
        userRepository.save(user);
    }

    private Map<String, String> requestTokensFromAuthService(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> authPayload = new HashMap<>();
        authPayload.put("username", username);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(authPayload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(authServiceUrl + "/login", entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", (String) response.getBody().get("accessToken"));
            tokens.put("refreshToken", (String) response.getBody().get("refreshToken"));
            return tokens;
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