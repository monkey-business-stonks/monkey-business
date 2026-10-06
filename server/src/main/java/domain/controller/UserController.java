package domain.controller;

import domain.dto.*;
import domain.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * Create a new user
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            logger.info("Creating new user with email: {}", request.getEmail());
            UserResponse user = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid user creation request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("INVALID_INPUT"));
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse()
                    .message("Failed to create user")
                    .error("SERVER_ERROR"));
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        try {
            UUID id = UUID.fromString(userId);
            logger.debug("Retrieving user: {}", userId);
            UserResponse user = userService.getUser(id);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid user ID format: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse()
                    .message("Invalid user ID format")
                    .error("INVALID_ID"));
        } catch (NoSuchElementException e) {
            logger.warn("User not found: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("NOT_FOUND"));
        } catch (Exception e) {
            logger.error("Error retrieving user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse()
                    .message("Failed to retrieve user")
                    .error("SERVER_ERROR"));
        }
    }

    /**
     * Authenticate user (login)
     */
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@Valid @RequestBody AuthenticateRequest request) {
        try {
            logger.info("User authentication attempt for email: {}", request.getEmail());
            AuthResponse authResponse = userService.authenticate(request);
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            logger.warn("Authentication failed for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse()
                    .message(e.getMessage())
                    .error("INVALID_CREDENTIALS"));
        } catch (Exception e) {
            logger.error("Authentication error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse()
                    .message("Authentication failed")
                    .error("SERVER_ERROR"));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is running");
    }
}
